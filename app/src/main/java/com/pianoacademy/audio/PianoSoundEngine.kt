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
 * PianoSoundEngine v4 — 메모리 최적화 + DSP 버그 수정
 *
 * 수정 사항:
 *  - 한 번에 한 가지 모드 12개만 로드 (이전 36개→12개) → SoundPool OOM 해결 → 짧게 눌러도 소리 남
 *  - 음원 4초 제한 → WAV 파일 크기 감소 (~1.6MB→~860KB)
 *  - Allpass 필터 수식 수정 → 잡음 제거 (wet[i] = -0.5f*x + bv, 이전엔 +0.5f*bv 중복)
 *  - 최종 정규화 + 피크 제한 → 하드 클리핑 왜곡 제거
 *  - 모드 전환 시 자동 리로드 (var soundMode 커스텀 setter)
 */
class PianoSoundEngine(private val context: Context) {

    companion object {
        private const val TAG = "PianoSoundEngine"
        private const val CACHE_VER = "sal_v5"
        private const val SR = 44100
        private const val MAX_SAMP = SR * 4  // 음원 4초로 제한

        private val NOTE_MIDI = mapOf(
            "C3"  to 48, "C#3" to 49, "D3"  to 50, "D#3" to 51,
            "E3"  to 52, "F3"  to 53, "F#3" to 54, "G3"  to 55,
            "G#3" to 56, "A3"  to 57, "A#3" to 58, "B3"  to 59,
            "C4"  to 60, "C#4" to 61, "D4"  to 62, "D#4" to 63,
            "E4"  to 64, "F4"  to 65, "F#4" to 66, "G4"  to 67,
            "G#4" to 68, "A4"  to 69, "A#4" to 70, "B4"  to 71,
            "C5"  to 72, "C#5" to 73, "D5"  to 74, "D#5" to 75,
            "E5"  to 76, "F5"  to 77, "F#5" to 78, "G5"  to 79,
            "G#5" to 80, "A5"  to 81, "A#5" to 82, "B5"  to 83
        )

        private val BASES = listOf(
            "C3" to 48, "Ds3" to 51, "Fs3" to 54, "A3" to 57,
            "C4" to 60, "Ds4" to 63, "Fs4" to 66, "A4" to 69,
            "C5" to 72, "Ds5" to 75, "Fs5" to 78, "A5" to 81
        )

        private val SAMPLE_MODES = setOf(SoundMode.GRAND, SoundMode.SOFT, SoundMode.ELECTRIC)

        // 웹 SOUND_MODES 파라미터 기준, rt60은 파일 크기 절약을 위해 단축
        private val GRAND_PARAMS    = DspParams(6500f, 1100f, 1.8f, 8f, 300f, 3f, 1200f, 0.15f, 1.5f, false, 0f)
        private val SOFT_PARAMS     = DspParams( 780f,  210f, 1.5f, 4f, 250f, 2f, 1200f, 0.45f, 2.0f, false, 0f)
        private val ELECTRIC_PARAMS = DspParams(2600f, 2000f, 0.6f, 2f, 400f, 2f, 1200f, 0.03f, 0.4f, true,  2.2f)
    }

    private data class DspParams(
        val lpStart: Float, val lpEnd: Float, val sweepSec: Float,
        val bassDb:  Float, val bassHz: Float,
        val peakDb:  Float, val peakHz: Float,
        val reverbWet: Float, val rt60: Float,
        val bandpass:  Boolean, val bpQ: Float
    )

    private data class NoteRef(val base: String, val rate: Float)

    private val noteRefs: Map<String, NoteRef> = NOTE_MIDI.mapValues { (_, midi) ->
        val (name, baseMidi) = BASES.minByOrNull { abs(it.second - midi) }!!
        NoteRef(name, 2f.pow((midi - baseMidi) / 12f))
    }

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(16)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    // 현재 모드 12개 샘플만 유지
    private val soundIds     = ConcurrentHashMap<String, Int>()
    private val loadedSounds = ConcurrentHashMap.newKeySet<Int>()
    private val activePool   = ConcurrentHashMap<String, Int>()
    private val activeSynth  = ConcurrentHashMap<String, AudioTrack>()

    private val _loadedCount = MutableStateFlow(0)
    val loadedCount: StateFlow<Int> = _loadedCount
    val totalSamples: Int = BASES.size   // 12

    private var _volume = 1.0f
    private var loadJob: Job? = null

    // 모드 전환 시 자동 리로드
    var soundMode: SoundMode = SoundMode.GRAND
        set(value) {
            if (field == value) return
            field = value
            if (value in SAMPLE_MODES) loadModeAsync(value)
        }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val rootCache = File(context.cacheDir, CACHE_VER).apply { mkdirs() }

    init {
        soundPool.setOnLoadCompleteListener { _, id, status ->
            if (status == 0) {
                loadedSounds.add(id)
                _loadedCount.value = loadedSounds.size
            }
        }
        loadModeAsync(SoundMode.GRAND)
    }

