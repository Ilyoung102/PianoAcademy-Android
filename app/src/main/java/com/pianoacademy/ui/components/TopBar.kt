package com.pianoacademy.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.*
import com.pianoacademy.audio.SoundMode
import com.pianoacademy.data.Song
import com.pianoacademy.ui.theme.PianoColors
import com.pianoacademy.viewmodel.FallingMode
import com.pianoacademy.viewmodel.PlayMode

// ── 상단 컨트롤 바 ─────────────────────────────────────────────
@Composable
fun TopBar(
    selectedSong: Song?,
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0E1018), Color(0xFF12151F))
                )
            )
    ) {
        // ── 메인 컨트롤 행 ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 앱 타이틀 (작은 아이콘 + 이름)
            Text(
                "🎹",
                fontSize = 16.sp
            )
            Text(
                "피아노 아카데미",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PianoColors.Amber
            )

            Spacer(Modifier.weight(1f))

            // 선택된 곡 표시
            if (selectedSong != null) {
                Text(
                    selectedSong.title,
                    fontSize = 10.sp,
                    color = PianoColors.TextSecondary,
                    maxLines = 1
                )
                Spacer(Modifier.weight(0.5f))
            }

            // 모드 버튼 그룹 (아이콘 + 텍스트)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1C2033))
                    .border(1.dp, Color(0xFF2A2D3E), RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
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
                val btnColor = if (isPlaying)
                    Brush.horizontalGradient(listOf(PianoColors.Rose, Color(0xFFBE123C)))
                else
                    Brush.horizontalGradient(listOf(PianoColors.Emerald, Color(0xFF059669)))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(btnColor)
                        .clickable { onPlayStop() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (isPlaying) "■ 정지" else "▶ 시작",
                        fontSize = 11.sp,
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
                    .padding(4.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "설정",
                    tint = if (showSettings) PianoColors.Amber else PianoColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── 진행 바 (재생/따라하기 중) ──────────────────────
        if (isPlaying && totalSteps > 0 && playMode != PlayMode.FREE) {
            val progress = if (totalSteps > 1) stepIndex.toFloat() / (totalSteps - 1) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = when (playMode) {
                    PlayMode.AUTO        -> PianoColors.Blue
                    PlayMode.INTERACTIVE -> PianoColors.Emerald
                    else                 -> PianoColors.Amber
                },
                trackColor = Color(0xFF1C2033)
            )
        } else {
            Divider(color = Color(0xFF1E2230), thickness = 1.dp)
        }

        // ── 2행: 뷰 선택 + 음색 선택 ────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("뷰", fontSize = 9.sp, color = PianoColors.TextMuted)
            Spacer(Modifier.width(2.dp))

            listOf(
                FallingMode.OFF  to "악보",
                FallingMode.DOWN to "⬇ 폭포수",
                FallingMode.UP   to "⬆ 역폭포"
            ).forEach { (mode, label) ->
                FallingModeButton(label, mode, fallingMode) { onFallingModeChange(mode) }
            }

            Spacer(Modifier.weight(1f))

            // 음색 선택
            Text("음색", fontSize = 9.sp, color = PianoColors.TextMuted)
            Spacer(Modifier.width(2.dp))
            SoundMode.values().forEach { sm ->
                val isSelected = sm == soundMode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            if (isSelected)
                                Brush.verticalGradient(listOf(PianoColors.Violet, Color(0xFF6D28D9)))
                            else
                                Brush.verticalGradient(listOf(Color(0xFF1E2030), Color(0xFF1A1C2A)))
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) PianoColors.Violet else Color(0xFF2A2D3E),
                            shape = RoundedCornerShape(5.dp)
                        )
                        .clickable { onSoundModeChange(sm) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(sm.icon, fontSize = 12.sp)
                }
            }
        }

        // ── 설정 패널 (토글) ──────────────────────────────
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
                    Brush.verticalGradient(
                        listOf(activeColor, activeColor.copy(alpha = 0.7f))
                    )
                else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(icon, fontSize = 9.sp, color = if (isSelected) Color.White else PianoColors.TextMuted)
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else PianoColors.TextSecondary
            )
        }
    }
}

@Composable
private fun FallingModeButton(
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
                if (isSelected) PianoColors.Blue.copy(alpha = 0.25f)
                else Color(0xFF1A1C2A)
            )
            .border(
                width = if (isSelected) 1.dp else 0.5.dp,
                color = if (isSelected) PianoColors.Blue else Color(0xFF2A2D3E),
                shape = RoundedCornerShape(5.dp)
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 볼륨 슬라이더
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "🔊 볼륨",
                fontSize = 10.sp,
                color = PianoColors.TextSecondary,
                modifier = Modifier.width(56.dp)
            )
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = PianoColors.Amber,
                    activeTrackColor = PianoColors.Amber,
                    inactiveTrackColor = Color(0xFF2A2D3E)
                )
            )
            Text(
                "${(volume * 100).toInt()}%",
                fontSize = 10.sp,
                color = PianoColors.TextSecondary,
                modifier = Modifier.width(32.dp)
            )
        }

        // 템포 슬라이더
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "🎵 템포",
                fontSize = 10.sp,
                color = PianoColors.TextSecondary,
                modifier = Modifier.width(56.dp)
            )
            Slider(
                value = tempo,
                onValueChange = onTempoChange,
                valueRange = 0.5f..2.0f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = PianoColors.Blue,
                    activeTrackColor = PianoColors.Blue,
                    inactiveTrackColor = Color(0xFF2A2D3E)
                )
            )
            Text(
                "×${String.format("%.1f", tempo)}",
                fontSize = 10.sp,
                color = PianoColors.TextSecondary,
                modifier = Modifier.width(32.dp)
            )
        }

        // 토글 스위치들
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ToggleRow("음이름 표시", showNoteNames, onToggleNoteNames)
            ToggleRow("다음 음 힌트", showNextHint, onToggleNextHint)
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable { onToggle() }
    ) {
        Text(label, fontSize = 10.sp, color = PianoColors.TextSecondary)
        Switch(
            checked = value,
            onCheckedChange = { onToggle() },
            modifier = Modifier.height(20.dp).width(36.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PianoColors.Amber,
                uncheckedThumbColor = PianoColors.TextMuted,
                uncheckedTrackColor = Color(0xFF2A2D3E)
            )
        )
    }
}
