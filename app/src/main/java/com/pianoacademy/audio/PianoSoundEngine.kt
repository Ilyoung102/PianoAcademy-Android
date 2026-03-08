package com.pianoacademy.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ── 사운드 모드 정의 ────────────────────────────────────────────
enum class SoundMode(
    val label: String,
    val icon: String
) {
    GRAND(label = "그랜드", icon = "🎹"),
    ELECTRIC(label = "일렉피아노", icon = "🎸"),
    SOFT(label = "소프트", icon = "🌙"),
    VIBRA(label = "비브라폰", icon = "🔔"),
    ORGAN(label = "오르간", icon = "🎺")
}

// ── 피아노 사운드 엔진 (실제 MP3 샘플 재생) ────────────────────
class PianoSoundEngine(private val context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    // 노트 이름 → 에셋 파일 이름 (반음은 플랫 표기)
    private val noteToAsset = mapOf(
        "C3" to "C3",   "C#3" to "Db3", "D3" to "D3",  "D#3" to "Eb3",
        "E3" to "E3",   "F3" to "F3",   "F#3" to "Gb3", "G3" to "G3",
        "G#3" to "Ab3", "A3" to "A3",   "A#3" to "Bb3", "B3" to "B3",
        "C4" to "C4",   "C#4" to "Db4", "D4" to "D4",  "D#4" to "Eb4",
        "E4" to "E4",   "F4" to "F4",   "F#4" to "Gb4", "G4" to "G4",
        "G#4" to "Ab4", "A4" to "A4",   "A#4" to "Bb4", "B4" to "B4",
        "C5" to "C5",   "C#5" to "Db5", "D5" to "D5",  "D#5" to "Eb5",
        "E5" to "E5",   "F5" to "F5",   "F#5" to "Gb5", "G5" to "G5",
        "G#5" to "Ab5", "A5" to "A5",   "A#5" to "Bb5", "B5" to "B5"
    )

    // soundId (SoundPool 로드 ID) 맵
    private val soundIds = mutableMapOf<String, Int>()
    // 로드 완료된 soundId 셋
    private val loadedSounds = mutableSetOf<Int>()
    // 현재 재생 중인 스트림 (note → streamId)
    private val activeStreams = mutableMapOf<String, Int>()

    private val _loadedCount = MutableStateFlow(0)
    val loadedCount: StateFlow<Int> = _loadedCount
    val totalSamples = noteToAsset.size

    private var _volume = 1.0f
    var soundMode = SoundMode.GRAND  // 현재 버전에서는 GRAND 음질 동일

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSounds.add(sampleId)
                _loadedCount.value = loadedSounds.size
            }
        }
        loadAllSamples()
    }

    private fun loadAllSamples() {
        noteToAsset.forEach { (note, assetName) ->
            try {
                val afd = context.assets.openFd("piano_samples/$assetName.mp3")
                val soundId = soundPool.load(afd, 1)
                soundIds[note] = soundId
                afd.close()
            } catch (e: Exception) {
                Log.e("PianoSoundEngine", "Failed to load $assetName.mp3: ${e.message}")
            }
        }
    }

    fun playNote(note: String, @Suppress("UNUSED_PARAMETER") frequency: Double) {
        stopNote(note)
        val soundId = soundIds[note] ?: return
        if (soundId !in loadedSounds) return  // 아직 로딩 중
        val streamId = soundPool.play(soundId, _volume, _volume, 1, 0, 1.0f)
        if (streamId != 0) activeStreams[note] = streamId
    }

    fun stopNote(note: String) {
        val streamId = activeStreams.remove(note) ?: return
        soundPool.stop(streamId)
    }

    fun stopAll() {
        activeStreams.forEach { (_, streamId) -> soundPool.stop(streamId) }
        activeStreams.clear()
    }

    fun setVolume(v: Float) {
        _volume = v.coerceIn(0f, 1f)
        activeStreams.forEach { (_, streamId) ->
            soundPool.setVolume(streamId, _volume, _volume)
        }
    }

    fun release() {
        stopAll()
        soundPool.release()
    }
}
