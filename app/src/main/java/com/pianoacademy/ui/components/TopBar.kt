package com.pianoacademy.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.pianoacademy.BuildConfig
import com.pianoacademy.audio.SoundMode
import com.pianoacademy.data.LEVEL_CONFIG
import com.pianoacademy.data.Song
import com.pianoacademy.ui.theme.PianoColors
import com.pianoacademy.viewmodel.FallingMode
import com.pianoacademy.viewmodel.PlayMode

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
    showNoteNames: Boolean,
    showNextHint: Boolean,
    stepIndex: Int = 0,
    totalSteps: Int = 0,
    isLandscape: Boolean = false,
    onSongPickerOpen: () -> Unit,
    onModeButtonClick: (PlayMode) -> Unit,
    onFallingModeChange: (FallingMode) -> Unit,
    onSoundModeChange: (SoundMode) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTempoChange: (Float) -> Unit,
    onToggleSettings: () -> Unit,
    onToggleNoteNames: () -> Unit,
    onToggleNextHint: () -> Unit,
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
            // ── 가로모드: 한 줄 (곡선택 + 악보/폭포 + 모드 + 음색 + 설정) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // ① 곡 선택 (좁게, 최대 130dp)
                Row(
                    modifier = Modifier
                        .widthIn(max = 130.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF181B28))
                        .border(1.dp, Color(0xFF282B3E), RoundedCornerShape(6.dp))
                        .clickable { onSongPickerOpen() }
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(levelCfg?.icon ?: "🎵", fontSize = 10.sp)
                    Text(
                        selectedSong?.title ?: "곡 선택",
                        fontSize = 9.sp,
                        fontWeight = if (selectedSong != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedSong != null) PianoColors.TextPrimary else PianoColors.TextMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }

                // ② 뷰 모드 칩 (악보/폭↓/폭↑) — 곡 선택 바로 옆
                listOf(FallingMode.OFF to "악보", FallingMode.DOWN to "폭↓", FallingMode.UP to "폭↑")
                    .forEach { (mode, label) ->
                        SmallChip(label, mode == fallingMode, PianoColors.Blue) { onFallingModeChange(mode) }
                    }

                // 구분선
                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFF282B3E)))

                // ③ 모드 버튼 (아이콘만, label 없음 — 공간 절약)
                LandscapeModeBtn("🎸", playMode == PlayMode.FREE, false, true, PianoColors.Amber) { onModeButtonClick(PlayMode.FREE) }
                LandscapeModeBtn(
                    icon = if (playMode == PlayMode.AUTO && isPlaying) "■" else "▶",
                    isActive = playMode == PlayMode.AUTO,
                    isPlaying = playMode == PlayMode.AUTO && isPlaying,
                    enabled = canPlay, activeColor = PianoColors.Blue
                ) { onModeButtonClick(PlayMode.AUTO) }
                LandscapeModeBtn(
                    icon = if (playMode == PlayMode.INTERACTIVE && isPlaying) "■" else "✋",
                    isActive = playMode == PlayMode.INTERACTIVE,
                    isPlaying = playMode == PlayMode.INTERACTIVE && isPlaying,
                    enabled = canPlay, activeColor = PianoColors.Emerald
                ) { onModeButtonClick(PlayMode.INTERACTIVE) }
                LandscapeModeBtn(
                    icon = if (playMode == PlayMode.PRACTICE && isPlaying) "■" else "🎓",
                    isActive = playMode == PlayMode.PRACTICE,
                    isPlaying = playMode == PlayMode.PRACTICE && isPlaying,
                    enabled = canPlay, activeColor = Color(0xFF8B5CF6)
                ) { onModeButtonClick(PlayMode.PRACTICE) }

                // 구분선
                Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFF282B3E)))

                // ④ 음색 아이콘
                SoundMode.values().forEach { sm ->
                    val sel = sm == soundMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (sel) Brush.verticalGradient(listOf(PianoColors.Violet, Color(0xFF6D28D9)))
                                else Brush.verticalGradient(listOf(Color(0xFF181B28), Color(0xFF181B28))),
                                RoundedCornerShape(4.dp)
                            )
                            .border(0.5.dp, if (sel) PianoColors.Violet else Color(0xFF282B3E), RoundedCornerShape(4.dp))
                            .clickable { onSoundModeChange(sm) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) { Text(sm.icon, fontSize = 11.sp) }
                }

                // ⑤ Spacer — 남은 공간 채워서 설정 아이콘을 오른쪽 끝으로
                Spacer(modifier = Modifier.weight(1f))

                // 버전
                Text("v${BuildConfig.VERSION_NAME}", fontSize = 7.sp, color = Color(0xFF3A3E55))

                // ⑥ 설정 아이콘 (항상 오른쪽에 노출)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (showSettings) PianoColors.Amber.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onToggleSettings() }
                        .padding(5.dp)
                ) {
                    Icon(Icons.Default.Settings, "설정",
                        tint = if (showSettings) PianoColors.Amber else PianoColors.TextSecondary,
                        modifier = Modifier.size(17.dp))
                }
            }

            // 진행 바 (가로)
            if (isPlaying && totalSteps > 0 && playMode != PlayMode.FREE) {
                val progress = if (totalSteps > 1) stepIndex.toFloat() / (totalSteps - 1) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = when (playMode) {
                        PlayMode.AUTO -> PianoColors.Blue
                        PlayMode.INTERACTIVE -> PianoColors.Emerald
                        PlayMode.PRACTICE -> Color(0xFF8B5CF6)
                        else -> PianoColors.Amber
                    },
                    trackColor = Color(0xFF1A1D2A)
                )
            }

        } else {
            // ── 세로모드: 2줄 ─────────────────────────────────────────
            // Row 1: 곡선택 | 모드버튼 | 설정
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // 곡 선택
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

                // 모드 버튼 4개
                ModeToggleBtn("자유", "🎸", playMode == PlayMode.FREE, false, true, PianoColors.Amber) { onModeButtonClick(PlayMode.FREE) }
                ModeToggleBtn(
                    label = if (playMode == PlayMode.AUTO && isPlaying) "정지" else "재생",
                    icon = if (playMode == PlayMode.AUTO && isPlaying) "■" else "▶",
                    isActive = playMode == PlayMode.AUTO, isPlaying = playMode == PlayMode.AUTO && isPlaying,
                    enabled = canPlay, activeColor = PianoColors.Blue
                ) { onModeButtonClick(PlayMode.AUTO) }
                ModeToggleBtn(
                    label = if (playMode == PlayMode.INTERACTIVE && isPlaying) "정지" else "따라하기",
                    icon = if (playMode == PlayMode.INTERACTIVE && isPlaying) "■" else "✋",
                    isActive = playMode == PlayMode.INTERACTIVE, isPlaying = playMode == PlayMode.INTERACTIVE && isPlaying,
                    enabled = canPlay, activeColor = PianoColors.Emerald
                ) { onModeButtonClick(PlayMode.INTERACTIVE) }
                ModeToggleBtn(
                    label = if (playMode == PlayMode.PRACTICE && isPlaying) "정지" else "혼자하기",
                    icon = if (playMode == PlayMode.PRACTICE && isPlaying) "■" else "🎓",
                    isActive = playMode == PlayMode.PRACTICE, isPlaying = playMode == PlayMode.PRACTICE && isPlaying,
                    enabled = canPlay, activeColor = Color(0xFF8B5CF6)
                ) { onModeButtonClick(PlayMode.PRACTICE) }

                // 버전
                Text("v${BuildConfig.VERSION_NAME}", fontSize = 8.sp, color = Color(0xFF3A3E55))

                // 설정
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

            // Row 2: 뷰모드 | 음색
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

            // 진행 바 (세로)
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
                Divider(color = Color(0xFF181B28), thickness = 1.dp)
            }

            // 설정 패널
            if (showSettings) {
                SettingsPanel(
                    volume, tempoMultiplier, showNoteNames, showNextHint, soundMode,
                    onVolumeChange, onTempoChange, onToggleNoteNames, onToggleNextHint, onSoundModeChange
                )
            }
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

