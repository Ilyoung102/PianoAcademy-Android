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
    val harmonics: List<Pair<Int, Float>>,  // (배음차수, 음량비율)
    val reverbAmount: Float
) {
    GRAND(
        label = "그랜드", icon = "🎹",
        attack = 0.012f, decay = 0.35f, sustain = 0.72f, release = 0.50f,
        harmonics = listOf(1 to 1.0f, 2 to 0.45f, 3 to 0.20f, 4 to 0.10f, 6 to 0.05f),
        reverbAmount = 0.18f
    ),
    ELECTRIC(
        label = "일렉피아노", icon = "🎸",
        attack = 0.004f, decay = 0.20f, sustain = 0.85f, release = 0.28f,
        harmonics = listOf(1 to 1.0f, 2 to 0.60f, 3 to 0.30f, 5 to 0.10f),
        reverbAmount = 0.03f
    ),
    SOFT(
        label = "소프트", icon = "🌙",
        attack = 0.065f, decay = 0.90f, sustain = 0.55f, release = 0.75f,
        harmonics = listOf(1 to 1.0f, 2 to 0.20f),
        reverbAmount = 0.58f
    ),
    VIBRA(
        label = "비브라폰", icon = "🔔",
        attack = 0.001f, decay = 0.05f, sustain = 0.14f, release = 1.20f,
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
    private val activeNotes = mutableMapOf<String, Job>()
    private var volume = 1.0f
    var soundMode = SoundMode.GRAND

    fun setVolume(v: Float) { volume = v.coerceIn(0f, 1f) }

    // 음 재생 (비동기)
    fun playNote(note: String, frequency: Double) {
        stopNote(note)
        activeNotes[note] = scope.launch {
            playTone(frequency, soundMode)
        }
    }

    fun stopNote(note: String) {
        activeNotes[note]?.cancel()
        activeNotes.remove(note)
    }

    fun stopAll() {
        activeNotes.values.forEach { it.cancel() }
        activeNotes.clear()
    }

    fun release() {
        stopAll()
        scope.cancel()
    }

    // ── 합성음 생성 및 재생 ──────────────────────────────────────
    private suspend fun playTone(frequency: Double, mode: SoundMode) = withContext(Dispatchers.IO) {
        val durationSec = 2.5f  // 최대 지속 시간
        val totalSamples = (sampleRate * durationSec).toInt()

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
            .setBufferSizeInBytes(totalSamples * 4 * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        val buffer = generateToneBuffer(frequency, mode, totalSamples)
        audioTrack.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        audioTrack.play()

        delay((durationSec * 1000).toLong())

        audioTrack.stop()
        audioTrack.release()
    }

    private fun generateToneBuffer(
        frequency: Double,
        mode: SoundMode,
        totalSamples: Int
    ): FloatArray {
        val stereoBuffer = FloatArray(totalSamples * 2)
        val attackSamples = (mode.attack * sampleRate).toInt().coerceAtLeast(1)
        val decaySamples = (mode.decay * sampleRate).toInt()
        val releaseSamples = (mode.release * sampleRate).toInt().coerceAtLeast(1)
        val sustainEnd = totalSamples - releaseSamples

        for (i in 0 until totalSamples) {
            // ADSR 엔벨로프 계산
            val envelope = when {
                i < attackSamples ->
                    i.toFloat() / attackSamples
                i < attackSamples + decaySamples -> {
                    val t = (i - attackSamples).toFloat() / decaySamples
                    1.0f - t * (1.0f - mode.sustain)
                }
                i < sustainEnd ->
                    mode.sustain
                else -> {
                    val t = (i - sustainEnd).toFloat() / releaseSamples
                    mode.sustain * (1.0f - t)
                }
            }

            // 배음 합성
            var sample = 0f
            for ((harmonic, amplitude) in mode.harmonics) {
                val phase = 2.0 * PI * frequency * harmonic * i / sampleRate
                sample += amplitude * sin(phase).toFloat()
            }

            // 음량 정규화 및 적용
            val normalizer = mode.harmonics.sumOf { it.second.toDouble() }.toFloat()
            val finalSample = (sample / normalizer) * envelope * volume * 0.8f

            // 스테레오 출력
            stereoBuffer[i * 2] = finalSample
            stereoBuffer[i * 2 + 1] = finalSample
        }
        return stereoBuffer
    }
}
