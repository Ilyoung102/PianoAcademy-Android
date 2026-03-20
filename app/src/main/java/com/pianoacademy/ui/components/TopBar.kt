package com.pianoacademy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.pianoacademy.BuildConfig
import com.pianoacademy.audio.SoundMode
import com.pianoacademy.data.LEVEL_CONFIG
import com.pianoacademy.data.Song
import com.pianoacademy.ui.theme.PianoColors
import com.pianoacademy.viewmodel.FallingMode
import com.pianoacademy.data.NoteNameMode
import com.pianoacademy.viewmodel.PlayMode
import com.pianoacademy.viewmodel.KeyboardLayout

@Composable
fun TopBar(
    selectedSong: Song?,
    selectedLevel: Int = 1,
    playMode: PlayMode,
    fallingMode: FallingMode,
    isPlaying: Boolean,
    soundMode: SoundMode,
    volume: Float,
    tempoMultiplier: Float,
    showSettings: Boolean,
    noteNameMode: NoteNameMode,
    showNextHint: Boolean,
    stepIndex: Int = 0,
    totalSteps: Int = 0,
    isLandscape: Boolean = false,
    keyOctaveShift: Int = 0,
    onSongPickerOpen: () -> Unit,
    onModeButtonClick: (PlayMode) -> Unit,
    onFallingModeChange: (FallingMode) -> Unit,
    onSoundModeChange: (SoundMode) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTempoChange: (Float) -> Unit,
    onToggleSettings: () -> Unit,
    onNoteNameModeChange: (NoteNameMode) -> Unit,
    onToggleNextHint: () -> Unit,
    onShiftKeyboard: (Int) -> Unit = {},
    isSustainPedal: Boolean = false,
    onToggleSustainPedal: () -> Unit = {},
    onLoadMdFile: () -> Unit = {},
    keyboardLayout: KeyboardLayout = KeyboardLayout.SINGLE,
    onKeyboardLayoutChange: (KeyboardLayout) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val levelCfg = LEVEL_CONFIG[selectedLevel]
    val canPlay = selectedSong != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF080A10), Color(0xFF0E1018))))
    ) {
        if (isLandscape) {
            var showViewPopup by remember { mutableStateOf(false) }
            var showDashboard by remember { mutableStateOf(false) }

            // ── Row 1: 대시보드 | 뷰▾ | 노래목록 || 미니건반 || 모드 | ⚙ ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽: 대시보드 + 뷰모드팝업 + 노래목록
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ≡ 대시보드
                    Box {
                        LandscapeModeBtn("≡", showDashboard, false, true, PianoColors.TextSecondary) {
                            showDashboard = !showDashboard
                        }
                        DropdownMenu(
                            expanded = showDashboard,
                            onDismissRequest = { showDashboard = false },
                            modifier = Modifier.background(Color(0xFF1A1D2E))
                        ) {
                            DropdownMenuItem(
                                text = { Text("다음 힌트 ${if (showNextHint) "✓" else ""}", fontSize = 12.sp, color = PianoColors.TextPrimary) },
                                onClick = { showDashboard = false; onToggleNextHint() }
                            )
                            DropdownMenuItem(
                                text = { Text("음색 설정", fontSize = 12.sp, color = PianoColors.TextPrimary) },
                                onClick = { showDashboard = false; onToggleSettings() }
                            )
                            HorizontalDivider(color = Color(0xFF2A2D3E))
                            DropdownMenuItem(
                                text = { Text("v${BuildConfig.VERSION_NAME}", fontSize = 10.sp, color = PianoColors.TextMuted) },
                                onClick = { showDashboard = false }
                            )
                        }
                    }

                    // 뷰 모드 팝업 버튼
                    Box {
                        val viewLabel = when (fallingMode) {
                            FallingMode.OFF  -> "악보 ▾"
                            FallingMode.DOWN -> "폭포 ▾"
                            FallingMode.UP   -> "역폭 ▾"
                        }
                        LandscapeModeBtn(viewLabel, true, false, true, PianoColors.Blue) {
                            showViewPopup = !showViewPopup
                        }
                        DropdownMenu(
                            expanded = showViewPopup,
                            onDismissRequest = { showViewPopup = false },
                            modifier = Modifier.background(Color(0xFF1A1D2E))
                        ) {
                            listOf(
                                FallingMode.OFF  to "악보",
                                FallingMode.DOWN to "폭포하락",
                                FallingMode.UP   to "폭포상승"
                            ).forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            fontSize = 12.sp,
                                            color = if (fallingMode == mode) PianoColors.Blue else PianoColors.TextPrimary,
                                            fontWeight = if (fallingMode == mode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = { onFallingModeChange(mode); showViewPopup = false }
                                )
                            }
                        }
                    }

                    // 노래 목록
                    LandscapeModeBtn(
                        icon = selectedSong?.title?.let { "♪ $it" } ?: "노래목록",
                        isActive = selectedSong != null,
                        isPlaying = false,
                        enabled = true,
                        activeColor = PianoColors.Amber
                    ) { onSongPickerOpen() }

                    // MD 파일 로드 아이콘
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF1C1F2E), Color(0xFF181B28))))
                            .border(1.dp, Color(0xFF282B3E), RoundedCornerShape(6.dp))
                            .clickable { onLoadMdFile() }
                            .padding(horizontal = 7.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📄", fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.weight(1f))

                // 중앙: 미니 건반 프리뷰
                MiniKeyboardPreview(
                    octaveShift = keyOctaveShift,
                    onShift = onShiftKeyboard,
                    modifier = Modifier
                        .height(30.dp)
                        .width(260.dp)
                )

                Spacer(Modifier.weight(1f))

                // 오른쪽: 재생 모드 + 설정
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 재생 모드 팝업
                    var showLandModePopup by remember { mutableStateOf(false) }
                    Box {
                        val (lmLabel, lmColor) = when {
                            playMode == PlayMode.FREE -> "🎸자유" to PianoColors.Amber
                            playMode == PlayMode.AUTO && isPlaying -> "■정지" to PianoColors.Blue
                            playMode == PlayMode.AUTO -> "▶재생" to PianoColors.Blue
                            playMode == PlayMode.INTERACTIVE && isPlaying -> "■정지" to PianoColors.Emerald
                            playMode == PlayMode.INTERACTIVE -> "✋따라하기" to PianoColors.Emerald
                            playMode == PlayMode.PRACTICE && isPlaying -> "■정지" to Color(0xFF8B5CF6)
                            else -> "🎓혼자하기" to Color(0xFF8B5CF6)
                        }
                        LandscapeModeBtn(lmLabel, true, playMode != PlayMode.FREE && isPlaying, true, lmColor) {
                            if (isPlaying) onModeButtonClick(playMode)
                            else showLandModePopup = true
                        }
                        DropdownMenu(
                            expanded = showLandModePopup,
                            onDismissRequest = { showLandModePopup = false },
                            modifier = Modifier.background(Color(0xFF1A1D2E))
                        ) {
                            listOf(
                                Triple(PlayMode.FREE, "🎸 자유", PianoColors.Amber),
                                Triple(PlayMode.AUTO, "▶ 재생", PianoColors.Blue),
                                Triple(PlayMode.INTERACTIVE, "✋ 따라하기", PianoColors.Emerald),
                                Triple(PlayMode.PRACTICE, "🎓 혼자하기", Color(0xFF8B5CF6))
                            ).forEach { (mode, label, color) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(label, fontSize = 12.sp,
                                            color = if (playMode == mode) color else PianoColors.TextPrimary,
                                            fontWeight = if (playMode == mode) FontWeight.Bold else FontWeight.Normal)
                                    },
                                    onClick = {
                                        showLandModePopup = false
                                        if (mode != PlayMode.FREE && !canPlay) return@DropdownMenuItem
                                        onModeButtonClick(mode)
                                    }
                                )
                            }
                        }
                    }

                    Box(Modifier.width(1.dp).height(18.dp).background(Color(0xFF303452)))

                    // 음이름 표시 아이콘
                    var showNoteNamePopup by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (noteNameMode != NoteNameMode.NONE) PianoColors.Emerald.copy(0.2f) else Color.Transparent)
                                .clickable { showNoteNamePopup = true }
                                .padding(6.dp)
                        ) {
                            Text("♩", fontSize = 14.sp,
                                color = if (noteNameMode != NoteNameMode.NONE) PianoColors.Emerald else PianoColors.TextSecondary)
                        }
                        DropdownMenu(
                            expanded = showNoteNamePopup,
                            onDismissRequest = { showNoteNamePopup = false },
                            modifier = Modifier.background(Color(0xFF1A1D2E))
                        ) {
                            Text("키에 라벨을 보여줍니다", fontSize = 11.sp, color = PianoColors.TextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            listOf(
                                NoteNameMode.PITCH_OCTAVE to "키에 'C4', 'D4' 표시",
                                NoteNameMode.SOLFEGE     to "키에 'Do', 'Re' 표시",
                                NoteNameMode.NUMBER      to "키에 '1', '2' 표시",
                                NoteNameMode.PITCH       to "키에 'C' 표시",
                                NoteNameMode.NONE        to "비활성"
                            ).forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(if (noteNameMode == mode) "●" else "○", fontSize = 12.sp,
                                                color = if (noteNameMode == mode) PianoColors.Emerald else PianoColors.TextMuted)
                                            Text(label, fontSize = 12.sp,
                                                color = if (noteNameMode == mode) PianoColors.Emerald else PianoColors.TextPrimary)
                                        }
                                    },
                                    onClick = { showNoteNamePopup = false; onNoteNameModeChange(mode) }
                                )
                            }
                        }
                    }

                    // 자유 모드: 피아노 종류 아이콘
                    if (playMode == PlayMode.FREE) {
                        var showLandPianoTypePopup by remember { mutableStateOf(false) }
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1C1F2E))
                                    .border(1.dp, Color(0xFF282B3E), RoundedCornerShape(6.dp))
                                    .clickable { showLandPianoTypePopup = true }
                                    .padding(6.dp)
                            ) {
                                Text(soundMode.icon, fontSize = 14.sp)
                            }
                            DropdownMenu(
                                expanded = showLandPianoTypePopup,
                                onDismissRequest = { showLandPianoTypePopup = false },
                                modifier = Modifier.background(Color(0xFF1A1D2E))
                            ) {
                                Text("피아노 종류", fontSize = 11.sp, color = PianoColors.TextMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                com.pianoacademy.audio.SoundMode.values().forEach { sm ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(if (soundMode == sm) "●" else "○", fontSize = 12.sp,
                                                    color = if (soundMode == sm) PianoColors.Violet else PianoColors.TextMuted)
                                                Text(sm.icon, fontSize = 14.sp)
                                                Text(sm.label, fontSize = 12.sp,
                                                    color = if (soundMode == sm) PianoColors.Violet else PianoColors.TextPrimary,
                                                    fontWeight = if (soundMode == sm) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        },
                                        onClick = { showLandPianoTypePopup = false; onSoundModeChange(sm) }
                                    )
                                }
                            }
                        }
                    }

                    // 건반 레이아웃 선택
                    run {
                        val isFreeMode = playMode == PlayMode.FREE
                        val layoutLabel = when (keyboardLayout) {
                            KeyboardLayout.SINGLE -> "1건"
                            KeyboardLayout.DOUBLE -> "2건"
                            KeyboardLayout.MIRROR -> "마주"
                        }
                        var showLandLayoutPopup by remember { mutableStateOf(false) }
                        Box {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isFreeMode && keyboardLayout != KeyboardLayout.SINGLE) PianoColors.Blue.copy(0.2f) else Color(0xFF1C1F2E))
                                    .border(1.dp, if (isFreeMode) Color(0xFF383B5E) else Color(0xFF1E2030), RoundedCornerShape(6.dp))
                                    .then(if (isFreeMode) Modifier.clickable { showLandLayoutPopup = true } else Modifier)
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(layoutLabel, fontSize = 10.sp,
                                    color = if (isFreeMode) PianoColors.Blue else PianoColors.TextMuted.copy(0.4f),
                                    fontWeight = FontWeight.Bold)
                            }
                            if (isFreeMode) {
                                DropdownMenu(
                                    expanded = showLandLayoutPopup,
                                    onDismissRequest = { showLandLayoutPopup = false },
                                    modifier = Modifier.background(Color(0xFF1A1D2E))
                                ) {
                                    listOf(
                                        KeyboardLayout.SINGLE to "🎹 1개 건반",
                                        KeyboardLayout.DOUBLE to "🎹🎹 2개 건반",
                                        KeyboardLayout.MIRROR to "↕ 마주 보기"
                                    ).forEach { (layout, label) ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(if (keyboardLayout == layout) "●" else "○", fontSize = 12.sp,
                                                        color = if (keyboardLayout == layout) PianoColors.Blue else PianoColors.TextMuted)
                                                    Text(label, fontSize = 12.sp,
                                                        color = if (keyboardLayout == layout) PianoColors.Blue else PianoColors.TextPrimary,
                                                        fontWeight = if (keyboardLayout == layout) FontWeight.Bold else FontWeight.Normal)
                                                }
                                            },
                                            onClick = { showLandLayoutPopup = false; onKeyboardLayoutChange(layout) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(Modifier.width(1.dp).height(18.dp).background(Color(0xFF303452)))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (showSettings) PianoColors.Amber.copy(0.2f) else Color.Transparent)
                            .clickable { onToggleSettings() }
                            .padding(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings, "설정",
                            tint = if (showSettings) PianoColors.Amber else PianoColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ── Row 2: 볼륨(30%↓) | 페달+아이콘 중앙 | 템포(30%↓) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽: 음량 (기존 대비 70%)
                Row(
                    modifier = Modifier.weight(0.35f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔊", fontSize = 12.sp)
                    Spacer(Modifier.width(3.dp))
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f).height(16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = PianoColors.Amber,
                            activeTrackColor = PianoColors.Amber,
                            inactiveTrackColor = Color(0xFF2A2D3E)
                        )
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${(volume * 100).toInt()}%",
                        fontSize = 9.sp, color = PianoColors.TextSecondary,
                        modifier = Modifier.width(24.dp)
                    )
                }

                // 중앙: 페달 + 향후 기능 아이콘 공간
                Row(
                    modifier = Modifier.weight(0.30f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 서스테인 페달 버튼 (3페달 아이콘)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSustainPedal)
                                    Brush.verticalGradient(listOf(PianoColors.Blue.copy(0.5f), PianoColors.Blue.copy(0.3f)))
                                else
                                    Brush.verticalGradient(listOf(Color(0xFF1C1F2E), Color(0xFF181B28)))
                            )
                            .border(1.dp, if (isSustainPedal) PianoColors.Blue else Color(0xFF282B3E), RoundedCornerShape(6.dp))
                            .clickable { onToggleSustainPedal() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ThreePedalIcon(
                            isActive = isSustainPedal,
                            modifier = Modifier.size(width = 28.dp, height = 16.dp)
                        )
                    }
                }

                // 오른쪽: 템포 (기존 대비 70%)
                Row(
                    modifier = Modifier.weight(0.35f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎵", fontSize = 12.sp)
                    Spacer(Modifier.width(3.dp))
                    Slider(
                        value = tempoMultiplier,
                        onValueChange = onTempoChange,
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.weight(1f).height(16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = PianoColors.Blue,
                            activeTrackColor = PianoColors.Blue,
                            inactiveTrackColor = Color(0xFF2A2D3E)
                        )
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "×${String.format("%.1f", tempoMultiplier)}",
                        fontSize = 9.sp, color = PianoColors.TextSecondary,
                        modifier = Modifier.width(28.dp)
                    )
                }
            }

            // 진행 바
            if (isPlaying && totalSteps > 0 && playMode != PlayMode.FREE) {
                val progress = if (totalSteps > 1) stepIndex.toFloat() / (totalSteps - 1) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = when (playMode) {
                        PlayMode.AUTO        -> PianoColors.Blue
                        PlayMode.INTERACTIVE -> PianoColors.Emerald
                        PlayMode.PRACTICE    -> Color(0xFF8B5CF6)
                        else                 -> PianoColors.Amber
                    },
                    trackColor = Color(0xFF1A1D2A)
                )
            }

            // 설정 패널 (음색만)
            if (showSettings) {
                SoundSettingsPanel(soundMode, showNextHint, onSoundModeChange, onToggleNextHint)
            }

        } else {
            // ── 세로모드: 2줄 ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF181B28))
                        .border(1.dp, Color(0xFF282B3E), RoundedCornerShape(8.dp))
                        .clickable { onSongPickerOpen() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(levelCfg?.icon ?: "🎵", fontSize = 14.sp)
                    Text(
                        selectedSong?.title ?: "곡 선택 ▾",
                        fontSize = 11.sp,
                        fontWeight = if (selectedSong != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedSong != null) PianoColors.TextPrimary else PianoColors.TextMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedSong != null) Text("▾", fontSize = 11.sp, color = PianoColors.TextMuted)
                }

                // 재생 모드 팝업
                var showPortModePopup by remember { mutableStateOf(false) }
                Box {
                    val (pmLabel, pmIcon, pmColor) = when {
                        playMode == PlayMode.FREE -> Triple("자유", "🎸", PianoColors.Amber)
                        playMode == PlayMode.AUTO && isPlaying -> Triple("정지", "■", PianoColors.Blue)
                        playMode == PlayMode.AUTO -> Triple("재생", "▶", PianoColors.Blue)
                        playMode == PlayMode.INTERACTIVE && isPlaying -> Triple("정지", "■", PianoColors.Emerald)
                        playMode == PlayMode.INTERACTIVE -> Triple("따라하기", "✋", PianoColors.Emerald)
                        playMode == PlayMode.PRACTICE && isPlaying -> Triple("정지", "■", Color(0xFF8B5CF6))
                        else -> Triple("혼자하기", "🎓", Color(0xFF8B5CF6))
                    }
                    ModeToggleBtn(pmLabel, pmIcon, true, playMode != PlayMode.FREE && isPlaying, true, pmColor) {
                        if (isPlaying) onModeButtonClick(playMode)
                        else showPortModePopup = true
                    }
                    DropdownMenu(
                        expanded = showPortModePopup,
                        onDismissRequest = { showPortModePopup = false },
                        modifier = Modifier.background(Color(0xFF1A1D2E))
                    ) {
                        listOf(
                            Triple(PlayMode.FREE, "🎸 자유", PianoColors.Amber),
                            Triple(PlayMode.AUTO, "▶ 재생", PianoColors.Blue),
                            Triple(PlayMode.INTERACTIVE, "✋ 따라하기", PianoColors.Emerald),
                            Triple(PlayMode.PRACTICE, "🎓 혼자하기", Color(0xFF8B5CF6))
                        ).forEach { (mode, label, color) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(if (playMode == mode) "●" else "○", fontSize = 12.sp,
                                            color = if (playMode == mode) color else PianoColors.TextMuted)
                                        Text(label, fontSize = 12.sp,
                                            color = if (playMode == mode) color else PianoColors.TextPrimary,
                                            fontWeight = if (playMode == mode) FontWeight.Bold else FontWeight.Normal)
                                    }
                                },
                                onClick = {
                                    showPortModePopup = false
                                    if (mode != PlayMode.FREE && !canPlay) return@DropdownMenuItem
                                    onModeButtonClick(mode)
                                }
                            )
                        }
                    }
                }

                // 음이름 표시 아이콘
                var showNoteNamePopupPort by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (noteNameMode != NoteNameMode.NONE) PianoColors.Emerald.copy(0.2f) else Color.Transparent)
                            .clickable { showNoteNamePopupPort = true }
                            .padding(5.dp)
                    ) {
                        Text("♩", fontSize = 14.sp,
                            color = if (noteNameMode != NoteNameMode.NONE) PianoColors.Emerald else PianoColors.TextSecondary)
                    }
                    DropdownMenu(
                        expanded = showNoteNamePopupPort,
                        onDismissRequest = { showNoteNamePopupPort = false },
                        modifier = Modifier.background(Color(0xFF1A1D2E))
                    ) {
                        Text("키에 라벨을 보여줍니다", fontSize = 11.sp, color = PianoColors.TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        listOf(
                            NoteNameMode.PITCH_OCTAVE to "키에 'C4', 'D4' 표시",
                            NoteNameMode.SOLFEGE     to "키에 'Do', 'Re' 표시",
                            NoteNameMode.NUMBER      to "키에 '1', '2' 표시",
                            NoteNameMode.PITCH       to "키에 'C' 표시",
                            NoteNameMode.NONE        to "비활성"
                        ).forEach { (mode, label) ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(if (noteNameMode == mode) "●" else "○", fontSize = 12.sp,
                                            color = if (noteNameMode == mode) PianoColors.Emerald else PianoColors.TextMuted)
                                        Text(label, fontSize = 12.sp,
                                            color = if (noteNameMode == mode) PianoColors.Emerald else PianoColors.TextPrimary)
                                    }
                                },
                                onClick = { showNoteNamePopupPort = false; onNoteNameModeChange(mode) }
                            )
                        }
                    }
                }

                // 자유 모드: 피아노 종류 아이콘
                if (playMode == PlayMode.FREE) {
                    var showPianoTypePopup by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1C1F2E))
                                .border(1.dp, Color(0xFF282B3E), RoundedCornerShape(6.dp))
                                .clickable { showPianoTypePopup = true }
                                .padding(5.dp)
                        ) {
                            Text(soundMode.icon, fontSize = 14.sp)
                        }
                        DropdownMenu(
                            expanded = showPianoTypePopup,
                            onDismissRequest = { showPianoTypePopup = false },
                            modifier = Modifier.background(Color(0xFF1A1D2E))
                        ) {
                            Text("피아노 종류", fontSize = 11.sp, color = PianoColors.TextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            com.pianoacademy.audio.SoundMode.values().forEach { sm ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(if (soundMode == sm) "●" else "○", fontSize = 12.sp,
                                                color = if (soundMode == sm) PianoColors.Violet else PianoColors.TextMuted)
                                            Text(sm.icon, fontSize = 14.sp)
                                            Text(sm.label, fontSize = 12.sp,
                                                color = if (soundMode == sm) PianoColors.Violet else PianoColors.TextPrimary,
                                                fontWeight = if (soundMode == sm) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    },
                                    onClick = { showPianoTypePopup = false; onSoundModeChange(sm) }
                                )
                            }
                        }
                    }
                }

                // 건반 레이아웃 선택 (자유모드 활성, 다른 모드 비활성)
                run {
                    val isFreeMode = playMode == PlayMode.FREE
                    val layoutLabel = when (keyboardLayout) {
                        KeyboardLayout.SINGLE -> "1건"
                        KeyboardLayout.DOUBLE -> "2건"
                        KeyboardLayout.MIRROR -> "마주"
                    }
                    var showLayoutPopup by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isFreeMode && keyboardLayout != KeyboardLayout.SINGLE) PianoColors.Blue.copy(0.2f) else Color(0xFF1C1F2E))
                                .border(1.dp, if (isFreeMode) Color(0xFF383B5E) else Color(0xFF1E2030), RoundedCornerShape(6.dp))
                                .then(if (isFreeMode) Modifier.clickable { showLayoutPopup = true } else Modifier)
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Text(layoutLabel, fontSize = 9.sp,
                                color = if (isFreeMode) PianoColors.Blue else PianoColors.TextMuted.copy(0.4f),
                                fontWeight = FontWeight.Bold)
                        }
                        if (isFreeMode) {
                            DropdownMenu(
                                expanded = showLayoutPopup,
                                onDismissRequest = { showLayoutPopup = false },
                                modifier = Modifier.background(Color(0xFF1A1D2E))
                            ) {
                                listOf(
                                    KeyboardLayout.SINGLE to "🎹 1개 건반",
                                    KeyboardLayout.DOUBLE to "🎹🎹 2개 건반",
                                    KeyboardLayout.MIRROR to "↕ 마주 보기"
                                ).forEach { (layout, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(if (keyboardLayout == layout) "●" else "○", fontSize = 12.sp,
                                                    color = if (keyboardLayout == layout) PianoColors.Blue else PianoColors.TextMuted)
                                                Text(label, fontSize = 12.sp,
                                                    color = if (keyboardLayout == layout) PianoColors.Blue else PianoColors.TextPrimary,
                                                    fontWeight = if (keyboardLayout == layout) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        },
                                        onClick = { showLayoutPopup = false; onKeyboardLayoutChange(layout) }
                                    )
                                }
                            }
                        }
                    }
                }

                Text("v${BuildConfig.VERSION_NAME}", fontSize = 8.sp, color = Color(0xFF3A3E55))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (showSettings) PianoColors.Amber.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onToggleSettings() }
                        .padding(5.dp)
                ) {
                    Icon(Icons.Default.Settings, "설정",
                        tint = if (showSettings) PianoColors.Amber else PianoColors.TextSecondary,
                        modifier = Modifier.size(19.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf(FallingMode.OFF to "📄 악보", FallingMode.DOWN to "⬇ 폭포", FallingMode.UP to "⬆ 역폭")
                    .forEach { (mode, label) ->
                        SmallChip(label, mode == fallingMode, PianoColors.Blue) { onFallingModeChange(mode) }
                    }
                Spacer(Modifier.weight(1f))
                SoundMode.values().forEach { sm ->
                    val sel = sm == soundMode
                    val bgMod = if (sel)
                        Modifier.background(Brush.verticalGradient(listOf(PianoColors.Violet, Color(0xFF6D28D9))), RoundedCornerShape(5.dp))
                    else
                        Modifier.background(Color(0xFF181B28), RoundedCornerShape(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .then(bgMod)
                            .border(0.5.dp, if (sel) PianoColors.Violet else Color(0xFF282B3E), RoundedCornerShape(5.dp))
                            .clickable { onSoundModeChange(sm) }
                            .padding(horizontal = 5.dp, vertical = 3.dp)
                    ) { Text(sm.icon, fontSize = 12.sp) }
                }
            }

            if (isPlaying && totalSteps > 0 && playMode != PlayMode.FREE) {
                val progress = if (totalSteps > 1) stepIndex.toFloat() / (totalSteps - 1) else 0f
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = when (playMode) {
                            PlayMode.AUTO -> PianoColors.Blue
                            PlayMode.INTERACTIVE -> PianoColors.Emerald
                            PlayMode.PRACTICE -> Color(0xFF8B5CF6)
                            else -> PianoColors.Amber
                        },
                        trackColor = Color(0xFF1A1D2A)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$stepIndex / $totalSteps", fontSize = 9.sp, color = PianoColors.TextMuted)
                        Text("${(progress * 100).toInt()}%", fontSize = 9.sp,
                            color = when (playMode) {
                                PlayMode.AUTO -> PianoColors.Blue
                                PlayMode.INTERACTIVE -> PianoColors.Emerald
                                PlayMode.PRACTICE -> Color(0xFF8B5CF6)
                                else -> PianoColors.TextMuted
                            })
                    }
                }
            } else {
                HorizontalDivider(color = Color(0xFF181B28), thickness = 1.dp)
            }

            if (showSettings) {
                SettingsPanel(
                    volume, tempoMultiplier, showNextHint, soundMode,
                    onVolumeChange, onTempoChange, onToggleNextHint, onSoundModeChange
                )
            }
        }
    }
}

