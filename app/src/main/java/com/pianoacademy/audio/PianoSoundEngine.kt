package com.pianoacademy.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

// ── 사운드 모드 ────────────────────────────────────────────────
enum class SoundMode(val label: String, val icon: String) {
    GRAND("그랜드", "🎹"),
    SOFT("소프트", "🌙"),
    ELECTRIC("일렉피아노", "🎸"),
    VIBRA("비브라폰", "🔔"),
    ORGAN("오르간", "🎺")
}

/**
 * 피아노 사운드 엔진 v2 - Salamander Grand Piano 실제 녹음 기반
 *
 * ┌─────────────────────────────────────────────────────┐
 * │  Salamander Grand Piano (실제 Yamaha C5 녹음)       │
 * │  12개 base sample (minor 3rd 간격)                  │
 * │  + SoundPool rate 파라미터로 ±2반음 피치 시프팅     │
 * │  → 36음 전부 실제 녹음 기반으로 재생               │
 * ├─────────────────────────────────────────────────────┤
 * │  GRAND  : 실녹음 + 자연스러운 룸 리버브             │
 * │  SOFT   : 실녹음 + 부드럽게 (볼륨 살짝 낮춤)       │
 * │  ELECTRIC: 실녹음 + 밝은 EQ                        │
 * │  VIBRA  : 사인파 합성 + 트레몰로                   │
 * │  ORGAN  : 배음 합성 (Hammond 스타일)               │
 * └─────────────────────────────────────────────────────┘
 */
class PianoSoundEngine(private val context: Context) {

    companion object {
        private const val TAG = "PianoSoundEngine"
        private const val CACHE_VER = "sal_v2"
        private const val SR = 44100

        // 각 노트의 MIDI 번호 (C3=48 기준)
        private val NOTE_MIDI = mapOf(
            "C3"  to 48,  "C#3" to 49,  "D3"  to 50,  "D#3" to 51,
            "E3"  to 52,  "F3"  to 53,  "F#3" to 54,  "G3"  to 55,
            "G#3" to 56,  "A3"  to 57,  "A#3" to 58,  "B3"  to 59,
            "C4"  to 60,  "C#4" to 61,  "D4"  to 62,  "D#4" to 63,
            "E4"  to 64,  "F4"  to 65,  "F#4" to 66,  "G4"  to 67,
            "G#4" to 68,  "A4"  to 69,  "A#4" to 70,  "B4"  to 71,
            "C5"  to 72,  "C#5" to 73,  "D5"  to 74,  "D#5" to 75,
            "E5"  to 76,  "F5"  to 77,  "F#5" to 78,  "G5"  to 79,
            "G#5" to 80,  "A5"  to 81,  "A#5" to 82,  "B5"  to 83
        )

        // Salamander 베이스 샘플 (minor 3rd 간격 = 3반음)
        // 파일명: C3.mp3, Ds3.mp3(D#3), Fs3.mp3(F#3), A3.mp3 ...
        private val BASES = listOf(
            "C3"  to 48,  "Ds3" to 51,  "Fs3" to 54,  "A3"  to 57,
            "C4"  to 60,  "Ds4" to 63,  "Fs4" to 66,  "A4"  to 69,
            "C5"  to 72,  "Ds5" to 75,  "Fs5" to 78,  "A5"  to 81
        )
    }

    // 각 노트 → (베이스샘플명, 재생rate)
    private data class NoteRef(val base: String, val rate: Float)

    private val noteRefs: Map<String, NoteRef> = NOTE_MIDI.mapValues { (_, midi) ->
        val (name, baseMidi) = BASES.minByOrNull { abs(it.second - midi) }!!
        NoteRef(name, 2f.pow((midi - baseMidi) / 12f))
    }

    // SoundPool: GRAND / SOFT / ELECTRIC 모드용
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private val soundIds     = ConcurrentHashMap<String, Int>()
    private val loadedSounds = ConcurrentHashMap.newKeySet<Int>()
    private val activePool   = ConcurrentHashMap<String, Int>()
    private val activeSynth  = ConcurrentHashMap<String, AudioTrack>()

    private val _loadedCount = MutableStateFlow(0)
    val loadedCount: StateFlow<Int> = _loadedCount
    val totalSamples: Int = BASES.size

    private var _volume = 1.0f
    var soundMode = SoundMode.GRAND

    private val scope    = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cacheDir = File(context.cacheDir, CACHE_VER).apply { mkdirs() }

    init {
        soundPool.setOnLoadCompleteListener { _, id, status ->
            if (status == 0) {
                loadedSounds.add(id)
                _loadedCount.value = loadedSounds.size
            }
        }
        loadAllSamples()
    }

