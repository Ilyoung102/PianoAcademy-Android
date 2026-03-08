package com.pianoacademy.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*
import kotlin.random.Random

// ── 사운드 모드 정의 ────────────────────────────────────────────
enum class SoundMode(
    val label: String,
    val icon: String,
    val brightness: Float,      // 0=따뜻한/어두운, 1=밝은
    val halfLifeMult: Float,    // 음 지속 길이 배율
    val transientLevel: Float   // 해머 타격 노이즈 레벨
) {
    GRAND(
        label = "그랜드", icon = "🎹",
        brightness = 0.60f,
        halfLifeMult = 1.0f,
        transientLevel = 0.20f
    ),
    ELECTRIC(
        label = "일렉피아노", icon = "🎸",
        brightness = 0.85f,
        halfLifeMult = 0.65f,
        transientLevel = 0.35f
    ),
    SOFT(
        label = "소프트", icon = "🌙",
        brightness = 0.20f,
        halfLifeMult = 1.2f,
        transientLevel = 0.05f
    ),
    VIBRA(
        label = "비브라폰", icon = "🔔",
        brightness = 0.80f,
        halfLifeMult = 0.45f,
        transientLevel = 0.55f
    ),
    ORGAN(
        label = "오르간", icon = "🎺",
        brightness = 0.5f,
        halfLifeMult = 0f,   // 사용 안 함 (가산 합성)
        transientLevel = 0f
    )
}

// ── 피아노 사운드 엔진 ────────────────────────────────────────────
class PianoSoundEngine {
    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val activeNotes = mutableMapOf<String, Pair<Job, AudioTrack?>>()
    private val noteLock = Any()

    private var volume = 1.0f
    var soundMode = SoundMode.GRAND

    fun setVolume(v: Float) { volume = v.coerceIn(0f, 1f) }

    fun playNote(note: String, frequency: Double) {
        stopNote(note)
        val trackHolder = arrayOfNulls<AudioTrack>(1)
        val job = scope.launch {
            playTone(frequency, soundMode, trackHolder)
        }
        synchronized(noteLock) {
            activeNotes[note] = Pair(job, null)
        }
        job.invokeOnCompletion {
            synchronized(noteLock) { activeNotes.remove(note) }
        }
    }

    fun stopNote(note: String) {
        val pair = synchronized(noteLock) { activeNotes.remove(note) }
        pair?.first?.cancel()
        pair?.second?.let { track ->
            try { track.pause(); track.flush(); track.stop() } catch (_: Exception) {}
            try { track.release() } catch (_: Exception) {}
        }
    }

    fun stopAll() {
        val snapshot = synchronized(noteLock) {
            activeNotes.values.toList().also { activeNotes.clear() }
        }
        snapshot.forEach { (job, track) ->
            job.cancel()
            track?.let { t ->
                try { t.pause(); t.flush(); t.stop() } catch (_: Exception) {}
                try { t.release() } catch (_: Exception) {}
            }
        }
    }

    fun release() {
        stopAll()
        scope.cancel()
    }

    // ── 오디오 트랙 생성 및 재생 ───────────────────────────────────
    private suspend fun playTone(
        frequency: Double,
        mode: SoundMode,
        trackHolder: Array<AudioTrack?>
    ) = withContext(Dispatchers.IO) {
        val durationSec = 2.5f
        val totalSamples = (sampleRate * durationSec).toInt()

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufferBytes = maxOf(minBufSize, totalSamples * 4 * 2)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        trackHolder[0] = audioTrack

        val buffer = when (mode) {
            SoundMode.ORGAN -> generateOrganBuffer(frequency, totalSamples)
            else -> generateKarplusStrongBuffer(frequency, mode, totalSamples)
        }

        audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        audioTrack.play()

        try {
            delay((durationSec * 1000L).toLong())
        } finally {
            try { audioTrack.pause(); audioTrack.flush(); audioTrack.stop() } catch (_: Exception) {}
            try { audioTrack.release() } catch (_: Exception) {}
            trackHolder[0] = null
        }
    }