// 피아노 3페달 아이콘 (왼쪽=우나코르다, 가운데=소스테누토, 오른쪽=서스테인/강조)
@Composable
private fun ThreePedalIcon(isActive: Boolean, modifier: Modifier = Modifier) {
    val activeColor = Color(0xFF3B82F6)   // 파랑 (서스테인 페달 활성색)
    val inactiveColor = Color(0xFF6B7094)
    val pedalColor = if (isActive) activeColor else inactiveColor

    Canvas(modifier = modifier) {
        val totalW = size.width
        val totalH = size.height
        val pedalW = totalW * 0.22f       // 각 페달 너비
        val gap = totalW * 0.08f
        val baseY = totalH * 0.15f

        // 페달 높이: 왼쪽(짧음), 가운데(중간), 오른쪽(가장 김 = 서스테인)
        val heights = floatArrayOf(totalH * 0.65f, totalH * 0.72f, totalH * 0.85f)
        val startXs = floatArrayOf(
            0f,
            pedalW + gap,
            (pedalW + gap) * 2f
        )

        for (i in 0..2) {
            val x = startXs[i]
            val h = heights[i]
            val color = if (i == 2 && isActive) activeColor else {
                if (isActive) activeColor.copy(alpha = 0.55f) else inactiveColor
            }

            // 페달 몸통 (둥근 하단)
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(x, baseY)
                lineTo(x + pedalW, baseY)
                lineTo(x + pedalW, baseY + h - pedalW * 0.4f)
                quadraticTo(x + pedalW, baseY + h, x + pedalW * 0.5f, baseY + h)
                quadraticTo(x, baseY + h, x, baseY + h - pedalW * 0.4f)
                close()
            }
            drawPath(path, color)

            // 페달 상단 연결 줄기 (위로 가는 스템)
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(x + pedalW / 2f, 0f),
                end = androidx.compose.ui.geometry.Offset(x + pedalW / 2f, baseY),
                strokeWidth = pedalW * 0.3f
            )
        }
    }
}

