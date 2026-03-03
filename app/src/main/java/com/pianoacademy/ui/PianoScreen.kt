package com.pianoacademy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pianoacademy.data.SONGS
import com.pianoacademy.ui.components.*
import com.pianoacademy.viewmodel.*

// ── 메인 피아노 스크린 ─────────────────────────────────────────
@Composable
fun PianoScreen(
    vm: PianoViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
    ) {
        if (state.isLandscape) {
            // ── 가로 모드 ─────────────────────────────────
            Row(modifier = Modifier.fillMaxSize()) {
                // 왼쪽: 곡 목록
                SongPanel(
                    selectedLevel = state.selectedLevel,
                    selectedSong = state.selectedSong,
                    bestScores = state.bestScores,
                    onLevelSelect = { vm.selectLevel(it) },
                    onSongSelect = { song ->
                        vm.selectSong(song)
                        when (state.playMode) {
                            PlayMode.AUTO -> vm.startAutoPlay(song)
                            PlayMode.INTERACTIVE -> vm.startInteractive(song)
                            else -> {}
                        }
                    },
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                )

                // 오른쪽: 악보 + 건반
                Column(modifier = Modifier.fillMaxSize()) {
                    // 상단 컨트롤
                    TopBar(
                        selectedSong = state.selectedSong,
                        playMode = state.playMode,
                        fallingMode = state.fallingMode,
                        isPlaying = state.isPlaying,
                        soundMode = state.soundMode,
                        volume = state.volume,
                        tempoMultiplier = state.tempoMultiplier,
                        showSettings = state.showSettings,
                        showNoteNames = state.showNoteNames,
                        showNextHint = state.showNextHint,
                        onPlayModeChange = { mode ->
                            vm.stopPlayback()
                            vm.uiState.value.let { /* 모드만 변경 */ }
                            // 직접 ViewModel에 모드 변경 요청
                        },
                        onFallingModeChange = { vm.setFallingMode(it) },
                        onPlayStop = {
                            if (state.isPlaying) {
                                vm.stopPlayback()
                            } else {
                                state.selectedSong?.let { song ->
                                    when (state.playMode) {
                                        PlayMode.AUTO -> vm.startAutoPlay(song)
                                        PlayMode.INTERACTIVE -> vm.startInteractive(song)
                                        else -> {}
                                    }
                                }
                            }
                        },
                        onSoundModeChange = { vm.setSoundMode(it) },
                        onVolumeChange = { vm.setVolume(it) },
                        onTempoChange = { vm.setTempoMultiplier(it) },
                        onToggleSettings = { vm.toggleSettings() },
                        onToggleNoteNames = { vm.toggleShowNoteNames() },
                        onToggleNextHint = { vm.toggleShowNextHint() }
                    )

                    // 악보/폭포수 영역
                    ScoreArea(state, modifier = Modifier.weight(1f))

                    // 피아노 건반
                    PianoKeyboard(
                        activeKeys = state.activeKeys,
                        highlightKeys = if (state.showNextHint) state.highlightKeys else emptySet(),
                        wrongKeys = state.wrongKeys,
                        correctKeys = state.correctKeys,
                        showNoteNames = state.showNoteNames,
                        isLandscape = true,
                        onNoteOn = { vm.pressKey(it) },
                        onNoteOff = { vm.releaseKey(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            }
        } else {
            // ── 세로 모드 ─────────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {
                // 상단 컨트롤
                TopBar(
                    selectedSong = state.selectedSong,
                    playMode = state.playMode,
                    fallingMode = state.fallingMode,
                    isPlaying = state.isPlaying,
                    soundMode = state.soundMode,
                    volume = state.volume,
                    tempoMultiplier = state.tempoMultiplier,
                    showSettings = state.showSettings,
                    showNoteNames = state.showNoteNames,
                    showNextHint = state.showNextHint,
                    onPlayModeChange = { /* ViewModel에 전달 */ },
                    onFallingModeChange = { vm.setFallingMode(it) },
                    onPlayStop = {
                        if (state.isPlaying) {
                            vm.stopPlayback()
                        } else {
                            state.selectedSong?.let { song ->
                                when (state.playMode) {
                                    PlayMode.AUTO -> vm.startAutoPlay(song)
                                    PlayMode.INTERACTIVE -> vm.startInteractive(song)
                                    else -> {}
                                }
                            }
                        }
                    },
                    onSoundModeChange = { vm.setSoundMode(it) },
                    onVolumeChange = { vm.setVolume(it) },
                    onTempoChange = { vm.setTempoMultiplier(it) },
                    onToggleSettings = { vm.toggleSettings() },
                    onToggleNoteNames = { vm.toggleShowNoteNames() },
                    onToggleNextHint = { vm.toggleShowNextHint() }
                )

                // 곡 목록 (세로에서는 상단에 축소형으로)
                SongPanel(
                    selectedLevel = state.selectedLevel,
                    selectedSong = state.selectedSong,
                    bestScores = state.bestScores,
                    onLevelSelect = { vm.selectLevel(it) },
                    onSongSelect = { song ->
                        vm.selectSong(song)
                        when (state.playMode) {
                            PlayMode.AUTO -> vm.startAutoPlay(song)
                            PlayMode.INTERACTIVE -> vm.startInteractive(song)
                            else -> {}
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )

                // 악보/폭포수
                ScoreArea(state, modifier = Modifier.weight(1f))

                // 건반
                PianoKeyboard(
                    activeKeys = state.activeKeys,
                    highlightKeys = if (state.showNextHint) state.highlightKeys else emptySet(),
                    wrongKeys = state.wrongKeys,
                    correctKeys = state.correctKeys,
                    showNoteNames = state.showNoteNames,
                    isLandscape = false,
                    onNoteOn = { vm.pressKey(it) },
                    onNoteOff = { vm.releaseKey(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        // ── 게임 결과 모달 ─────────────────────────────────
        state.gameResult?.let { result ->
            GameResultDialog(
                result = result,
                onRetry = {
                    vm.resetStep()
                    state.selectedSong?.let { song ->
                        when (state.playMode) {
                            PlayMode.AUTO -> vm.startAutoPlay(song)
                            PlayMode.INTERACTIVE -> vm.startInteractive(song)
                            else -> {}
                        }
                    }
                },
                onClose = { vm.dismissGameResult() }
            )
        }
    }
}

// ── 악보/폭포수 영역 분기 ─────────────────────────────────────
@Composable
private fun ScoreArea(state: com.pianoacademy.viewmodel.PianoUiState, modifier: Modifier = Modifier) {
    val song = state.selectedSong
    if (song == null) {
        // 곡 미선택 상태
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFF15171E)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text(
                "곡을 선택하면 악보가 표시됩니다",
                color = Color(0xFF4A5568),
                fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
        return
    }

    when (state.fallingMode) {
        FallingMode.OFF -> {
            // 수평 악보 뷰 (간단 버전)
            SheetMusicView(
                song = song,
                stepIndex = state.stepIndex,
                playMode = state.playMode,
                modifier = modifier
            )
        }
        FallingMode.DOWN, FallingMode.UP -> {
            FallingNotesView(
                song = song,
                stepIndex = state.stepIndex,
                playMode = state.playMode,
                fallingMode = state.fallingMode,
                isLandscape = state.isLandscape,
                modifier = modifier
            )
        }
    }
}