    /**
     * Karplus-Strong 물리 현 모델 합성
     *
     * 작동 원리:
     * 1. 링 버퍼를 노이즈로 초기화 (해머가 현을 두드리는 것과 동일)
     * 2. 인접 샘플을 평균내는 저역통과 필터를 반복 적용
     *    → 고주파 성분이 먼저 감쇠되어 자연스러운 피아노 음색 구현
     * 3. 링 버퍼 길이 = sampleRate / frequency → 정확한 음정 생성
     * 4. 감쇠 계수로 음의 지속 시간 제어 (낮은 음 = 오래 지속)
     */
    private fun generateKarplusStrongBuffer(
        frequency: Double,
        mode: SoundMode,
        totalSamples: Int
    ): FloatArray {
        // 딜레이 라인 길이 = 현의 진동 주기
        val N = (sampleRate.toDouble() / frequency).roundToInt().coerceIn(2, 8192)
        val rng = Random(System.nanoTime())

        // 링 버퍼 초기화: 랜덤 노이즈 (해머 타격 시뮬레이션)
        val ringBuf = FloatArray(N) { (rng.nextFloat() - 0.5f) * 2f }

        // 초기 노이즈에 저역통과 필터 적용 (음색 조정)
        // 반복 횟수가 많을수록 어두운/따뜻한 소리
        val filterPasses = ((1f - mode.brightness) * 10).roundToInt().coerceIn(1, 10)
        repeat(filterPasses) {
            // 전방향 패스
            for (i in 1 until N) {
                ringBuf[i] = ringBuf[i] * 0.5f + ringBuf[i - 1] * 0.5f
            }
            // 역방향 패스 (위상 왜곡 없는 필터링)
            for (i in N - 2 downTo 0) {
                ringBuf[i] = ringBuf[i] * 0.5f + ringBuf[i + 1] * 0.5f
            }
        }

        // 주파수 의존 반감기 (낮은 음일수록 오래 지속)
        val baseHalfLife = when {
            frequency < 110  -> 5.0
            frequency < 220  -> 3.5
            frequency < 440  -> 2.5   // 중간 C 근처
            frequency < 880  -> 1.6
            frequency < 1760 -> 1.0
            else             -> 0.65
        }
        val halfLifeSec = baseHalfLife * mode.halfLifeMult.coerceAtLeast(0.1f)

        // 링 버퍼 한 사이클(N 스텝)당 감쇠 계수
        // halfLifeSec * frequency 사이클 후 진폭이 0.5가 되도록 설정
        val decay = exp(-LN2 / (halfLifeSec * frequency)).toFloat().coerceIn(0.90f, 0.9999f)

        val stereoBuffer = FloatArray(totalSamples * 2)
        var pos = 0

        for (i in 0 until totalSamples) {
            val next = (pos + 1) % N

            // Karplus-Strong 핵심: 현재값과 다음값의 평균 × 감쇠 계수
            val ks = (ringBuf[pos] + ringBuf[next]) * 0.5f * decay
            val out = ringBuf[pos]
            ringBuf[pos] = ks
            pos = next

            // 해머 타격 트랜지언트: 초기 짧은 노이즈 버스트
            val transient = if (i < N * 4 && mode.transientLevel > 0f) {
                val tEnv = exp(-i.toFloat() / N * 3f)
                (rng.nextFloat() - 0.5f) * 2f * mode.transientLevel * tEnv
            } else 0f

            // 팝 방지 페이드인 (~3ms)
            val fadeIn = (i.toFloat() / (sampleRate * 0.003f)).coerceAtMost(1f)

            val finalSample = ((out + transient) * volume * fadeIn).coerceIn(-1f, 1f)
            stereoBuffer[i * 2]     = finalSample
            stereoBuffer[i * 2 + 1] = finalSample
        }

        return stereoBuffer
    }

    /**
     * 오르간: 가산 사인파 합성 (무한 서스테인, 감쇠 없음)
     */
    private fun generateOrganBuffer(frequency: Double, totalSamples: Int): FloatArray {
        val harmonics = listOf(1 to 1.0f, 2 to 0.80f, 3 to 0.50f, 4 to 0.30f, 8 to 0.15f)
        val normalizer = harmonics.sumOf { it.second.toDouble() }.toFloat()
        val stereoBuffer = FloatArray(totalSamples * 2)
        val attackS  = (sampleRate * 0.008f).toInt()
        val releaseS = (sampleRate * 0.020f).toInt()
        val sustainEnd = totalSamples - releaseS

        for (i in 0 until totalSamples) {
            val env = when {
                i < attackS    -> i.toFloat() / attackS
                i < sustainEnd -> 1.0f
                else           -> (totalSamples - i).toFloat() / releaseS
            }
            var sample = 0f
            for ((h, amp) in harmonics) {
                sample += amp * sin(2.0 * PI * frequency * h * i / sampleRate).toFloat()
            }
            val finalSample = (sample / normalizer) * env * volume * 0.80f
            stereoBuffer[i * 2]     = finalSample
            stereoBuffer[i * 2 + 1] = finalSample
        }
        return stereoBuffer
    }

    companion object {
        private val LN2 = ln(2.0)
    }
}
