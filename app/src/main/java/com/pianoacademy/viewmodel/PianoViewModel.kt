package com.pianoacademy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pianoacademy.audio.PianoSoundEngine
import com.pianoacademy.audio.SoundMode
import com.pianoacademy.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class PlayMode { FREE, AUTO, INTERACTIVE, PRACTICE }
enum class FallingMode { OFF, DOWN, UP }
enum class KeyboardLayout { SINGLE, DOUBLE, MIRROR }

data class PianoUiState(
    val selectedLevel: Int = 1,
    val selectedSong: Song? = null,
    val playMode: PlayMode = PlayMode.FREE,
    val fallingMode: FallingMode = FallingMode.OFF,
    val isPlaying: Boolean = false,
    val stepIndex: Int = 0,
    val activeKeys: Set<String> = emptySet(),
    val highlightKeys: Set<String> = emptySet(),
    val wrongKeys: Set<String> = emptySet(),
    val correctKeys: Set<String> = emptySet(),
    val waitingForInput: Boolean = false,
    val soundMode: SoundMode = SoundMode.GRAND,
    val volume: Float = 1.0f,
    val tempoMultiplier: Float = 1.0f,
    val noteNameMode: NoteNameMode = NoteNameMode.SOLFEGE,
    val showNextHint: Boolean = true,
    val showSettings: Boolean = false,
    val showSongPicker: Boolean = false,
    val isLandscape: Boolean = false,
    val gameResult: GameResult? = null,
    val bestScores: Map<String, Int> = emptyMap(),
    val wrongCount: Int = 0,
    val keyOctaveShift: Int = 0,
    val keyOctaveShift2: Int = 0,
    val keyboardLayout: KeyboardLayout = KeyboardLayout.SINGLE,
    val activeKeys2: Set<String> = emptySet(),
    val isSustainPedal: Boolean = false,
    val customSongs: List<Song> = emptyList()
)

data class GameResult(
    val accuracy: Int,
    val stars: Int,
    val wrongCount: Int,
    val prevBest: Int?
)

class PianoViewModel(app: Application) : AndroidViewModel(app) {

    private val soundEngine = PianoSoundEngine(app.applicationContext)
    private val _uiState = MutableStateFlow(PianoUiState())
    val uiState: StateFlow<PianoUiState> = _uiState.asStateFlow()

    private var autoPlayJob: Job? = null
    private var inputWaitJob: Job? = null
    private val bestScores = mutableMapOf<String, Int>()
    private var wrongCount = 0
    private val sustainedNotes = mutableSetOf<String>()
    private val sustainedNotes2 = mutableSetOf<String>()

    fun pressKey(note: String) {
        val freq = NOTE_MAP[note]?.frequency ?: noteToFrequency(note)
        soundEngine.playNote(note, freq)
        _uiState.update { it.copy(activeKeys = it.activeKeys + note) }
        val state = _uiState.value
        if (state.playMode == PlayMode.INTERACTIVE && state.isPlaying) {
            handleInteractiveInput(note)
        }
        if (state.playMode == PlayMode.PRACTICE && state.isPlaying) {
            handleInteractiveInput(note)
        }
    }

    fun shiftKeyboard(delta: Int) {
        _uiState.update { it.copy(keyOctaveShift = (it.keyOctaveShift + delta).coerceIn(-2, 2)) }
    }

    fun shiftKeyboard2(delta: Int) {
        _uiState.update { it.copy(keyOctaveShift2 = (it.keyOctaveShift2 + delta).coerceIn(-2, 2)) }
    }

    fun releaseKey(note: String, natural: Boolean = true) {
        if (_uiState.value.isSustainPedal) {
            // 페달 ON: 소리는 유지, 추적만 등록
            sustainedNotes.add(note)
        } else {
            soundEngine.stopNote(note, natural)
        }
        _uiState.update { it.copy(
            activeKeys = it.activeKeys - note,
            wrongKeys = it.wrongKeys - note,
            correctKeys = it.correctKeys - note
        )}
    }

    fun setKeyboardLayout(layout: KeyboardLayout) {
        // DOUBLE/MIRROR는 FREE 모드만 지원 → 다른 모드 중이면 FREE로 복원
        if (layout == KeyboardLayout.DOUBLE || layout == KeyboardLayout.MIRROR) {
            if (_uiState.value.playMode != PlayMode.FREE) stopPlayback()
            _uiState.update { it.copy(keyboardLayout = layout, playMode = PlayMode.FREE) }
        } else {
            _uiState.update { it.copy(keyboardLayout = layout) }
        }
    }

