package com.pianoacademy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pianoacademy.audio.PianoSoundEngine
import com.pianoacademy.audio.SoundMode
import com.pianoacademy.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

// ── 재생 모드 ──────────────────────────────────────────────────
enum class PlayMode { FREE, AUTO, INTERACTIVE }
enum class FallingMode { OFF, DOWN, UP }

// ── UI 상태 ────────────────────────────────────────────────────
data class PianoUiState(
    val selectedLevel: Int = 1,
    val selectedSong: Song? = null,
    val playMode: PlayMode = PlayMode.FREE,
    val fallingMode: FallingMode = FallingMode.OFF,
    val isPlaying: Boolean = false,
    val stepIndex: Int = 0,
    val activeKeys: Set<String> = emptySet(),
    val highlightKeys: Set<String> = emptySet(),  // 다음에 눌러야 할 키 (따라하기)
    val wrongKeys: Set<String> = emptySet(),
    val correctKeys: Set<String> = emptySet(),
    val waitingForInput: Boolean = false,
    val soundMode: SoundMode = SoundMode.GRAND,
    val volume: Float = 1.0f,
    val tempoMultiplier: Float = 1.0f,
    val showNoteNames: Boolean = true,
    val showNextHint: Boolean = true,
    val showSettings: Boolean = false,
    val showSongPicker: Boolean = false,
    val isLandscape: Boolean = false,
    val gameResult: GameResult? = null,
    val bestScores: Map<String, Int> = emptyMap(),
    val wrongCount: Int = 0
)

data class GameResult(
    val accuracy: Int,
    val stars: Int,
    val wrongCount: Int,
    val prevBest: Int?
)

// ── ViewModel ─────────────────────────────────────────────────
class PianoViewModel : ViewModel() {

    private val soundEngine = PianoSoundEngine()
    private val _uiState = MutableStateFlow(PianoUiState())
    val uiState: StateFlow<PianoUiState> = _uiState.asStateFlow()

    private var autoPlayJob: Job? = null
    private var inputWaitJob: Job? = null
    private val bestScores = mutableMapOf<String, Int>()
    private var wrongCount = 0

    // ── 건반 조작 ──────────────────────────────────────────────
    fun pressKey(note: String) {
        val freq = NOTE_MAP[note]?.frequency ?: return
        soundEngine.playNote(note, freq)
        _uiState.update { it.copy(activeKeys = it.activeKeys + note) }

        val state = _uiState.value
        if (state.playMode == PlayMode.INTERACTIVE && state.isPlaying) {
            handleInteractiveInput(note)
        }
    }

    fun releaseKey(note: String) {
        soundEngine.stopNote(note)
        _uiState.update { it.copy(
            activeKeys = it.activeKeys - note,
            wrongKeys = it.wrongKeys - note,
            correctKeys = it.correctKeys - note
        )}
    }

