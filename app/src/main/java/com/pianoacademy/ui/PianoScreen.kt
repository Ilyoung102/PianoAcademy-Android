package com.pianoacademy.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pianoacademy.viewmodel.KeyboardLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pianoacademy.data.parseMdSongs
import com.pianoacademy.ui.components.*
import com.pianoacademy.viewmodel.*

@Composable
fun PianoScreen(
    vm: PianoViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // MD 파일 로드 런처
    val mdFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                content?.let { text ->
                    val songs = parseMdSongs(text)
                    if (songs.isNotEmpty()) vm.loadCustomSongs(songs)
                }
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF080A10), Color(0xFF0F1117), Color(0xFF111520))
                )
            )
    ) {
        val topBarContent: @Composable () -> Unit = {
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
                noteNameMode = state.noteNameMode,
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
                onNoteNameModeChange = { vm.setNoteNameMode(it) },
                onToggleNextHint = { vm.toggleShowNextHint() },
                keyOctaveShift = state.keyOctaveShift,
                onShiftKeyboard = { vm.shiftKeyboard(it) },
                isSustainPedal = state.isSustainPedal,
                onToggleSustainPedal = { vm.toggleSustainPedal() },
                onLoadMdFile = { mdFileLauncher.launch(arrayOf("text/*", "text/plain", "text/markdown")) },
                keyboardLayout = state.keyboardLayout,
                onKeyboardLayoutChange = { vm.setKeyboardLayout(it) }
            )
        }

        // 건반1 (메인, 항상 vm.pressKey 사용)
        val keyboardContent: @Composable (Boolean, Modifier) -> Unit = { mirror, mod ->
            PianoKeyboard(
                activeKeys = state.activeKeys,
                highlightKeys = when {
                    state.playMode == PlayMode.PRACTICE -> emptySet()
                    state.showNextHint -> state.highlightKeys
                    else -> emptySet()
                },
                wrongKeys = if (state.playMode == PlayMode.PRACTICE) emptySet() else state.wrongKeys,
                correctKeys = if (state.playMode == PlayMode.PRACTICE) emptySet() else state.correctKeys,
                noteNameMode = state.noteNameMode,
                isLandscape = state.isLandscape,
                octaveShift = state.keyOctaveShift,
                isMirror = mirror,
                onNoteOn = { vm.pressKey(it) },
                onNoteOff = { note, natural -> vm.releaseKey(note, natural) },
                modifier = mod
            )
        }

        // 건반2 (DOUBLE/MIRROR 두 번째 건반, 독립 상태)
        val keyboardContent2: @Composable (Boolean, Modifier) -> Unit = { mirror, mod ->
            PianoKeyboard(
                activeKeys = state.activeKeys2,
                highlightKeys = emptySet(),
                wrongKeys = emptySet(),
                correctKeys = emptySet(),
                noteNameMode = state.noteNameMode,
                isLandscape = state.isLandscape,
                octaveShift = state.keyOctaveShift,
                isMirror = mirror,
                onNoteOn = { vm.pressKey2(it) },
                onNoteOff = { note, natural -> vm.releaseKey2(note, natural) },
                modifier = mod
            )
        }

        val fixedKbHeight = if (state.isLandscape) 173.dp else 234.dp

        when {
            // ── 마주 보기: [건반2 뒤집힘] [TopBar 중앙] [건반1 일반] ──
            state.playMode == PlayMode.FREE && state.keyboardLayout == KeyboardLayout.MIRROR -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    keyboardContent2(true, Modifier.fillMaxWidth().weight(1f))
                    HorizontalDivider(color = Color(0xFF252840), thickness = 1.dp)
                    topBarContent()
                    HorizontalDivider(color = Color(0xFF252840), thickness = 1.dp)
                    keyboardContent(false, Modifier.fillMaxWidth().weight(1f))
                }
            }
            // ── 2개 건반: TopBar + [건반1] + [건반2] ──
            state.playMode == PlayMode.FREE && state.keyboardLayout == KeyboardLayout.DOUBLE -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    topBarContent()
                    keyboardContent(false, Modifier.fillMaxWidth().weight(1f))
                    HorizontalDivider(color = Color(0xFF1A1D2A), thickness = 1.dp)
                    keyboardContent2(false, Modifier.fillMaxWidth().weight(1f))
                }
            }
            // ── 자유 모드 1개: TopBar + 건반(90%) + 여백(10%) ──
            state.playMode == PlayMode.FREE -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    topBarContent()
                    keyboardContent(false, Modifier.fillMaxWidth().weight(9f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            // ── 일반 모드: TopBar + ScoreArea + 건반(고정) ──
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    topBarContent()
                    ScoreArea(state, modifier = Modifier.weight(1f))
                    keyboardContent(false, Modifier.fillMaxWidth().height(fixedKbHeight))
                }
            }
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
            customSongs = state.customSongs,
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
            playMode = state.playMode,
            isPlaying = state.isPlaying,
            tempoMultiplier = state.tempoMultiplier,
            isLandscape = state.isLandscape,
            modifier = modifier
        )
        FallingMode.DOWN, FallingMode.UP -> FallingNotesView(
            song = song, stepIndex = state.stepIndex,
            playMode = state.playMode, fallingMode = state.fallingMode,
            isLandscape = state.isLandscape, modifier = modifier
        )
    }
}
