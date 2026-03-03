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
            .background(Color(0xFF111318))
    ) {
        // ── 메인 컨트롤 행 ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 앱 타이틀
            Text(
                "🎹 피아노 아카데미",
                fontSize = 12.sp,
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
                Spacer(Modifier.weight(1f))
            }

            // 모드 버튼 그룹
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E2030))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ModeButton("자유", PlayMode.FREE, playMode) { onPlayModeChange(PlayMode.FREE) }
                ModeButton("재생", PlayMode.AUTO, playMode) { onPlayModeChange(PlayMode.AUTO) }
                ModeButton("따라하기", PlayMode.INTERACTIVE, playMode) { onPlayModeChange(PlayMode.INTERACTIVE) }
            }

            // 재생/정지 버튼
            if (selectedSong != null && playMode != PlayMode.FREE) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isPlaying) PianoColors.Rose else PianoColors.Emerald)
                        .clickable { onPlayStop() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (isPlaying) "■ 정지" else "▶ 시작",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // 설정 버튼
            IconButton(
                onClick = onToggleSettings,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "설정",
                    tint = if (showSettings) PianoColors.Amber else PianoColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── 폭포수 모드 선택 ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("뷰:", fontSize = 9.sp, color = PianoColors.TextMuted)
            listOf(
                FallingMode.OFF to "악보",
                FallingMode.DOWN to "⬇️ 폭포수",
                FallingMode.UP to "⬆️ 역폭포"
            ).forEach { (mode, label) ->
                FallingModeButton(label, mode, fallingMode) { onFallingModeChange(mode) }
            }

            Spacer(Modifier.weight(1f))

            // 음색 선택
            Text("음색:", fontSize = 9.sp, color = PianoColors.TextMuted)
            SoundMode.values().forEach { sm ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (sm == soundMode) PianoColors.Violet else Color(0xFF1E2030))
                        .border(
                            1.dp,
                            if (sm == soundMode) PianoColors.Violet else Color(0xFF2A2D3E),
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onSoundModeChange(sm) }
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                ) {
                    Text(sm.icon, fontSize = 11.sp)
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
    mode: PlayMode,
    current: PlayMode,
    onClick: () -> Unit
) {
    val isSelected = mode == current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    isSelected && mode == PlayMode.AUTO -> PianoColors.Blue
                    isSelected && mode == PlayMode.INTERACTIVE -> PianoColors.Emerald
                    isSelected -> PianoColors.Amber
                    else -> Color.Transparent
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else PianoColors.TextSecondary
        )
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
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) PianoColors.Blue.copy(alpha = 0.7f) else Color(0xFF1E2030))
            .border(
                1.dp,
                if (isSelected) PianoColors.Blue else Color(0xFF2A2D3E),
                RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            label,
            fontSize = 9.sp,
            color = if (isSelected) Color.White else PianoColors.TextSecondary
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
            .background(Color(0xFF16182A))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 볼륨 슬라이더
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔊 볼륨", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(60.dp))
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = PianoColors.Amber, activeTrackColor = PianoColors.Amber)
            )
            Text("${(volume * 100).toInt()}%", fontSize = 10.sp, color = PianoColors.TextSecondary)
        }

        // 템포 슬라이더
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🎵 템포", fontSize = 10.sp, color = PianoColors.TextSecondary, modifier = Modifier.width(60.dp))
            Slider(
                value = tempo,
                onValueChange = onTempoChange,
                valueRange = 0.5f..2.0f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = PianoColors.Blue, activeTrackColor = PianoColors.Blue)
            )
            Text("×${String.format("%.1f", tempo)}", fontSize = 10.sp, color = PianoColors.TextSecondary)
        }

        // 토글 스위치들
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ToggleRow("🎵 음이름 표시", showNoteNames, onToggleNoteNames)
            ToggleRow("💡 다음 음 힌트", showNextHint, onToggleNextHint)
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
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PianoColors.Amber)
        )
    }
}