    // ── 모드별 샘플 로드 ────────────────────────────────────────
    private fun loadModeAsync(mode: SoundMode) {
        loadJob?.cancel()
        // 이전 모드 언로드
        soundIds.values.forEach { soundPool.unload(it) }
        soundIds.clear()
        loadedSounds.clear()
        _loadedCount.value = 0

        loadJob = scope.launch {
            BASES.map { (name, _) ->
                async { loadOneSample(name, mode) }
            }.awaitAll()
        }
    }

    private suspend fun loadOneSample(name: String, mode: SoundMode) {
        val params = when (mode) {
            SoundMode.GRAND    -> GRAND_PARAMS
            SoundMode.SOFT     -> SOFT_PARAMS
            SoundMode.ELECTRIC -> ELECTRIC_PARAMS
            else               -> return
        }
        val dir     = File(rootCache, mode.name.lowercase()).apply { mkdirs() }
        val wavFile = File(dir, "$name.wav")
        try {
            if (!wavFile.exists() || wavFile.length() < 200) {
                val raw  = decodeAsset("piano_samples/$name.mp3") ?: return
                val mono = raw.copyOfRange(0, minOf(raw.size, MAX_SAMP))
                val out  = processWithParams(mono, params)
                writeWav(out, wavFile)
            }
            val id = soundPool.load(wavFile.absolutePath, 1)
            soundIds[name] = id
        } catch (e: Exception) {
            Log.e(TAG, "load $mode/$name: $e")
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
            var inDone  = false; var outDone = false
            var outCh   = inCh;  var outSR   = inSR

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
                         else FloatArray(floats.size / 2) { i -> (floats[i*2] + floats[i*2+1]) * 0.5f }
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
    //  DSP 처리 — 웹 Web Audio API와 동일한 파라미터
    // ══════════════════════════════════════════════════════════
    private fun processWithParams(mono: FloatArray, p: DspParams): FloatArray {
        val n = mono.size

        // ─ 1. 입력 정규화 ─────────────────────────────────────
        val peak = mono.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val norm = FloatArray(n) { mono[it] / peak * 0.80f }

        // ─ 2. 필터 스위프 ─────────────────────────────────────
        val filtered = if (p.bandpass)
            applyBandpassSweep(norm, p.lpStart, p.lpEnd, p.sweepSec, p.bpQ)
        else
            applyLpfSweep(norm, p.lpStart, p.lpEnd, p.sweepSec)

        // ─ 3. 저음 쉘프 부스트 ────────────────────────────────
        //    y[n] = x[n] + (A-1)*lp[n]
        val bassAmp = 10f.pow(p.bassDb / 20f)
        val bassA   = exp(-2f * PI.toFloat() * p.bassHz / SR)
        val bassBoosted = FloatArray(n)
        var bsLp = 0f
        for (i in filtered.indices) {
            bsLp = (1f - bassA) * filtered[i] + bassA * bsLp
            bassBoosted[i] = filtered[i] + (bassAmp - 1f) * bsLp
        }

        // ─ 4. 피킹 EQ ─────────────────────────────────────────
        val peakAmp = 10f.pow(p.peakDb / 20f)
        val bw      = p.peakHz / 1f
        val aLo     = exp(-2f * PI.toFloat() * (p.peakHz - bw * 0.5f).coerceAtLeast(1f) / SR)
        val aHi     = exp(-2f * PI.toFloat() * (p.peakHz + bw * 0.5f) / SR)
        val peaked  = FloatArray(n)
        var pkLo = 0f; var pkHi = 0f
        for (i in bassBoosted.indices) {
            pkLo = (1f - aLo) * bassBoosted[i] + aLo * pkLo
            pkHi = (1f - aHi) * bassBoosted[i] + aHi * pkHi
            peaked[i] = bassBoosted[i] + (peakAmp - 1f) * (pkHi - pkLo)
        }

        // ─ 5. Schroeder 리버브 ─────────────────────────────────
        val tailLen = (SR * p.rt60 * 0.5f).toInt().coerceAtMost(SR * 2)
        val outLen  = n + tailLen
        val wet     = FloatArray(outLen)

        val combDelays = intArrayOf(1307, 1637, 1811, 1931)
        for (d in combDelays) {
            val g   = 10f.pow(-3f * d.toFloat() / (SR * p.rt60))
            val buf = FloatArray(d); var pos = 0
            for (i in 0 until outLen) {
                val x = if (i < n) peaked[i] else 0f
                val y = x + g * buf[pos]
                buf[pos] = y; pos = (pos + 1) % d
                wet[i] += y * 0.25f
            }
        }

        // Allpass (수정: 올바른 Schroeder allpass 수식)
        // y[n] = -g*x[n] + x[n-D] + g*y[n-D]  (g=0.5)
        for (apD in intArrayOf(225, 77)) {
            val buf = FloatArray(apD); var pos = 0
            val tmp = wet.copyOf()
            for (i in tmp.indices) {
                val x  = tmp[i]
                val bv = buf[pos]
                wet[i]   = -0.5f * x + bv   // 수정: 이전 코드의 (+0.5f*bv 오류) 제거
                buf[pos] = x + 0.5f * bv
                pos = (pos + 1) % apD
            }
        }

        // ─ 6. Dry + Wet 믹스 → 스테레오 ─────────────────────
        val stereo  = FloatArray(outLen * 2)
        val dryGain = 1f - p.reverbWet
        for (i in 0 until outLen) {
            val dry = if (i < n) peaked[i] else 0f
            val s   = dryGain * dry + p.reverbWet * wet[i]
            stereo[i * 2]     = s
            stereo[i * 2 + 1] = s
        }

        // ─ 7. 최종 정규화 (클리핑 왜곡 방지) ─────────────────
        val maxPeak = stereo.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val normGain = (0.85f / maxPeak).coerceAtMost(1.5f)
        for (i in stereo.indices) {
            // 소프트 클리핑: tanh 근사
            val v = stereo[i] * normGain
            stereo[i] = if (abs(v) <= 1f) v
                        else sign(v) * (1f - exp(-abs(v)))
        }
        return stereo
    }

    // LPF 지수 스위프: web exponentialRampToValueAtTime 동일
    private fun applyLpfSweep(sig: FloatArray, fcStart: Float, fcEnd: Float, sweepSec: Float): FloatArray {
        val result = FloatArray(sig.size)
        var prev   = 0f
        val ratio  = fcEnd / fcStart
        for (i in sig.indices) {
            val t  = i.toFloat() / SR
            val p  = (t / sweepSec).coerceIn(0f, 1f)
            val fc = fcStart * ratio.pow(p)
            val a  = exp(-2f * PI.toFloat() * fc / SR)
            val out = (1f - a) * sig[i] + a * prev
            result[i] = out; prev = out
        }
        return result
    }

    // 밴드패스 스위프 (일렉피아노)
    private fun applyBandpassSweep(sig: FloatArray, fcStart: Float, fcEnd: Float, sweepSec: Float, Q: Float): FloatArray {
        val result = FloatArray(sig.size)
        var lpPrev = 0f; var hpPrev = 0f; var hpX = 0f
        val ratio  = fcEnd / fcStart
        for (i in sig.indices) {
            val t  = i.toFloat() / SR
            val p  = (t / sweepSec).coerceIn(0f, 1f)
            val fc = fcStart * ratio.pow(p)
            val bw = fc / Q
            val aLp = exp(-2f * PI.toFloat() * (fc + bw * 0.5f) / SR)
            val aHp = exp(-2f * PI.toFloat() * (fc - bw * 0.5f).coerceAtLeast(1f) / SR)
            val hp  = aHp * (hpPrev + sig[i] - hpX)
            hpPrev = hp; hpX = sig[i]
            val bp  = (1f - aLp) * hp + aLp * lpPrev
            lpPrev = bp
            result[i] = bp
        }
        return result
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
    //  합성: VIBRA / ORGAN (AudioTrack)
    // ══════════════════════════════════════════════════════════
    private fun synthNote(note: String, frequency: Double) {
        val freq = frequency.toFloat()
        scope.launch(Dispatchers.Default) {
            val durMs  = 3500L
            val nSamp  = (SR * durMs / 1000).toInt()
            val data   = ShortArray(nSamp * 2)

            when (soundMode) {
                SoundMode.VIBRA -> {
                    for (i in 0 until nSamp) {
                        val t    = i.toFloat() / SR
                        val env  = exp(-1.5f * t)
                        val trem = 1f + 0.25f * sin(2 * PI.toFloat() * 5f * t)
                        val s    = sin(2 * PI.toFloat() * freq * t) * env * trem * 0.75f
                        val s16  = (s.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                        data[i * 2] = s16; data[i * 2 + 1] = s16
                    }
                }
                SoundMode.ORGAN -> {
                    val drawbars  = floatArrayOf(0.5f, 1.0f, 0.8f, 0.7f, 0.4f, 0.5f, 0.3f, 0.2f, 0.1f)
                    val freqMults = floatArrayOf(0.5f, 1f, 1.5f, 2f, 3f, 4f, 5f, 6f, 8f)
                    val totalW    = drawbars.sum()
                    val attackS   = (SR * 0.012f).toInt()
                    for (i in 0 until nSamp) {
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
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SR).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, data.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC).build()

            track.write(data, 0, data.size)
            track.setVolume(_volume)
            track.play()
            activeSynth[note] = track
            delay(durMs + 300)
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
            synthNote(note, frequency); return
        }

        val ref    = noteRefs[note] ?: return
        val id     = soundIds[ref.base] ?: return
        if (id !in loadedSounds) return

        val stream = soundPool.play(id, _volume, _volume, 1, 0, ref.rate.coerceIn(0.5f, 2.0f))
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
        loadJob?.cancel()
        soundPool.release()
        scope.cancel()
    }
}