// 미니 건반 프리뷰 (전체 피아노 범위 C1-B7 표시, 현재 visible 범위 하이라이트)
@Composable
private fun MiniKeyboardPreview(
    octaveShift: Int,
    onShift: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnShift by rememberUpdatedState(onShift)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // 왼쪽 화살표
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (octaveShift > -2) Color(0xFF1C1F2E) else Color(0xFF0F1018))
                .then(if (octaveShift > -2) Modifier.clickable { latestOnShift(-1) } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "◀", fontSize = 9.sp,
                color = if (octaveShift > -2) PianoColors.TextSecondary else Color(0xFF252838)
            )
        }

        Canvas(
            modifier = modifier.pointerInput(Unit) {
                var accX = 0f
                val octaveW = size.width / 7f  // C1-B7 = 7 octaves
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent()
                        ev.changes.forEach { change ->
                            if (change.pressed && change.positionChanged()) {
                                val dx = change.position.x - change.previousPosition.x
                                accX += dx
                                while (accX > octaveW) {
                                    latestOnShift(1)
                                    accX -= octaveW
                                }
                                while (accX < -octaveW) {
                                    latestOnShift(-1)
                                    accX += octaveW
                                }
                                change.consume()
                            }
                            if (!change.pressed) {
                                accX = 0f
                            }
                        }
                    }
                }
            }
        ) {
            val totalWhites = 49  // 7 octaves C1-B7
            val ww = size.width / totalWhites
            val wh = size.height
            val bh = wh * 0.58f
            val bw = ww * 0.52f

            // 흰 건반 배경
            drawRect(Color(0xFFBEC2CC))

            // 흰 건반 구분선
            for (i in 0..totalWhites) {
                drawLine(Color(0xFF888C98), Offset(i * ww, 0f), Offset(i * ww, wh), 0.4f)
            }

            // 검은 건반 (각 옥타브에서 흰건반 0,1,3,4,5 다음 위치)
            val blackAfter = listOf(0, 1, 3, 4, 5)
            for (oct in 0 until 7) {
                for (w in blackAfter) {
                    val bx = (oct * 7 + w + 1) * ww - bw / 2f
                    drawRect(Color(0xFF1A1C28), Offset(bx, 0f), Size(bw, bh))
                }
            }

            // 현재 visible 범위 하이라이트
            val visStart = (2 + octaveShift) * 7
            val visWidth = 21
            drawRect(
                color = Color(0xFF3B82F6).copy(alpha = 0.38f),
                topLeft = Offset(visStart * ww, 0f),
                size = Size(visWidth * ww, wh)
            )
            drawRect(
                color = Color(0xFF3B82F6).copy(alpha = 0.9f),
                topLeft = Offset(visStart * ww + 0.5f, 0.5f),
                size = Size(visWidth * ww - 1f, wh - 1f),
                style = Stroke(width = 1.5f)
            )
        }

        // 오른쪽 화살표
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (octaveShift < 2) Color(0xFF1C1F2E) else Color(0xFF0F1018))
                .then(if (octaveShift < 2) Modifier.clickable { latestOnShift(1) } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "▶", fontSize = 9.sp,
                color = if (octaveShift < 2) PianoColors.TextSecondary else Color(0xFF252838)
            )
        }
    }
}

