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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF080A10), Color(0xFF0E1018))))
    ) {
        // ── Row 1: 곡선택 | 모드버튼 토글 | 설정 ──────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 곡 선택 버튼
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

            // ── 모드 토글 버튼 3개 ──────────────────────────────
            val canPlay = selectedSong != null

            // 자유
            ModeToggleBtn(
                label = "자유",
                icon = "🎸",
                isActive = playMode == PlayMode.FREE,
                isPlaying = false,
                enabled = true,
                activeColor = PianoColors.Amber
            ) { onModeButtonClick(PlayMode.FREE) }

            // 재생
            ModeToggleBtn(
                label = if (playMode == PlayMode.AUTO && isPlaying) "정지" else "재생",
                icon = if (playMode == PlayMode.AUTO && isPlaying) "■" else "▶",
                isActive = playMode == PlayMode.AUTO,
                isPlaying = playMode == PlayMode.AUTO && isPlaying,
                enabled = canPlay,
                activeColor = PianoColors.Blue
            ) { onModeButtonClick(PlayMode.AUTO) }

            // 따라하기
            ModeToggleBtn(
                label = if (playMode == PlayMode.INTERACTIVE && isPlaying) "정지" else "따라하기",
                icon = if (playMode == PlayMode.INTERACTIVE && isPlaying) "■" else "✋",
                isActive = playMode == PlayMode.INTERACTIVE,
                isPlaying = playMode == PlayMode.INTERACTIVE && isPlaying,
                enabled = canPlay,
                activeColor = PianoColors.Emerald
            ) { onModeButtonClick(PlayMode.INTERACTIVE) }

            // 설정
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showSettings) PianoColors.Amber.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onToggleSettings() }
                    .padding(5.dp)
            ) {
                Icon(
                    Icons.Default.Settings, "설정",
                    tint = if (showSettings) PianoColors.Amber else PianoColors.TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // ── Row 2: 뷰모드 | 음색 ────────────────────────────────
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

        // ── 진행 바 ───────────────────────────────────────────
        if (isPlaying && totalSteps > 0 && playMode != PlayMode.FREE) {
            val progress = if (totalSteps > 1) stepIndex.toFloat() / (totalSteps - 1) else 0f
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = when (playMode) {
                        PlayMode.AUTO -> PianoColors.Blue
                        PlayMode.INTERACTIVE -> PianoColors.Emerald
                        else -> PianoColors.Amber
                    },
                    trackColor = Color(0xFF1A1D2A)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$stepIndex / $totalSteps", fontSize = 9.sp, color = PianoColors.TextMuted)
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize = 9.sp,
                        color = when (playMode) {
                            PlayMode.AUTO -> PianoColors.Blue
                            PlayMode.INTERACTIVE -> PianoColors.Emerald
                            else -> PianoColors.TextMuted
                        }
                    )
                }
            }
        } else {
            Divider(color = Color(0xFF181B28), thickness = 1.dp)
        }

        // ── 설정 패널 ─────────────────────────────────────────
        if (showSettings) {
            SettingsPanel(volume, tempoMultiplier, showNoteNames, showNextHint,
                onVolumeChange, onTempoChange, onToggleNoteNames, onToggleNextHint)
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
        !enabled  -> PianoColors.TextMuted.copy(alpha = 0.4f)
        isActive  -> Color.White
        else      -> PianoColors.TextSecondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(icon, fontSize = 11.sp, color = textColor)
            Text(label, fontSize = 10.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = textColor)
        }
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
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 9.sp, color = if (selected) activeColor else PianoColors.TextSecondary)
    }
}

@Composable
private fun SettingsPanel(
    volume: Float, tempo: Float, showNoteNames: Boolean, showNextHint: Boolean,
    onVolumeChange: (Float) -> Unit, onTempoChange: (Float) -> Unit,
    onToggleNoteNames: () -> Unit, onToggleNextHint: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF0C0E18))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
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
