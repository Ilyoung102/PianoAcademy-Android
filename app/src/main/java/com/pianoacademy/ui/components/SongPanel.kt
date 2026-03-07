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

    Column(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(Color(0xFF0E1018), Color(0xFF111520))
            )
        )
    ) {
        // ── 레벨 탭 ─────────────────────────────────────────
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C0E14))
                .padding(vertical = 5.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(7) { idx ->
                val level = idx + 1
                val cfg = LEVEL_CONFIG[level] ?: return@items
                val isSelected = level == selectedLevel
                val color = Color(cfg.colorHex)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected)
                                Brush.verticalGradient(listOf(color, color.copy(alpha = 0.7f)))
                            else
                                Brush.verticalGradient(listOf(Color(0xFF1C1F2E), Color(0xFF171A26)))
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) color else Color(0xFF252840),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onLevelSelect(level) }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(cfg.icon, fontSize = 12.sp)
                        Text(
                            cfg.label,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else PianoColors.TextSecondary
                        )
                    }
                }
            }
        }

        // 구분선
        Divider(color = Color(0xFF1A1D2A), thickness = 1.dp)

        // ── 곡 목록 ─────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(songs) { song ->
                val isSelected = song.id == selectedSong?.id
                val best = bestScores[song.id]
                val levelColor = Color(levelCfg?.colorHex ?: 0xFF10B981)
                val stars = best?.let {
                    when {
                        it >= 95 -> 3
                        it >= 75 -> 2
                        it >= 50 -> 1
                        else -> 0
                    }
                } ?: -1

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected)
                                Brush.horizontalGradient(
                                    listOf(
                                        levelColor.copy(alpha = 0.25f),
                                        levelColor.copy(alpha = 0.10f)
                                    )
                                )
                            else
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF191C28), Color(0xFF161924))
                                )
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) levelColor.copy(alpha = 0.7f) else Color(0xFF222535),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSongSelect(song) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 곡 순서 번호
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) levelColor.copy(alpha = 0.3f)
                                else Color(0xFF222535)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${songs.indexOf(song) + 1}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) levelColor else PianoColors.TextMuted
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            song.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) levelColor else PianoColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "♩=${song.tempo}",
                                fontSize = 8.sp,
                                color = PianoColors.TextMuted
                            )
                            Text(
                                "•",
                                fontSize = 8.sp,
                                color = PianoColors.TextMuted
                            )
                            Text(
                                "${song.steps.size}음",
                                fontSize = 8.sp,
                                color = PianoColors.TextMuted
                            )
                        }
                    }

                    // 최고 점수 / 별점
                    if (stars >= 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row {
                                repeat(3) { i ->
                                    Text(
                                        if (i < stars) "★" else "☆",
                                        fontSize = 9.sp,
                                        color = if (i < stars) PianoColors.Amber else Color(0xFF333645)
                                    )
                                }
                            }
                            Text(
                                "$best%",
                                fontSize = 8.sp,
                                color = PianoColors.TextMuted
                            )
                        }
                    } else {
                        // 미플레이
                        Text(
                            "NEW",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = PianoColors.Emerald.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(PianoColors.Emerald.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