@Composable
private fun ModeToggleBtn(
    label: String,
    icon: String,
    isActive: Boolean,
    isPlaying: Boolean,
    enabled: Boolean,
    activeColor: Color,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val bgBrush = when {
        isPlaying -> Brush.verticalGradient(listOf(activeColor, activeColor.copy(alpha = 0.75f)))
        isActive  -> Brush.verticalGradient(listOf(activeColor.copy(alpha = 0.5f), activeColor.copy(alpha = 0.3f)))
        else      -> Brush.verticalGradient(listOf(Color(0xFF1C1F2E), Color(0xFF181B28)))
    }
    val borderColor = when {
        isPlaying -> activeColor
        isActive  -> activeColor.copy(alpha = 0.5f)
        else      -> Color(0xFF282B3E)
    }
    val textColor = when {
        !enabled -> PianoColors.TextMuted.copy(alpha = 0.4f)
        isActive -> Color.White
        else     -> PianoColors.TextSecondary
    }
    val hPad = if (compact) 5.dp else 8.dp
    val vPad = if (compact) 3.dp else 6.dp
    val iconSp = if (compact) 10.sp else 11.sp
    val labelSp = if (compact) 9.sp else 10.sp

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(7.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = hPad, vertical = vPad),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (icon.isNotEmpty()) Text(icon, fontSize = iconSp, color = textColor)
            Text(label, fontSize = labelSp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = textColor)
        }
    }
}