// 가로모드 전용 — 아이콘만 있는 초소형 모드 버튼
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
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            icon, fontSize = 12.sp,
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

@Composable
private fun SettingsPanel(
    volume: Float, tempo: Float, showNoteNames: Boolean, showNextHint: Boolean,
    soundMode: SoundMode,
    onVolumeChange: (Float) -> Unit, onTempoChange: (Float) -> Unit,
    onToggleNoteNames: () -> Unit, onToggleNextHint: () -> Unit,
    onSoundModeChange: (SoundMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF0C0E18))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ─ 음색 번호 선택 ─
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

        // ─ 볼륨 ─
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔊 볼륨", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(52.dp))
            Slider(value = volume, onValueChange = onVolumeChange, valueRange = 0f..1f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = PianoColors.Amber, activeTrackColor = PianoColors.Amber, inactiveTrackColor = Color(0xFF2A2D3E)))
            Text("${(volume * 100).toInt()}%", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(30.dp))
        }
        // ─ 템포 ─
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🎵 템포", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(52.dp))
            Slider(value = tempo, onValueChange = onTempoChange, valueRange = 0.5f..2.0f, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = PianoColors.Blue, activeTrackColor = PianoColors.Blue, inactiveTrackColor = Color(0xFF2A2D3E)))
            Text("×${String.format("%.1f", tempo)}", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(30.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ToggleRow("음이름", showNoteNames, onToggleNoteNames)
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
