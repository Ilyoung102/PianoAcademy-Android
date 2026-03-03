package com.pianoacademy.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.pianoacademy.data.*
import com.pianoacademy.ui.theme.PianoColors

// ── 레벨 탭 + 곡 목록 패널 ───────────────────────────────────
@Composable
fun SongPanel(
    selectedLevel: Int,
    selectedSong: Song?,
    bestScores: Map<String, Int>,
    onLevelSelect: (Int) -> Unit,
    onSongSelect: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val songs = SONGS.filter { it.level == selectedLevel }
    val levelCfg = LEVEL_CONFIG[selectedLevel]

    Column(modifier = modifier) {
        // ── 레벨 탭 ─────────────────────────────────────────
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111318))
                .padding(vertical = 4.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(7) { idx ->
                val level = idx + 1
                val cfg = LEVEL_CONFIG[level] ?: return@items
                val isSelected = level == selectedLevel
                val color = Color(cfg.colorHex)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) color else Color(0xFF1E2030))
                        .border(
                            1.dp,
                            if (isSelected) color else Color(0xFF2A2D3E),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onLevelSelect(level) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(cfg.icon, fontSize = 11.sp)
                        Text(
                            cfg.label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else PianoColors.TextSecondary
                        )
                    }
                }
            }
        }

        // ── 곡 목록 ─────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF111318)),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(songs) { song ->
                val isSelected = song.id == selectedSong?.id
                val best = bestScores[song.id]
                val levelColor = Color(levelCfg?.colorHex ?: 0xFF10B981)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) levelColor.copy(alpha = 0.2f)
                            else Color(0xFF1A1D27)
                        )
                        .border(
                            1.dp,
                            if (isSelected) levelColor else Color(0xFF2A2D3E),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSongSelect(song) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            song.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) levelColor else PianoColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "♩=${song.tempo}  ${song.steps.size}음",
                            fontSize = 9.sp,
                            color = PianoColors.TextMuted
                        )
                    }

                    // 최고 점수 표시
                    if (best != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val stars = when {
                                best >= 95 -> 3
                                best >= 75 -> 2
                                best >= 50 -> 1
                                else -> 0
                            }
                            Text(
                                "★".repeat(stars) + "☆".repeat(3 - stars),
                                fontSize = 8.sp,
                                color = PianoColors.Amber
                            )
                            Text("$best%", fontSize = 8.sp, color = PianoColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
