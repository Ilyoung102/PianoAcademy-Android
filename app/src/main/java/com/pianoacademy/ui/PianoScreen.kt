package com.pianoacademy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pianoacademy.ui.components.*
import com.pianoacademy.viewmodel.*

@Composable
fun PianoScreen(
    vm: PianoViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF080A10), Color(0xFF0F1117), Color(0xFF111520))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                selectedSong = state.selectedSong,
                selectedLevel = state.selectedLevel,
                playMode = state.playMode,
                fallingMode = state.fallingMode,
                isPlaying = state.isPlaying,
                soundMode = state.soundMode,
                volume = state.volume,
                tempoMultiplier = state.tempoMultiplier,
                showSettings = state.showSettings,
                showNoteNames = state.showNoteNames,
                showNextHint = state.showNextHint,
                stepIndex = state.stepIndex,
                totalSteps = state.selectedSong?.steps?.size ?: 0,
                isLandscape = state.isLandscape,
                onSongPickerOpen = { vm.toggleSongPicker() },
                onModeButtonClick = { vm.handleModeButtonClick(it) },
                onFallingModeChange = { vm.setFallingMode(it) },
                onSoundModeChange = { vm.setSoundMode(it) },
                onVolumeChange = { vm.setVolume(it) },
                onTempoChange = { vm.setTempoMultiplier(it) },
                onToggleSettings = { vm.toggleSettings() },
                onToggleNoteNames = { vm.toggleShowNoteNames() },
                onToggleNextHint = { vm.toggleShowNextHint() }
            )

            ScoreArea(state, modifier = Modifier.weight(1f))

            PianoKeyboard(
                activeKeys = state.activeKeys,
                highlightKeys = when {
                    state.playMode == PlayMode.PRACTICE -> emptySet()
                    state.showNextHint -> state.highlightKeys
                    else -> emptySet()
                },
                wrongKeys = if (state.playMode == PlayMode.PRACTICE) emptySet() else state.wrongKeys,
                correctKeys = if (state.playMode == PlayMode.PRACTICE) emptySet() else state.correctKeys,
                showNoteNames = state.showNoteNames,
                isLandscape = state.isLandscape,
                onNoteOn = { vm.pressKey(it) },
                onNoteOff = { vm.releaseKey(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (state.isLandscape) 150.dp else 195.dp)
            )
        }

        state.gameResult?.let { result ->
            GameResultDialog(
                result = result,
                onRetry = {
                    vm.resetStep()
                    state.selectedSong?.let { song ->
                        when (state.playMode) {
                            PlayMode.AUTO        -> vm.startAutoPlay(song)
                            PlayMode.INTERACTIVE -> vm.startInteractive(song)
                            PlayMode.PRACTICE    -> vm.startInteractive(song, PlayMode.PRACTICE)
                            PlayMode.FREE        -> {}
                        }
                    }
                },
                onClose = { vm.dismissGameResult() }
            )
        }
    }

    if (state.showSongPicker) {
        SongPickerSheet(
            selectedLevel = state.selectedLevel,
            selectedSong = state.selectedSong,
            bestScores = state.bestScores,
            onLevelSelect = { vm.selectLevel(it) },
            onSongSelect = { song ->
                vm.selectSong(song)
                vm.dismissSongPicker()
                when (state.playMode) {
                    PlayMode.AUTO        -> vm.startAutoPlay(song)
                    PlayMode.INTERACTIVE -> vm.startInteractive(song)
                    PlayMode.PRACTICE    -> vm.startInteractive(song, PlayMode.PRACTICE)
                    PlayMode.FREE        -> {}
                }
            },
            onDismiss = { vm.dismissSongPicker() }
        )
    }
}

@Composable
private fun ScoreArea(state: PianoUiState, modifier: Modifier = Modifier) {
    val song = state.selectedSong
    if (song == null) {
        Box(
            modifier = modifier.fillMaxWidth().background(Color(0xFF0C0E14)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                "위 버튼을 눌러 곡을 선택하세요",
                color = Color(0xFF2E3450),
                fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
        return
    }
    when (state.fallingMode) {
        FallingMode.OFF -> SheetMusicView(
            song = song, stepIndex = state.stepIndex,
            playMode = state.playMode, modifier = modifier
        )
        FallingMode.DOWN, FallingMode.UP -> FallingNotesView(
            song = song, stepIndex = state.stepIndex,
            playMode = state.playMode, fallingMode = state.fallingMode,
            isLandscape = state.isLandscape, modifier = modifier
        )
    }
}
