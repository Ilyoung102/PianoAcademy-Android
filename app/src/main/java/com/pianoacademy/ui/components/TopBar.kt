package com.pianoacademy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

// ── 상단 컨트롤 바 ─────────────────────────────────────────────
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
    onPlayModeChange: (PlayMode) -> Unit,
    onFallingModeChange: (FallingMode) -> Unit,
    onPlayStop: () -> Unit,
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
            .background(
                Brush.verticalGradient(colors = listOf(Color(0xFF0C0E14), Color(0xFF111520)))
            )
    ) {
        // ── 1행: 곡선택 버튼 | 모드 | 재생 | 설정 ──────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // 곡 선택 버튼 (레벨 아이콘 + 곡 제목 + 화살표)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1D2A))
                    .border(1.dp, Color(0xFF2A2D3E), RoundedCornerShape(8.dp))
                    .clickable { onSongPickerOpen() }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(levelCfg?.icon ?: "🎵", fontSize = 14.sp)
                Text(
                    selectedSong?.title ?: "곡 선택 ▾",
                    fontSize = 11.sp,
                    fontWeight = if (selectedSong != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selectedSong != null) PianoColors.TextPrimary else PianoColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (selectedSong != null) {
                    Text("▾", fontSize = 12.sp, color = PianoColors.TextMuted)
                }
            }

            // 모드 버튼 그룹
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1C2033))
                    .border(1.dp, Color(0xFF2A2D3E), RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                ModeButton("자유", "🎸", PlayMode.FREE, playMode, PianoColors.Amber) {
                    onPlayModeChange(PlayMode.FREE)
                }
                ModeButton("재생", "▶", PlayMode.AUTO, playMode, PianoColors.Blue) {
                    onPlayModeChange(PlayMode.AUTO)
                }
                ModeButton("따라하기", "✋", PlayMode.INTERACTIVE, playMode, PianoColors.Emerald) {
                    onPlayModeChange(PlayMode.INTERACTIVE)
                }
            }

            // 재생/정지 버튼
            if (selectedSong != null && playMode != PlayMode.FREE) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isPlaying)
                                Brush.horizontalGradient(listOf(PianoColors.Rose, Color(0xFFBE123C)))
                            else
                                Brush.horizontalGradient(listOf(PianoColors.Emerald, Color(0xFF059669)))
                        )
                        .clickable { onPlayStop() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (isPlaying) "■" else "▶",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // 설정 버튼
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showSettings) PianoColors.Amber.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onToggleSettings() }
                    .padding(5.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "설정",
                    tint = if (showSettings) PianoColors.Amber else PianoColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── 2행: 뷰 모드 | 음색 선택 ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf(
                FallingMode.OFF  to "📄 악보",
                FallingMode.DOWN to "⬇ 폭포",
                FallingMode.UP   to "⬆ 역폭"
            ).forEach { (mode, label) ->
                ViewModeChip(label, mode, fallingMode) { onFallingModeChange(mode) }
            }

            Spacer(Modifier.weight(1f))

            SoundMode.values().forEach { sm ->
                val isSelected = sm == soundMode
                val soundBgMod = if (isSelected)
                    Modifier.background(
                        Brush.verticalGradient(listOf(PianoColors.Violet, Color(0xFF6D28D9))),
                        RoundedCornerShape(5.dp)
                    )
                else
                    Modifier.background(Color(0xFF1A1C2A), RoundedCornerShape(5.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .then(soundBgMod)
                        .border(
                            0.5.dp,
                            if (isSelected) PianoColors.Violet else Color(0xFF2A2D3E),
                            RoundedCornerShape(5.dp)
                        )
                        .clickable { onSoundModeChange(sm) }
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(sm.icon, fontSize = 11.sp)
                }
            }
        }

        // ── 진행 바 ───────────────────────────────────────────
        if (isPlaying && totalSteps > 0 && playMode != PlayMode.FREE) {
            val progress = if (totalSteps > 1) stepIndex.toFloat() / (totalSteps - 1) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = when (playMode) {
                    PlayMode.AUTO        -> PianoColors.Blue
                    PlayMode.INTERACTIVE -> PianoColors.Emerald
                    else                 -> PianoColors.Amber
                },
                trackColor = Color(0xFF1C2033)
            )
        } else {
            Divider(color = Color(0xFF1A1E2A), thickness = 1.dp)
        }

        // ── 설정 패널 ─────────────────────────────────────────
        if (showSettings) {
            SettingsPanel(
                volume = volume,
                tempo = tempoMultiplier,
                showNoteNames = showNoteNames,
                showNextHint = showNextHint,
                onVolumeChange = onVolumeChange,
                onTempoChange = onTempoChange,
                onToggleNoteNames = onToggleNoteNames,
                onToggleNextHint = onToggleNextHint
            )
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    icon: String,
    mode: PlayMode,
    current: PlayMode,
    activeColor: Color,
    onClick: () -> Unit
) {
    val isSelected = mode == current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected)
                    Brush.verticalGradient(listOf(activeColor, activeColor.copy(alpha = 0.7f)))
                else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$icon $label",
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else PianoColors.TextSecondary
        )
    }
}

@Composable
private fun ViewModeChip(
    label: String,
    mode: FallingMode,
    current: FallingMode,
    onClick: () -> Unit
) {
    val isSelected = mode == current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(
                if (isSelected) PianoColors.Blue.copy(alpha = 0.25f) else Color(0xFF1A1C2A)
            )
            .border(
                0.5.dp,
                if (isSelected) PianoColors.Blue else Color(0xFF2A2D3E),
                RoundedCornerShape(5.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            fontSize = 9.sp,
            color = if (isSelected) PianoColors.Blue else PianoColors.TextSecondary
        )
    }
}

@Composable
private fun SettingsPanel(
    volume: Float,
    tempo: Float,
    showNoteNames: Boolean,
    showNextHint: Boolean,
    onVolumeChange: (Float) -> Unit,
    onTempoChange: (Float) -> Unit,
    onToggleNoteNames: () -> Unit,
    onToggleNextHint: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1220))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🔊 볼륨", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(52.dp))
            Slider(
                value = volume, onValueChange = onVolumeChange, valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = PianoColors.Amber, activeTrackColor = PianoColors.Amber, inactiveTrackColor = Color(0xFF2A2D3E))
            )
            Text("${(volume * 100).toInt()}%", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(30.dp))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🎵 템포", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(52.dp))
            Slider(
                value = tempo, onValueChange = onTempoChange, valueRange = 0.5f..2.0f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = PianoColors.Blue, activeTrackColor = PianoColors.Blue, inactiveTrackColor = Color(0xFF2A2D3E))
            )
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.clickable { onToggle() }
    ) {
        Text(label, fontSize = 10.sp, color = PianoColors.TextSecondary)
        Switch(
            checked = value, onCheckedChange = { onToggle() },
            modifier = Modifier.height(20.dp).width(36.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = PianoColors.Amber,
                uncheckedThumbColor = PianoColors.TextMuted, uncheckedTrackColor = Color(0xFF2A2D3E)
            )
        )
    }
}
