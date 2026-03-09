package com.pianoacademy.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.pianoacademy.data.*
import com.pianoacademy.ui.theme.PianoColors
import com.pianoacademy.viewmodel.PlayMode

// 2옥타브 피아노롤에 표시할 음 목록 (위=높은음, 아래=낮은음)
// C3~B4 (24음, 2옥타브)
private val ROLL_NOTES = listOf(
    "B4","A#4","A4","G#4","G4","F#4","F4","E4","D#4","D4","C#4","C4",
    "B3","A#3","A3","G#3","G3","F#3","F3","E3","D#3","D3","C#3","C3"
)
private val BLACK_NOTES = setOf(
    "A#4","G#4","F#4","D#4","C#4",
    "A#3","G#3","F#3","D#3","C#3"
)

@Composable
fun SheetMusicView(
    song: Song,
    stepIndex: Int,
    playMode: PlayMode,
    isPlaying: Boolean = false,
    tempoMultiplier: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    // 현재 스텝까지 누적 beat 수
    val targetBeat = remember(song, stepIndex) {
        song.steps.take(stepIndex).sumOf { it.duration.toDouble() }.toFloat()
    }

    // 현재 스텝 duration → 애니메이션 시간을 note 길이에 맞춤
    val animDurationMs = remember(song, stepIndex, tempoMultiplier) {
        if (!isPlaying) 220
        else {
            val d = song.steps.getOrNull(stepIndex)?.duration ?: 1f
            (d * 60000f / song.tempo / tempoMultiplier).toInt().coerceIn(60, 3000)
        }
    }

    // 부드러운 스크롤 애니메이션
    val animBeat by animateFloatAsState(
        targetValue = targetBeat,
        animationSpec = tween(durationMillis = animDurationMs, easing = LinearEasing),
        label = "sheetScroll"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A))
    ) {
        val BW   = 110f          // px per beat (가로 비율)
        val vw   = size.width
        val vh   = size.height

        val scrollX = vw / 2f - animBeat * BW

        // ══════════════════════════════════════════════════════
        // 상단 40%: 오선 악보
        // ══════════════════════════════════════════════════════
        val staffH  = vh * 0.40f
        val SS      = 8.5f                    // 오선 간격
        val centerY = staffH * 0.52f

        // 배경 수평선
        for (li in -5..5) {
            drawLine(
                color = Color(0xFF191C28),
                start = Offset(0f, centerY - li * SS * 2),
                end   = Offset(vw, centerY - li * SS * 2),
                strokeWidth = 0.6f
            )
        }
        // 5선 (오선보)
        for (i in -2..2) {
            drawLine(
                color = Color(0xFF353A58),
                start = Offset(0f, centerY - i * SS),
                end   = Offset(vw, centerY - i * SS),
                strokeWidth = 1.1f
            )
        }

        // 음표 그리기
        var xAcc = 0f
        song.steps.forEachIndexed { idx, step ->
            val noteW   = step.duration * BW
            val sx      = xAcc + scrollX
            val cx      = sx + noteW / 2f   // note head center x
            xAcc += noteW

            if (cx < -noteW || cx > vw + noteW) return@forEachIndexed

            val isPast    = idx < stepIndex
            val isCurrent = idx == stepIndex

            val noteColor = when {
                isPast    -> Color(0xFF282C3E)
                isCurrent -> if (playMode == PlayMode.AUTO) PianoColors.Blue else PianoColors.Emerald
                else      -> Color(0xFF4A5270)
            }

            step.keys.forEach { note ->
                val offset = NOTE_OFFSETS[note] ?: 0
                val noteY  = centerY - offset * SS

                // 가온선 (C4)
                if (note == "C4") {
                    drawLine(color = noteColor.copy(alpha = 0.65f),
                        start = Offset(cx - 12f, noteY), end = Offset(cx + 12f, noteY), strokeWidth = 1.4f)
                }
                // 가온 아래 C3 보조선
                if (note == "C3") {
                    for (li in 0..2) {
                        drawLine(color = noteColor.copy(alpha = 0.5f),
                            start = Offset(cx - 12f, noteY + li * SS),
                            end   = Offset(cx + 12f, noteY + li * SS), strokeWidth = 1.2f)
                    }
                }
                // 높은음 (C5, B4 위) 보조선
                if (note == "C5") {
                    drawLine(color = noteColor.copy(alpha = 0.65f),
                        start = Offset(cx - 12f, noteY), end = Offset(cx + 12f, noteY), strokeWidth = 1.4f)
                }

                // 샤프 기호
                if (note.contains("#")) {
                    drawLine(color = noteColor, start = Offset(cx - 18f, noteY - 9f),
                        end = Offset(cx - 18f, noteY + 6f), strokeWidth = 1.5f)
                    drawLine(color = noteColor, start = Offset(cx - 21f, noteY - 5f),
                        end = Offset(cx - 14f, noteY - 5f), strokeWidth = 1.1f)
                    drawLine(color = noteColor, start = Offset(cx - 21f, noteY + 1f),
                        end = Offset(cx - 14f, noteY + 1f), strokeWidth = 1.1f)
                }

                val isWhole = step.duration >= 4f
                val isHalf  = step.duration >= 2f && step.duration < 4f

                // 음표 머리
                if (isWhole || isHalf) {
                    drawOval(color = noteColor, topLeft = Offset(cx - 7f, noteY - 5f), size = Size(14f, 10f))
                    drawOval(color = Color(0xFF0F111A), topLeft = Offset(cx - 4f, noteY - 3f), size = Size(8f, 6f))
                } else {
                    drawOval(color = noteColor, topLeft = Offset(cx - 7f, noteY - 5f), size = Size(14f, 10f))
                }

                // 기둥
                if (!isWhole) {
                    val stemUp = offset <= 0
                    if (stemUp) {
                        drawLine(color = noteColor,
                            start = Offset(cx + 6f, noteY),
                            end   = Offset(cx + 6f, noteY - 26f), strokeWidth = 1.5f)
                        if (step.duration < 1f) {
                            drawLine(color = noteColor,
                                start = Offset(cx + 6f, noteY - 26f),
                                end   = Offset(cx + 16f, noteY - 14f), strokeWidth = 2f)
                        }
                    } else {
                        drawLine(color = noteColor,
                            start = Offset(cx - 6f, noteY),
                            end   = Offset(cx - 6f, noteY + 26f), strokeWidth = 1.5f)
                        if (step.duration < 1f) {
                            drawLine(color = noteColor,
                                start = Offset(cx - 6f, noteY + 26f),
                                end   = Offset(cx + 4f, noteY + 14f), strokeWidth = 2f)
                        }
                    }
                }
            }
        }

        // 오선 영역 구분선
        drawLine(color = Color(0xFF1E2235),
            start = Offset(0f, staffH), end = Offset(vw, staffH), strokeWidth = 1f)

        // ══════════════════════════════════════════════════════
        // 하단 60%: 2옥타브 피아노롤 (C3~B4)
        // ══════════════════════════════════════════════════════
        val rollTop = staffH + 1f
        val rollH   = vh - rollTop
        val rowH    = rollH / ROLL_NOTES.size   // 각 음 행 높이

        // 행 배경 그리기
        ROLL_NOTES.forEachIndexed { i, note ->
            val y       = rollTop + i * rowH
            val isBlack = note in BLACK_NOTES
            drawRect(
                color     = if (isBlack) Color(0xFF0A0C15) else Color(0xFF111420),
                topLeft   = Offset(0f, y),
                size      = Size(vw, rowH)
            )
            // C 음에 구분선 (옥타브 경계)
            if (note == "C4" || note == "C3") {
                drawLine(
                    color = Color(0xFF2A3060),
                    start = Offset(0f, y),
                    end   = Offset(vw, y),
                    strokeWidth = 1.5f
                )
            }
        }

        // 음 이름 라벨 (C3, C4, C5)
        val labelNotes = setOf("C4", "C3")
        ROLL_NOTES.forEachIndexed { i, note ->
            if (note in labelNotes) {
                val y = rollTop + i * rowH
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color     = 0xFF3A4580.toInt()
                        textSize  = (rowH * 0.85f).coerceIn(7f, 11f)
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(note, 3f, y + rowH * 0.78f, paint)
                }
            }
        }

        // 음표 블록 그리기
        var bx = 0f
        song.steps.forEachIndexed { idx, step ->
            val noteW   = step.duration * BW
            val sx      = bx + scrollX
            bx += noteW

            if (sx + noteW < 0 || sx > vw) return@forEachIndexed

            val isPast    = idx < stepIndex
            val isCurrent = idx == stepIndex

            step.keys.forEach { note ->
                val rowIdx = ROLL_NOTES.indexOf(note)
                if (rowIdx < 0) return@forEach   // 범위 밖 음(C5↑)은 오선 영역에서만 표시

                val color = when {
                    isPast    -> Color(0xFF252840).copy(alpha = 0.7f)
                    isCurrent -> if (playMode == PlayMode.AUTO) PianoColors.Blue else PianoColors.Emerald
                    else      -> PianoColors.Amber.copy(alpha = 0.65f)
                }
                val y = rollTop + rowIdx * rowH
                drawRoundRect(
                    color       = color,
                    topLeft     = Offset(sx + 1.5f, y + 1.5f),
                    size        = Size((noteW - 3f).coerceAtLeast(5f), rowH - 3f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // 현재 음표 밝은 상단 강조
                if (isCurrent) {
                    drawRoundRect(
                        color       = color.copy(alpha = 1f),
                        topLeft     = Offset(sx + 1.5f, y + 1.5f),
                        size        = Size((noteW - 3f).coerceAtLeast(5f), 2.5f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }

        // ══════════════════════════════════════════════════════
        // 중앙 커서 라인 (전체 높이)
        // ══════════════════════════════════════════════════════
        drawLine(color = PianoColors.Amber.copy(alpha = 0.15f),
            start = Offset(vw / 2f, 0f), end = Offset(vw / 2f, vh), strokeWidth = 18f)
        drawLine(color = PianoColors.Amber.copy(alpha = 0.55f),
            start = Offset(vw / 2f, 0f), end = Offset(vw / 2f, vh), strokeWidth = 3f)
        drawLine(color = PianoColors.Amber,
            start = Offset(vw / 2f, 0f), end = Offset(vw / 2f, vh), strokeWidth = 1.5f)
    }
}
