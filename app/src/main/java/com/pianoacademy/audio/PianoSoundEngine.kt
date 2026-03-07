package com.pianoacademy.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*

// ── 사운드 모드 정의 ────────────────────────────────────────────
enum class SoundMode(
    val label: String,
    val icon: String,
    val attack: Float,
    val decay: Float,
    val sustain: Float,
    val release: Float,
    val harmonics: List<Pair<Int, Float>>,
    val reverbAmount: Float
) {
    GRAND(
        label = "그랜드", icon = "🎹",
        attack = 0.012f, decay = 0.30f, sustain = 0.68f, release = 0.40f,
        harmonics = listOf(1 to 1.0f, 2 to 0.45f, 3 to 0.20f, 4 to 0.10f, 6 to 0.05f),
        reverbAmount = 0.18f
    ),
    ELECTRIC(
        label = "일렉피아노", icon = "🎸",
        attack = 0.004f, decay = 0.18f, sustain = 0.80f, release = 0.22f,
        harmonics = listOf(1 to 1.0f, 2 to 0.60f, 3 to 0.30f, 5 to 0.10f),
        reverbAmount = 0.03f
    ),
    SOFT(
        label = "소프트", icon = "🌙",
        attack = 0.060f, decay = 0.80f, sustain = 0.50f, release = 0.60f,
        harmonics = listOf(1 to 1.0f, 2 to 0.20f),
        reverbAmount = 0.55f
    ),
    VIBRA(
        label = "비브라폰", icon = "🔔",
        attack = 0.001f, decay = 0.04f, sustain = 0.12f, release = 1.00f,
        harmonics = listOf(1 to 1.0f, 2 to 0.80f, 3 to 0.35f),
        reverbAmount = 0.30f
    ),
    ORGAN(
        label = "오르간", icon = "🎺",
        attack = 0.005f, decay = 0f, sustain = 1.0f, release = 0.02f,
        harmonics = listOf(1 to 1.0f, 2 to 0.80f, 3 to 0.50f, 4 to 0.30f, 8 to 0.15f),
        reverbAmount = 0.04f
    )
}

// ── 피아노 사운드 엔진 ────────────────────────────────────────────
class PianoSoundEngine {
    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // note → (Job, AudioTrack) 쌍으로 관리해서 즉시 정지 가능
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
        // Job 내부에서 AudioTrack 참조가 설정되면 map 업데이트
        job.invokeOnCompletion {
            synchronized(noteLock) { activeNotes.remove(note) }
        }
    }

    fun stopNote(note: String) {
        val pair = synchronized(noteLock) { activeNotes.remove(note) }
        pair?.first?.cancel()
        pair?.second?.let { track ->
            try {
                track.pause()
                track.flush()
                track.stop()
            } catch (_: Exception) {}
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

    // ── 합성음 생성 및 재생 ──────────────────────────────────────
    private suspend fun playTone(
        frequency: Double,
        mode: SoundMode,
        trackHolder: Array<AudioTrack?>
    ) = withContext(Dispatchers.IO) {
        // 최대 지속 시간 (ADSR 완료 기준, 짧게 조정)
        val durationSec = 1.8f
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

        // AudioTrack 참조 공유 (즉시 정지용)
        trackHolder[0] = audioTrack
        synchronized(noteLock) {
            // 이미 remove됐다면 (stopNote 경쟁) 즉시 중단
        }

        val buffer = generateToneBuffer(frequency, mode, totalSamples)
        audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        audioTrack.play()

        try {
            // 취소 가능한 대기
            delay((durationSec * 1000L).toLong())
        } finally {
            // 코루틴이 취소되거나 완료되면 반드시 정지
            try {
                audioTrack.pause()
                audioTrack.flush()
                audioTrack.stop()
            } catch (_: Exception) {}
            try { audioTrack.release() } catch (_: Exception) {}
            trackHolder[0] = null
        }
    }

    private fun generateToneBuffer(
        frequency: Double,
        mode: SoundMode,
        totalSamples: Int
    ): FloatArray {
        val stereoBuffer = FloatArray(totalSamples * 2)
        val attackSamples  = (mode.attack * sampleRate).toInt().coerceAtLeast(1)
        val decaySamples   = (mode.decay  * sampleRate).toInt()
        val releaseSamples = (mode.release * sampleRate).toInt().coerceAtLeast(1)
        val sustainEnd     = totalSamples - releaseSamples
        val normalizer     = mode.harmonics.sumOf { it.second.toDouble() }.toFloat()

        for (i in 0 until totalSamples) {
            val envelope = when {
                i < attackSamples -> i.toFloat() / attackSamples
                i < attackSamples + decaySamples -> {
                    val t = (i - attackSamples).toFloat() / decaySamples
                    1.0f - t * (1.0f - mode.sustain)
                }
                i < sustainEnd   -> mode.sustain
                else -> {
                    val t = (i - sustainEnd).toFloat() / releaseSamples
                    mode.sustain * (1.0f - t)
                }
            }

            var sample = 0f
            for ((harmonic, amplitude) in mode.harmonics) {
                val phase = 2.0 * PI * frequency * harmonic * i / sampleRate
                sample += amplitude * sin(phase).toFloat()
            }

            val finalSample = (sample / normalizer) * envelope * volume * 0.85f
            stereoBuffer[i * 2]     = finalSample
            stereoBuffer[i * 2 + 1] = finalSample
        }
        return stereoBuffer
    }
}
