package com.pianoacademy.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pianoacademy.data.*
import com.pianoacademy.ui.theme.PianoColors

// ── 곡 선택 바텀 시트 ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPickerSheet(
    selectedLevel: Int,
    selectedSong: Song?,
    bestScores: Map<String, Int>,
    onLevelSelect: (Int) -> Unit,
    onSongSelect: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E1018),
        contentColor = PianoColors.TextPrimary,
        dragHandle = {
            // 커스텀 핸들
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E1018)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF3A3D50))
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🎵 곡 선택",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PianoColors.TextPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    val levelCfg = LEVEL_CONFIG[selectedLevel]
                    if (levelCfg != null) {
                        val songs = SONGS.filter { it.level == selectedLevel }
                        Text(
                            "${levelCfg.icon} ${levelCfg.label} · ${songs.size}곡",
                            fontSize = 11.sp,
                            color = Color(levelCfg.colorHex)
                        )
                    }
                }
                Divider(color = Color(0xFF1A1D2A), thickness = 1.dp)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ── 레벨 탭 ─────────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0E14))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(7) { idx ->
                    val level = idx + 1
                    val cfg = LEVEL_CONFIG[level] ?: return@items
                    val isSelected = level == selectedLevel
                    val color = Color(cfg.colorHex)
                    val songCount = SONGS.count { it.level == level }
                    val clearedCount = SONGS.filter { it.level == level }
                        .count { bestScores[it.id] != null }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected)
                                    Brush.verticalGradient(listOf(color, color.copy(alpha = 0.7f)))
                                else
                                    Brush.verticalGradient(listOf(Color(0xFF1C1F2E), Color(0xFF171A26)))
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) color else Color(0xFF252840),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onLevelSelect(level) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(cfg.icon, fontSize = 16.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                cfg.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else PianoColors.TextSecondary
                            )
                            Text(
                                "$clearedCount/$songCount",
                                fontSize = 8.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else PianoColors.TextMuted
                            )
                        }
                    }
                }
            }

            Divider(color = Color(0xFF1A1D2A), thickness = 1.dp)

            // ── 곡 목록 ─────────────────────────────────────────
            val songs = SONGS.filter { it.level == selectedLevel }
            val levelCfg = LEVEL_CONFIG[selectedLevel]

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(songs) { song ->
                    val isSelected = song.id == selectedSong?.id
                    val best = bestScores[song.id]
                    val levelColor = Color(levelCfg?.colorHex ?: 0xFF10B981)
                    val stars = best?.let {
                        when { it >= 95 -> 3; it >= 75 -> 2; it >= 50 -> 1; else -> 0 }
                    } ?: -1

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected)
                                    Brush.horizontalGradient(
                                        listOf(levelColor.copy(alpha = 0.28f), levelColor.copy(alpha = 0.10f))
                                    )
                                else Brush.horizontalGradient(
                                    listOf(Color(0xFF191C28), Color(0xFF161924))
                                )
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) levelColor.copy(alpha = 0.7f) else Color(0xFF222535),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onSongSelect(song)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 번호 배지
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (isSelected) levelColor.copy(alpha = 0.3f)
                                    else Color(0xFF222535)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${songs.indexOf(song) + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) levelColor else PianoColors.TextMuted
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                song.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) levelColor else PianoColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("♩=${song.tempo}", fontSize = 9.sp, color = PianoColors.TextMuted)
                                Text("·", fontSize = 9.sp, color = PianoColors.TextMuted)
                                Text("${song.steps.size}음", fontSize = 9.sp, color = PianoColors.TextMuted)
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // 별점 / NEW
                        if (stars >= 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row {
                                    repeat(3) { i ->
                                        Text(
                                            if (i < stars) "★" else "☆",
                                            fontSize = 10.sp,
                                            color = if (i < stars) PianoColors.Amber else Color(0xFF333645)
                                        )
                                    }
                                }
                                Text("$best%", fontSize = 9.sp, color = PianoColors.TextMuted)
                            }
                        } else {
                            Text(
                                "NEW",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = PianoColors.Emerald.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PianoColors.Emerald.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