@Composable
private fun LandscapeModeBtn(
    icon: String,
    isActive: Boolean,
    isPlaying: Boolean,
    enabled: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bgBrush = when {
        isPlaying -> Brush.verticalGradient(listOf(activeColor, activeColor.copy(alpha = 0.7f)))
        isActive  -> Brush.verticalGradient(listOf(activeColor.copy(0.45f), activeColor.copy(0.25f)))
        else      -> Brush.verticalGradient(listOf(Color(0xFF1C1F2E), Color(0xFF181B28)))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgBrush)
            .border(1.dp, if (isActive) activeColor.copy(0.6f) else Color(0xFF282B3E), RoundedCornerShape(6.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            icon, fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (!enabled) PianoColors.TextMuted.copy(0.4f)
                    else if (isActive) Color.White else PianoColors.TextSecondary
        )
    }
}

@Composable
private fun SmallChip(label: String, selected: Boolean, activeColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (selected) activeColor.copy(alpha = 0.25f) else Color(0xFF181B28))
            .border(0.5.dp, if (selected) activeColor else Color(0xFF282B3E), RoundedCornerShape(5.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 9.sp, color = if (selected) activeColor else PianoColors.TextSecondary)
    }
}

// 간소화된 음색 설정 패널 (가로모드 - 볼륨/템포는 Row2에 있으므로 음색만)
@Composable
private fun SoundSettingsPanel(
    soundMode: SoundMode,
    showNextHint: Boolean,
    onSoundModeChange: (SoundMode) -> Unit,
    onToggleNextHint: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0E18))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🎹 음색", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(48.dp))
        SoundMode.values().forEachIndexed { i, sm ->
            val sel = sm == soundMode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (sel) Brush.verticalGradient(listOf(PianoColors.Violet, Color(0xFF6D28D9)))
                        else Brush.verticalGradient(listOf(Color(0xFF1C1F2E), Color(0xFF181B28)))
                    )
                    .border(1.dp, if (sel) PianoColors.Violet else Color(0xFF282B3E), RoundedCornerShape(6.dp))
                    .clickable { onSoundModeChange(sm) }
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${i + 1}번", fontSize = 8.sp, color = if (sel) Color.White.copy(0.7f) else PianoColors.TextMuted)
                    Text(sm.icon, fontSize = 13.sp)
                    Text(sm.label, fontSize = 8.sp, color = if (sel) Color.White else PianoColors.TextSecondary,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        ToggleRow("힌트", showNextHint, onToggleNextHint)
    }
}