    // 두 번째 건반 (DOUBLE/MIRROR 모드 독립 동작)
    fun pressKey2(note: String) {
        val freq = NOTE_MAP[note]?.frequency ?: noteToFrequency(note)
        soundEngine.playNote(note, freq)
        _uiState.update { it.copy(activeKeys2 = it.activeKeys2 + note) }
    }

    fun releaseKey2(note: String, natural: Boolean = true) {
        if (_uiState.value.isSustainPedal) {
            sustainedNotes2.add(note)
        } else {
            soundEngine.stopNote(note, natural)
        }
        _uiState.update { it.copy(activeKeys2 = it.activeKeys2 - note) }
    }

    fun toggleSustainPedal() {
        val isActive = _uiState.value.isSustainPedal
        if (isActive) {
            // 페달 OFF: 유지 중이던 음 자연 감쇠로 정지
            sustainedNotes.forEach { soundEngine.stopNote(it, natural = true) }
            sustainedNotes.clear()
        }
        _uiState.update { it.copy(isSustainPedal = !it.isSustainPedal) }
    }

    private fun handleInteractiveInput(pressedNote: String) {
        val state = _uiState.value
        val song = state.selectedSong ?: return
        if (!state.waitingForInput) return
        val currentStep = song.steps.getOrNull(state.stepIndex) ?: return
        val shift = state.keyOctaveShift
        val expectedKeys = currentStep.keys.map { shiftNote(it, shift) }.toSet()

        if (expectedKeys.contains(pressedNote)) {
            _uiState.update { it.copy(
                correctKeys = it.correctKeys + pressedNote,
                wrongKeys = it.wrongKeys - pressedNote
            )}
            val allPressed = expectedKeys.all { k -> _uiState.value.correctKeys.contains(k) }
            if (allPressed) advanceStep()
        } else {
            wrongCount++
            _uiState.update { it.copy(
                wrongKeys = it.wrongKeys + pressedNote,
                wrongCount = wrongCount
            )}
        }
    }

    private fun advanceStep() {
        val state = _uiState.value
        val song = state.selectedSong ?: return
        val nextIdx = state.stepIndex + 1
        if (nextIdx >= song.steps.size) { finishSong(); return }
        val nextStep = song.steps[nextIdx]
        val shift = state.keyOctaveShift
        _uiState.update { it.copy(
            stepIndex = nextIdx,
            highlightKeys = nextStep.keys.map { shiftNote(it, shift) }.toSet(),
            waitingForInput = true,
            correctKeys = emptySet(),
            wrongKeys = emptySet()
        )}
    }

    fun startAutoPlay(song: Song) {
        stopPlayback()
        _uiState.update { it.copy(
            selectedSong = song,
            playMode = PlayMode.AUTO,
            isPlaying = true,
            stepIndex = 0
        )}
        autoPlayJob = viewModelScope.launch {
            val tempoMul = _uiState.value.tempoMultiplier
            val shift = _uiState.value.keyOctaveShift  // 시작 시 옥타브 캡처
            for ((index, step) in song.steps.withIndex()) {
                if (!isActive) break
                val beatMs = (60000.0 / song.tempo / tempoMul).toLong()
                val durationMs = (step.duration * beatMs).toLong()
                val shiftedKeys = step.keys.map { shiftNote(it, shift) }
                _uiState.update { it.copy(stepIndex = index, activeKeys = shiftedKeys.toSet()) }
                shiftedKeys.forEach { note ->
                    soundEngine.playNote(note, noteToFrequency(note))
                }
                delay(durationMs.coerceAtLeast(60))
            }
            _uiState.update { it.copy(activeKeys = emptySet()) }
            finishAutoPlay()
        }
    }

    fun startInteractive(song: Song, mode: PlayMode = PlayMode.INTERACTIVE) {
        stopPlayback()
        wrongCount = 0
        val firstStep = song.steps.firstOrNull()
        val shift = _uiState.value.keyOctaveShift  // 현재 옥타브 유지
        _uiState.update { it.copy(
            selectedSong = song,
            playMode = mode,
            isPlaying = true,
            stepIndex = 0,
            wrongCount = 0,
            highlightKeys = firstStep?.keys?.map { shiftNote(it, shift) }?.toSet() ?: emptySet(),
            waitingForInput = true,
            correctKeys = emptySet(),
            wrongKeys = emptySet()
        )}
    }