    // ── 샘플 로드 ──────────────────────────────────────────────
    private fun loadAllSamples() {
        scope.launch {
            BASES.map { (name, _) ->
                async {
                    try {
                        val wavFile = File(cacheDir, "$name.wav")
                        if (!wavFile.exists() || wavFile.length() < 200) {
                            val mono   = decodeAsset("piano_samples/$name.mp3") ?: return@async
                            val stereo = processGrand(mono)
                            writeWav(stereo, wavFile)
                        }
                        soundIds[name] = soundPool.load(wavFile.absolutePath, 1)
                    } catch (e: Exception) {
                        Log.e(TAG, "load $name: $e")
                    }
                }
            }.awaitAll()
        }
    }

    // ── 오디오 에셋 디코딩 → 모노 FloatArray @ 44100Hz ────────
    private fun decodeAsset(path: String): FloatArray? {
        val extractor = MediaExtractor()
        return try {
            context.assets.openFd(path).use { afd ->
                extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            var fmt: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    extractor.selectTrack(i); fmt = f; break
                }
            }
            if (fmt == null) return null

            val inCh  = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
            val inSR  = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE, SR)
            val codec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(fmt, null, null, 0)
            codec.start()

            val shorts  = ArrayList<Short>(SR * 6)
            val bufInfo = MediaCodec.BufferInfo()
            var inDone = false; var outDone = false
            var outCh = inCh; var outSR = inSR