    // ── 인터랙티브 모드 입력 처리 ─────────────────────────────
    private fun handleInteractiveInput(pressedNote: String) {
        val state = _uiState.value
        val song = state.selectedSong ?: return
        if (!state.waitingForInput) return
        val currentStep = song.steps.getOrNull(state.stepIndex) ?: return
        val expectedKeys = currentStep.keys.toSet()

        if (expectedKeys.contains(pressedNote)) {
            // 정답
            _uiState.update { it.copy(
                correctKeys = it.correctKeys + pressedNote,
                wrongKeys = it.wrongKeys - pressedNote
            )}
            // 스텝의 모든 키를 눌렀는지 확인
            val allPressed = expectedKeys.all { k ->
                _uiState.value.correctKeys.contains(k)
            }
            if (allPressed) {
                advanceStep()
            }
        } else {
            // 오답
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

        if (nextIdx >= song.steps.size) {
            finishSong()
            return
        }

        val nextStep = song.steps[nextIdx]
        _uiState.update { it.copy(
            stepIndex = nextIdx,
            highlightKeys = nextStep.keys.toSet(),
            waitingForInput = true,
            correctKeys = emptySet(),
            wrongKeys = emptySet()
        )}
    }

    // ── 자동재생 모드 ─────────────────────────────────────────
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
            for ((index, step) in song.steps.withIndex()) {
                if (!isActive) break
                val beatMs = (60000.0 / song.tempo / tempoMul).toLong()
                val durationMs = (step.duration * beatMs).toLong()

                _uiState.update { it.copy(stepIndex = index, activeKeys = step.keys.toSet()) }
                step.keys.forEach { note ->
                    NOTE_MAP[note]?.let { nk -> soundEngine.playNote(note, nk.frequency) }
                }
                delay(durationMs.coerceAtLeast(80))
                _uiState.update { it.copy(activeKeys = emptySet()) }
                delay(30)
            }
            finishAutoPlay()
        }
    }

    // ── 따라하기 모드 시작 ────────────────────────────────────
    fun startInteractive(song: Song) {
        stopPlayback()
        wrongCount = 0
        val firstStep = song.steps.firstOrNull()
        _uiState.update { it.copy(
            selectedSong = song,
            playMode = PlayMode.INTERACTIVE,
            isPlaying = true,
            stepIndex = 0,
            wrongCount = 0,
            highlightKeys = firstStep?.keys?.toSet() ?: emptySet(),
            waitingForInput = true,
            correctKeys = emptySet(),
            wrongKeys = emptySet()
        )}
    }

    // ── 재생 중지 ─────────────────────────────────────────────
    fun stopPlayback() {
        autoPlayJob?.cancel()
        inputWaitJob?.cancel()
        soundEngine.stopAll()
        _uiState.update { it.copy(
            isPlaying = false,
            activeKeys = emptySet(),
            highlightKeys = emptySet(),
            correctKeys = emptySet(),
            wrongKeys = emptySet(),
            waitingForInput = false
        )}
    }

    // ── 곡 완료 처리 ─────────────────────────────────────────
    private fun finishSong() {
        val state = _uiState.value
        val song = state.selectedSong ?: return
        val totalSteps = song.steps.size
        val accuracy = ((totalSteps - wrongCount).coerceAtLeast(0) * 100 / totalSteps)
        val stars = when {
            accuracy >= 95 -> 3
            accuracy >= 75 -> 2
            accuracy >= 50 -> 1
            else -> 0
        }
        val prevBest = bestScores[song.id]
        if (prevBest == null || accuracy > prevBest) {
            bestScores[song.id] = accuracy
        }
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

    // ── 재생 모드 변경 ────────────────────────────────────────
    fun setPlayMode(mode: PlayMode) {
        stopPlayback()
        _uiState.update { it.copy(playMode = mode) }
    }

    // ── 상태 변경 ─────────────────────────────────────────────
    fun selectLevel(level: Int) {
        stopPlayback()
        _uiState.update { it.copy(selectedLevel = level, selectedSong = null) }
    }

    fun selectSong(song: Song) {
        stopPlayback()
        _uiState.update { it.copy(selectedSong = song, stepIndex = 0) }
    }

    fun setFallingMode(mode: FallingMode) {
        _uiState.update { it.copy(fallingMode = mode) }
    }

    fun setSoundMode(mode: SoundMode) {
        soundEngine.soundMode = mode
        _uiState.update { it.copy(soundMode = mode) }
    }

    fun setVolume(v: Float) {
        soundEngine.setVolume(v)
        _uiState.update { it.copy(volume = v) }
    }

    fun setTempoMultiplier(t: Float) {
        _uiState.update { it.copy(tempoMultiplier = t) }
    }

    fun toggleShowNoteNames() {
        _uiState.update { it.copy(showNoteNames = !it.showNoteNames) }
    }

    fun toggleShowNextHint() {
        _uiState.update { it.copy(showNextHint = !it.showNextHint) }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun toggleSongPicker() {
        _uiState.update { it.copy(showSongPicker = !it.showSongPicker) }
    }

    fun dismissSongPicker() {
        _uiState.update { it.copy(showSongPicker = false) }
    }

    fun setLandscape(landscape: Boolean) {
        _uiState.update { it.copy(isLandscape = landscape) }
    }

    fun dismissGameResult() {
        _uiState.update { it.copy(gameResult = null) }
    }

    fun resetStep() {
        val state = _uiState.value
        val song = state.selectedSong ?: return
        wrongCount = 0
        _uiState.update { it.copy(
            stepIndex = 0,
            wrongCount = 0,
            isPlaying = false,
            activeKeys = emptySet(),
            highlightKeys = emptySet(),
            gameResult = null
        )}
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.release()
    }
}
