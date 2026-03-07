package com.pianoacademy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pianoacademy.ui.theme.PianoColors
import com.pianoacademy.viewmodel.GameResult

@Composable
fun GameResultDialog(
    result: GameResult,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A1D2E), Color(0xFF131620))
                    )
                )
                .border(1.dp, Color(0xFF2A2D45), RoundedCornerShape(24.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 트로피 이모지
                Text(
                    when (result.stars) {
                        3    -> "🏆"
                        2    -> "🥈"
                        1    -> "🥉"
                        else -> "😢"
                    },
                    fontSize = 48.sp
                )

                // 별점
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { i ->
                        Text(
                            if (i < result.stars) "★" else "☆",
                            fontSize = 28.sp,
                            color = if (i < result.stars) PianoColors.Amber else Color(0xFF333645)
                        )
                    }
                }

                // 정확도 대형 표시
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E2238), Color(0xFF181B2C))
                            )
                        )
                        .border(1.dp, Color(0xFF2A2D4A), RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${result.accuracy}%",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                result.accuracy >= 95 -> PianoColors.Amber
                                result.accuracy >= 75 -> PianoColors.Blue
                                result.accuracy >= 50 -> PianoColors.Emerald
                                else                  -> PianoColors.Rose
                            }
                        )
                        Text(
                            "정확도",
                            fontSize = 10.sp,
                            color = PianoColors.TextMuted
                        )
                    }
                }

                // 상세 통계
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip("틀린 음", "${result.wrongCount}번", PianoColors.Rose)
                    if (result.prevBest != null) {
                        StatChip(
                            "이전 최고",
                            "${result.prevBest}%",
                            PianoColors.TextMuted
                        )
                    }
                }

                // 최고기록 메시지
                val message = when {
                    result.prevBest == null ->
                        Pair("🎉 첫 완주!", PianoColors.Emerald)
                    result.accuracy > result.prevBest ->
                        Pair("🔥 최고기록 갱신!", PianoColors.Amber)
                    result.accuracy == result.prevBest ->
                        Pair("최고기록 유지", PianoColors.Blue)
                    else ->
                        Pair("계속 연습하세요!", PianoColors.TextSecondary)
                }
                Text(
                    message.first,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = message.second
                )

                Spacer(Modifier.height(4.dp))

                // 버튼
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PianoColors.Emerald
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "🔄 다시하기",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PianoColors.TextSecondary
                        )
                    ) {
                        Text("닫기", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 9.sp, color = PianoColors.TextMuted)
    }
}