    fun stopPlayback() {
        autoPlayJob?.cancel()
        inputWaitJob?.cancel()
        sustainedNotes.clear()
        sustainedNotes2.clear()
        soundEngine.stopAll()
        _uiState.update { it.copy(
            isPlaying = false,
            activeKeys = emptySet(),
            activeKeys2 = emptySet(),
            highlightKeys = emptySet(),
            correctKeys = emptySet(),
            wrongKeys = emptySet(),
            waitingForInput = false
        )}
    }

    // 모드 버튼 직접 토글 (별도 시작버튼 없음)
    fun handleModeButtonClick(mode: PlayMode) {
        val state = _uiState.value
        when {
            mode == PlayMode.FREE -> {
                stopPlayback()
                _uiState.update { it.copy(playMode = PlayMode.FREE) }
            }
            state.playMode == mode && state.isPlaying -> {
                // 현재 재생중 모드 클릭 → 정지
                stopPlayback()
            }
            else -> {
                // 다른 모드 or 정지 상태 → 전환 후 시작
                stopPlayback()
                _uiState.update { it.copy(playMode = mode) }
                state.selectedSong?.let { song ->
                    when (mode) {
                        PlayMode.AUTO        -> startAutoPlay(song)
                        PlayMode.INTERACTIVE -> startInteractive(song, PlayMode.INTERACTIVE)
                        PlayMode.PRACTICE    -> startInteractive(song, PlayMode.PRACTICE)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun finishSong() {
        val state = _uiState.value
        val song = state.selectedSong ?: return
        val totalSteps = song.steps.size
        val accuracy = ((totalSteps - wrongCount).coerceAtLeast(0) * 100 / totalSteps)
        val stars = when {
            accuracy >= 95 -> 3; accuracy >= 75 -> 2; accuracy >= 50 -> 1; else -> 0
        }
        val prevBest = bestScores[song.id]
        if (prevBest == null || accuracy > prevBest) bestScores[song.id] = accuracy
        _uiState.update { it.copy(
            isPlaying = false,
            activeKeys = emptySet(),
            highlightKeys = emptySet(),
            gameResult = GameResult(accuracy, stars, wrongCount, prevBest),
            bestScores = bestScores.toMap()
        )}
    }

    private fun finishAutoPlay() {
        _uiState.update { it.copy(isPlaying = false, activeKeys = emptySet()) }
    }

    fun setPlayMode(mode: PlayMode) {
        stopPlayback()
        _uiState.update { it.copy(playMode = mode) }
    }

    fun selectLevel(level: Int) {
        stopPlayback()
        _uiState.update { it.copy(selectedLevel = level, selectedSong = null) }
    }

    fun selectSong(song: Song) {
        stopPlayback()
        _uiState.update { it.copy(selectedSong = song, stepIndex = 0) }
    }

    fun loadCustomSongs(songs: List<Song>) { _uiState.update { it.copy(customSongs = songs) } }
    fun setFallingMode(mode: FallingMode) { _uiState.update { it.copy(fallingMode = mode) } }
    fun setSoundMode(mode: SoundMode) { soundEngine.soundMode = mode; _uiState.update { it.copy(soundMode = mode) } }
    fun setVolume(v: Float) { soundEngine.setVolume(v); _uiState.update { it.copy(volume = v) } }
    fun setTempoMultiplier(t: Float) { _uiState.update { it.copy(tempoMultiplier = t) } }
    fun setNoteNameMode(mode: NoteNameMode) { _uiState.update { it.copy(noteNameMode = mode) } }
    fun toggleShowNextHint() { _uiState.update { it.copy(showNextHint = !it.showNextHint) } }
    fun toggleSettings() { _uiState.update { it.copy(showSettings = !it.showSettings) } }
    fun toggleSongPicker() { _uiState.update { it.copy(showSongPicker = !it.showSongPicker) } }
    fun dismissSongPicker() { _uiState.update { it.copy(showSongPicker = false) } }
    fun setLandscape(landscape: Boolean) { _uiState.update { it.copy(isLandscape = landscape) } }
    fun dismissGameResult() { _uiState.update { it.copy(gameResult = null) } }

    fun resetStep() {
        wrongCount = 0
        _uiState.update { it.copy(
            stepIndex = 0, wrongCount = 0, isPlaying = false,
            activeKeys = emptySet(), highlightKeys = emptySet(), gameResult = null
        )}
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.release()
    }
}
