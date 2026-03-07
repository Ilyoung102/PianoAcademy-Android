package com.pianoacademy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.pianoacademy.data.*
import com.pianoacademy.ui.theme.PianoColors
import com.pianoacademy.viewmodel.PlayMode

@Composable
fun SheetMusicView(
    song: Song,
    stepIndex: Int,
    playMode: PlayMode,
    modifier: Modifier = Modifier
) {
    // BW = pixels per beat
    val BW = 150f
    // Staff step = pixels per staff line gap
    val SS = 10f

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF13151D))
    ) {
        val vw = size.width
        val vh = size.height

        // Staff center Y = 42% from top
        val centerY = vh * 0.42f
        // Piano roll block area bottom Y
        val blockBottom = vh * 0.90f
        val blockH = 34f

        // Auto-scroll: current step centered at vw/2
        val currentLeft = song.steps.take(stepIndex).sumOf { it.duration.toDouble() }.toFloat() * BW
        val scrollX = vw / 2f - currentLeft

        // ── 배경 수평선 (악보 분위기) ─────────────────────────
        for (li in -4..4) {
            val lineY = centerY - li * SS * 2
            drawLine(
                color = Color(0xFF1E2233),
                start = Offset(0f, lineY),
                end = Offset(vw, lineY),
                strokeWidth = 0.8f
            )
        }

        // ── 5선 (오선) ────────────────────────────────────────
        for (i in -2..2) {
            drawLine(
                color = Color(0xFF3A3F5C),
                start = Offset(0f, centerY - i * SS),
                end = Offset(vw, centerY - i * SS),
                strokeWidth = 1.2f
            )
        }

        // ── 음표 & 피아노롤 블록 ──────────────────────────────
        var xAccum = 0f
        song.steps.forEachIndexed { idx, step ->
            val noteW = step.duration * BW
            val sx = xAccum * 1f + scrollX  // screen x (left edge of note block)
            val centerX = sx + noteW / 2f    // center x
            xAccum += noteW

            // 화면 밖이면 스킵
            if (centerX < -noteW || centerX > vw + noteW) return@forEachIndexed

            val isPast    = idx < stepIndex
            val isCurrent = idx == stepIndex

            // 색상 결정
            val noteColor = when {
                isPast    -> Color(0xFF2A2E40)
                isCurrent -> if (playMode == PlayMode.AUTO) PianoColors.Blue else PianoColors.Emerald
                else      -> Color(0xFF4A5270)
            }
            val blockColor = when {
                isPast    -> Color(0xFF181B28)
                isCurrent -> if (playMode == PlayMode.AUTO)
                    PianoColors.Blue.copy(alpha = 0.88f)
                else PianoColors.Emerald.copy(alpha = 0.88f)
                else      -> Color(0xFF1E2235)
            }
            val blockBorder = when {
                isPast    -> Color(0xFF222535)
                isCurrent -> if (playMode == PlayMode.AUTO) PianoColors.Blue else PianoColors.Emerald
                else      -> Color(0xFF2A2D45)
            }

            // ── 음표 머리 그리기 ────────────────────────────
            step.keys.forEach { note ->
                val offset = NOTE_OFFSETS[note] ?: 0
                val noteY = centerY - offset * SS

                // 가온선 (C4, C3)
                if (note == "C4" || note == "C3") {
                    drawLine(
                        color = noteColor.copy(alpha = 0.7f),
                        start = Offset(centerX - 11f, noteY),
                        end = Offset(centerX + 11f, noteY),
                        strokeWidth = 1.5f
                    )
                }

                // 샤프 기호
                if (note.contains("#")) {
                    drawLine(color = noteColor, start = Offset(centerX - 17f, noteY - 8f),
                        end = Offset(centerX - 17f, noteY + 5f), strokeWidth = 1.5f)
                    drawLine(color = noteColor, start = Offset(centerX - 20f, noteY - 4f),
                        end = Offset(centerX - 13f, noteY - 4f), strokeWidth = 1.2f)
                    drawLine(color = noteColor, start = Offset(centerX - 20f, noteY + 1f),
                        end = Offset(centerX - 13f, noteY + 1f), strokeWidth = 1.2f)
                }

                val isHalf = step.duration >= 2f && step.duration < 4f
                val isWhole = step.duration >= 4f

                // 음표 머리 (타원)
                if (isWhole || isHalf) {
                    // 속 빈 음표
                    drawOval(color = noteColor, topLeft = Offset(centerX - 7f, noteY - 5f), size = Size(14f, 10f))
                    drawOval(color = Color(0xFF13151D), topLeft = Offset(centerX - 4f, noteY - 3f), size = Size(8f, 6f))
                } else {
                    drawOval(color = noteColor, topLeft = Offset(centerX - 7f, noteY - 5f), size = Size(14f, 10f))
                }

                // 기둥 (온음표 제외)
                if (!isWhole) {
                    val stemUp = offset <= 0
                    if (stemUp) {
                        drawLine(color = noteColor,
                            start = Offset(centerX + 6f, noteY),
                            end = Offset(centerX + 6f, noteY - 28f),
                            strokeWidth = 1.5f)
                        // 8분음표 꼬리
                        if (step.duration < 1f) {
                            drawLine(color = noteColor,
                                start = Offset(centerX + 6f, noteY - 28f),
                                end = Offset(centerX + 16f, noteY - 16f),
                                strokeWidth = 2f)
                        }
                    } else {
                        drawLine(color = noteColor,
                            start = Offset(centerX - 6f, noteY),
                            end = Offset(centerX - 6f, noteY + 28f),
                            strokeWidth = 1.5f)
                        if (step.duration < 1f) {
                            drawLine(color = noteColor,
                                start = Offset(centerX - 6f, noteY + 28f),
                                end = Offset(centerX + 4f, noteY + 16f),
                                strokeWidth = 2f)
                        }
                    }
                }
            }

            // ── 피아노 롤 블록 (하단) ─────────────────────────
            val blockTop = blockBottom - blockH
            val bx = sx + 2f
            val bw = (noteW - 4f).coerceAtLeast(20f)

            // 블록 배경
            drawRoundRect(
                color = blockColor,
                topLeft = Offset(bx, blockTop),
                size = Size(bw, blockH),
                cornerRadius = CornerRadius(5f)
            )
            // 블록 테두리 (현재 음표 강조)
            if (isCurrent) {
                drawRoundRect(
                    color = blockBorder.copy(alpha = 0.6f),
                    topLeft = Offset(bx, blockTop),
                    size = Size(bw, blockH),
                    cornerRadius = CornerRadius(5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                )
            }

            // 블록 텍스트 (음이름 + 음표기호)
            if (bw > 24f) {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = if (isPast) 0xFF3A3F5C.toInt() else android.graphics.Color.WHITE
                        textSize = if (bw > 50f) 13f else 10f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val noteLabel = step.keys.joinToString("+") { getKoreanName(it) }
                    val symbol = getNoteSymbol(step.duration)
                    val displayText = if (bw > 60f) "$noteLabel $symbol" else noteLabel
                    canvas.nativeCanvas.drawText(
                        displayText,
                        sx + noteW / 2f,
                        blockTop + blockH * 0.68f,
                        paint
                    )
                }
            }
        }

        // ── 중앙 커서 라인 (amber, 글로우) ───────────────────
        // 글로우 효과 (넓은 반투명 선)
        drawLine(
            color = PianoColors.Amber.copy(alpha = 0.20f),
            start = Offset(vw / 2f, 0f),
            end = Offset(vw / 2f, vh),
            strokeWidth = 16f
        )
        drawLine(
            color = PianoColors.Amber.copy(alpha = 0.50f),
            start = Offset(vw / 2f, 0f),
            end = Offset(vw / 2f, vh),
            strokeWidth = 4f
        )
        drawLine(
            color = PianoColors.Amber,
            start = Offset(vw / 2f, 0f),
            end = Offset(vw / 2f, vh),
            strokeWidth = 2f
        )
    }
}