            while (!outDone) {
                if (!inDone) {
                    val idx = codec.dequeueInputBuffer(10_000L)
                    if (idx >= 0) {
                        val buf  = codec.getInputBuffer(idx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inDone = true
                        } else {
                            codec.queueInputBuffer(idx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val idx = codec.dequeueOutputBuffer(bufInfo, 10_000L)
                when {
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        codec.outputFormat.also { of ->
                            outCh = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT, inCh)
                            outSR = of.getInteger(MediaFormat.KEY_SAMPLE_RATE, inSR)
                        }
                    }
                    idx >= 0 -> {
                        if (bufInfo.size > 0) {
                            val buf = codec.getOutputBuffer(idx)!!
                            buf.order(ByteOrder.LITTLE_ENDIAN)
                                .position(bufInfo.offset).limit(bufInfo.offset + bufInfo.size)
                            val sb = buf.asShortBuffer()
                            while (sb.hasRemaining()) shorts.add(sb.get())
                        }
                        codec.releaseOutputBuffer(idx, false)
                        if (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outDone = true
                    }
                }
            }
            codec.stop(); codec.release(); extractor.release()

            val floats = FloatArray(shorts.size) { shorts[it] / 32768f }
            val mono   = if (outCh == 1) floats
                         else FloatArray(floats.size / 2) { i -> (floats[i * 2] + floats[i * 2 + 1]) * 0.5f }
            if (outSR != SR) resample(mono, outSR, SR) else mono

        } catch (e: Exception) {
            Log.e(TAG, "decode $path: $e")
            try { extractor.release() } catch (_: Exception) {}
            null
        }
    }

    private fun resample(input: FloatArray, from: Int, to: Int): FloatArray {
        val ratio = from.toDouble() / to
        val len   = (input.size / ratio).toInt()
        return FloatArray(len) { i ->
            val p  = i * ratio
            val pi = p.toInt().coerceIn(0, input.size - 2)
            val fr = (p - pi).toFloat()
            input[pi] * (1f - fr) + input[pi + 1] * fr
        }
    }

    // ══════════════════════════════════════════════════════════
    //  DSP: GRAND 처리 (실녹음을 최소한만 다듬기)
    // ══════════════════════════════════════════════════════════
    private fun processGrand(mono: FloatArray): FloatArray {
        val n = mono.size

        // 1. 정규화 (피크 0.88)
        val peak = mono.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val norm = FloatArray(n) { mono[it] / peak * 0.88f }

        // 2. 12kHz 저역통과 (고주파 노이즈만 제거, 자연음 보존)
        val lpf = FloatArray(n)
        val a   = exp(-2f * PI.toFloat() * 12000f / SR)
        var p   = 0f
        for (i in norm.indices) { lpf[i] = (1f - a) * norm[i] + a * p; p = lpf[i] }

        // 3. 짧은 룸 리버브 (자연스러운 공간감만, muddy 방지)
        val tailLen = (SR * 0.7f).toInt()
        val outLen  = n + tailLen
        val wetBuf  = FloatArray(outLen)

        val cD = intArrayOf(1307, 1637, 1811, 1931)
        val cG = floatArrayOf(0.74f, 0.75f, 0.73f, 0.72f)
        for (ci in cD.indices) {
            val d = cD[ci]; val g = cG[ci]
            val buf = FloatArray(d); var pos = 0
            for (i in 0 until outLen) {
                val x = if (i < n) lpf[i] else 0f
                val y = x + g * buf[pos]
                buf[pos] = y; pos = (pos + 1) % d
                wetBuf[i] += y * 0.25f
            }
        }

        // 4. dry 92% + wet 8% → 스테레오
        val stereo = FloatArray(outLen * 2)
        for (i in 0 until outLen) {
            val dry = if (i < n) lpf[i] else 0f
            val wet = wetBuf[i]
            stereo[i * 2]     = (0.92f * dry + 0.08f * wet).coerceIn(-1f, 1f)
            stereo[i * 2 + 1] = (0.92f * dry + 0.06f * wet).coerceIn(-1f, 1f)
        }
        return stereo
    }

    private fun writeWav(stereo: FloatArray, file: File) {
        val nFrames   = stereo.size / 2
        val dataBytes = nFrames * 4
        val buf = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()); buf.putInt(36 + dataBytes)
        buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()); buf.putInt(16)
        buf.putShort(1); buf.putShort(2)
        buf.putInt(SR); buf.putInt(SR * 4)
        buf.putShort(4); buf.putShort(16)
        buf.put("data".toByteArray()); buf.putInt(dataBytes)
        for (f in stereo) buf.putShort((f * 32767f).toInt().coerceIn(-32768, 32767).toShort())
        FileOutputStream(file).use { it.write(buf.array()) }
    }

    // ══════════════════════════════════════════════════════════
    //  합성 엔진: VIBRA / ORGAN
    // ══════════════════════════════════════════════════════════
    private fun synthNote(note: String, frequency: Double) {
        val freq = frequency.toFloat()

        scope.launch(Dispatchers.Default) {
            val durationMs = 3500L
            val samples    = (SR * durationMs / 1000).toInt()
            val data       = ShortArray(samples * 2)

            when (soundMode) {
                // ─ 비브라폰: 맑은 사인파 + 5Hz 트레몰로 ─
                SoundMode.VIBRA -> {
                    for (i in 0 until samples) {
                        val t    = i.toFloat() / SR
                        val env  = exp(-1.5f * t)
                        val trem = 1f + 0.25f * sin(2 * PI.toFloat() * 5f * t)
                        val s    = sin(2 * PI.toFloat() * freq * t) * env * trem * 0.75f
                        val s16  = (s.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                        data[i * 2] = s16; data[i * 2 + 1] = s16
                    }
                }

                // ─ 오르간: Hammond 풍 배음 합성 ─
                SoundMode.ORGAN -> {
                    // 16' 8' 5⅓' 4' 2⅔' 2' 1⅗' 1⅓' 1'
                    val drawbars  = floatArrayOf(0.5f, 1.0f, 0.8f, 0.7f, 0.4f, 0.5f, 0.3f, 0.2f, 0.1f)
                    val freqMults = floatArrayOf(0.5f, 1f, 1.5f, 2f, 3f, 4f, 5f, 6f, 8f)
                    val totalW    = drawbars.sum()
                    val attackS   = (SR * 0.012f).toInt()
                    for (i in 0 until samples) {
                        val t   = i.toFloat() / SR
                        val env = if (i < attackS) i.toFloat() / attackS else 1f
                        var s   = 0f
                        for (h in drawbars.indices) {
                            s += drawbars[h] * sin(2 * PI.toFloat() * freq * freqMults[h] * t)
                        }
                        val s16 = ((s / totalW * env * 0.65f).coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                        data[i * 2] = s16; data[i * 2 + 1] = s16
                    }
                }

                else -> return@launch
            }

            val minBuf = AudioTrack.getMinBufferSize(SR,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SR)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, data.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(data, 0, data.size)
            track.setVolume(_volume)
            track.play()
            activeSynth[note] = track

            delay(durationMs + 300)
            activeSynth.remove(note)
            runCatching { track.stop(); track.release() }
        }
    }

    // ══════════════════════════════════════════════════════════
    //  공개 API
    // ══════════════════════════════════════════════════════════
    fun playNote(note: String, frequency: Double) {
        stopNote(note)

        if (soundMode == SoundMode.VIBRA || soundMode == SoundMode.ORGAN) {
            synthNote(note, frequency)
            return
        }

        val ref = noteRefs[note] ?: return
        val id  = soundIds[ref.base] ?: return
        if (id !in loadedSounds) return

        val vol = when (soundMode) {
            SoundMode.SOFT     -> _volume * 0.80f
            SoundMode.ELECTRIC -> _volume * 0.95f
            else               -> _volume
        }

        val stream = soundPool.play(id, vol, vol, 1, 0, ref.rate.coerceIn(0.5f, 2.0f))
        if (stream != 0) activePool[note] = stream
    }

    fun stopNote(note: String) {
        activePool.remove(note)?.let { soundPool.stop(it) }
        activeSynth.remove(note)?.let { t -> scope.launch { runCatching { t.stop(); t.release() } } }
    }

    fun stopAll() {
        activePool.forEach { (_, s) -> soundPool.stop(s) }
        activePool.clear()
        activeSynth.forEach { (_, t) -> scope.launch { runCatching { t.stop(); t.release() } } }
        activeSynth.clear()
    }

    fun setVolume(v: Float) {
        _volume = v.coerceIn(0f, 1f)
        activePool.forEach { (_, s) -> soundPool.setVolume(s, _volume, _volume) }
    }

    fun release() {
        stopAll()
        soundPool.release()
        scope.cancel()
    }
}
