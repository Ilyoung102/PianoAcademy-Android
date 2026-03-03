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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.pianoacademy.ui.theme.PianoColors
import com.pianoacademy.viewmodel.GameResult

@Composable
fun GameResultDialog(
    result: GameResult,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1D27))
                .border(1.dp, Color(0xFF2A2D3E), RoundedCornerShape(20.dp))
                .padding(24.dp)
                .width(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 이모지 트로피
                Text(
                    when (result.stars) {
                        3 -> "🏆"
                        2 -> "🥈"
                        1 -> "🥉"
                        else -> "😢"
                    },
                    fontSize = 40.sp
                )

                // 별점
                Text(
                    "★".repeat(result.stars) + "☆".repeat(3 - result.stars),
                    fontSize = 24.sp,
                    color = PianoColors.Amber
                )

                // 정확도
                Text(
                    "${result.accuracy}%",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PianoColors.Amber
                )

                Text(
                    "틀린 음: ${result.wrongCount}번",
                    fontSize = 13.sp,
                    color = PianoColors.TextSecondary
                )

                // 최고기록 메시지
                when {
                    result.prevBest == null ->
                        Text("🎉 첫 완주!", fontSize = 11.sp, color = PianoColors.Emerald)
                    result.accuracy > result.prevBest ->
                        Text("🔥 최고기록 갱신! (이전 ${result.prevBest}%)", fontSize = 11.sp, color = PianoColors.Emerald)
                    else ->
                        Text("최고기록: ${result.prevBest}%", fontSize = 11.sp, color = PianoColors.TextMuted)
                }

                Spacer(Modifier.height(4.dp))

                // 버튼
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = PianoColors.Emerald),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🔄 다시하기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Text("닫기", fontSize = 12.sp, color = PianoColors.TextSecondary)
                    }
                }
            }
        }
    }
}
