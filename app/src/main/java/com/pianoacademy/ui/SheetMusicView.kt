package com.pianoacademy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import com.pianoacademy.data.*
import com.pianoacademy.ui.theme.PianoColors
import com.pianoacademy.viewmodel.PlayMode
import kotlin.math.absoluteValue

// ── 수평 악보 뷰 ───────────────────────────────────────────────
@Composable
fun SheetMusicView(
    song: Song,
    stepIndex: Int,
    playMode: PlayMode,
    modifier: Modifier = Modifier
) {
    val BW = 120f  // 비트당 픽셀 너비
    val SS = 9f    // 오선 간격

    // 드래그로 악보 이동
    var scrollOffset by remember { mutableStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF15171E))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, delta ->
                    scrollOffset = (scrollOffset + delta).coerceIn(-5000f, 0f)
                }
            }
    ) {
        val viewW = size.width
        val viewH = size.height
        val centerY = viewH * 0.4f

        // 현재 스텝 기준 오토 스크롤
        val autoScroll = if (playMode != PlayMode.FREE) {
            val currentLeft = song.steps.take(stepIndex).sumOf { it.duration.toDouble() }.toFloat() * BW
            -(currentLeft - viewW / 2f)
        } else scrollOffset

        // ── 오선 그리기 ────────────────────────────────────
        for (i in -2..2) {
            drawLine(
                color = Color(0xFF4A5568).copy(alpha = 0.6f),
                start = Offset(0f, centerY - i * SS),
                end = Offset(viewW, centerY - i * SS),
                strokeWidth = 1f
            )
        }

        // ── 중앙 커서 라인 ──────────────────────────────────
        drawLine(
            color = PianoColors.Amber.copy(alpha = 0.8f),
            start = Offset(viewW / 2f, 0f),
            end = Offset(viewW / 2f, viewH),
            strokeWidth = 2f
        )

        // ── 음표 그리기 ─────────────────────────────────────
        var xPos = 0f
        song.steps.forEachIndexed { idx, step ->
            val w = step.duration * BW
            val screenX = xPos + viewW / 2f + autoScroll
            xPos += w

            if (screenX < -100f || screenX > viewW + 100f) return@forEachIndexed

            val isPast    = idx < stepIndex
            val isCurrent = idx == stepIndex

            val noteColor = when {
                isPast -> Color(0xFF374151)
                isCurrent -> if (playMode == PlayMode.AUTO) PianoColors.Blue else PianoColors.Emerald
                else -> Color(0xFF94A3B8)
            }

            step.keys.forEach { note ->
                val offset = NOTE_OFFSETS[note] ?: 0
                val noteY = centerY - offset * SS

                // 음표 머리
                drawOval(
                    color = noteColor,
                    topLeft = Offset(screenX - 6f, noteY - 5f),
                    size = Size(13f, 9f)
                )

                // 임시표 (샤프)
                if (note.contains("#")) {
                    drawLine(
                        color = noteColor,
                        start = Offset(screenX - 16f, noteY - 8f),
                        end = Offset(screenX - 16f, noteY + 4f),
                        strokeWidth = 1.5f
                    )
                }

                // 가온선 (C4, B5, C3)
                if (note == "C4" || note == "C3" || note == "B5") {
                    drawLine(
                        color = noteColor.copy(alpha = 0.6f),
                        start = Offset(screenX - 10f, noteY),
                        end = Offset(screenX + 10f, noteY),
                        strokeWidth = 1.5f
                    )
                }

                // 음이름 (하단)
                // Canvas에서 직접 텍스트는 nativePaint 사용
            }

            // 음표 아래 이름 표시 박스
            val boxColor = when {
                isPast -> Color(0xFF1E2030)
                isCurrent -> if (playMode == PlayMode.AUTO)
                    PianoColors.Blue.copy(alpha = 0.9f)
                else PianoColors.Emerald.copy(alpha = 0.9f)
                else -> Color(0xFF22253A)
            }
            val boxH = if (step.keys.size > 1) 36f else 28f
            drawRoundRect(
                color = boxColor,
                topLeft = Offset(screenX - w / 2f + 2f, viewH * 0.85f),
                size = Size(w - 4f, boxH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
            )
        }
    }
}