@Composable
private fun SettingsPanel(
    volume: Float, tempo: Float, showNextHint: Boolean,
    soundMode: SoundMode,
    onVolumeChange: (Float) -> Unit, onTempoChange: (Float) -> Unit,
    onToggleNextHint: () -> Unit,
    onSoundModeChange: (SoundMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF0C0E18))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("🎹 음색", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(52.dp))
            SoundMode.values().forEachIndexed { i, sm ->
                val sel = sm == soundMode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (sel) Brush.verticalGradient(listOf(PianoColors.Violet, Color(0xFF6D28D9)))
                            else Brush.verticalGradient(listOf(Color(0xFF1C1F2E), Color(0xFF181B28)))
                        )
                        .border(1.dp, if (sel) PianoColors.Violet else Color(0xFF282B3E), RoundedCornerShape(6.dp))
                        .clickable { onSoundModeChange(sm) }
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${i + 1}번", fontSize = 8.sp,
                            color = if (sel) Color.White.copy(alpha = 0.7f) else PianoColors.TextMuted)
                        Text(sm.icon, fontSize = 13.sp)
                        Text(sm.label, fontSize = 8.sp,
                            color = if (sel) Color.White else PianoColors.TextSecondary,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔊 볼륨", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(52.dp))
            Slider(value = volume, onValueChange = onVolumeChange, valueRange = 0f..1f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = PianoColors.Amber, activeTrackColor = PianoColors.Amber, inactiveTrackColor = Color(0xFF2A2D3E)))
            Text("${(volume * 100).toInt()}%", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(30.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🎵 템포", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(52.dp))
            Slider(value = tempo, onValueChange = onTempoChange, valueRange = 0.5f..2.0f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = PianoColors.Blue, activeTrackColor = PianoColors.Blue, inactiveTrackColor = Color(0xFF2A2D3E)))
            Text("×${String.format("%.1f", tempo)}", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(30.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ToggleRow("다음 힌트", showNextHint, onToggleNextHint)
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.clickable { onToggle() }) {
        Text(label, fontSize = 10.sp, color = PianoColors.TextSecondary)
        Switch(checked = value, onCheckedChange = { onToggle() },
            modifier = Modifier.height(20.dp).width(36.dp),
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PianoColors.Amber,
                uncheckedThumbColor = PianoColors.TextMuted, uncheckedTrackColor = Color(0xFF2A2D3E)))
    }
}
