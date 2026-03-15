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

// 피아노롤 음표 목록
// 세로모드: 2옥타브 (C4~B5, 24음)
private val ROLL_NOTES_FULL = listOf(
    "B5","A#5","A5","G#5","G5","F#5","F5","E5","D#5","D5","C#5","C5",
    "B4","A#4","A4","G#4","G4","F#4","F4","E4","D#4","D4","C#4","C4"
)
// 가로모드: 1옥타브 (C5~B5, 12음) — 악보 영역이 크므로 롤은 1옥타브만
private val ROLL_NOTES_COMPACT = listOf(
    "B5","A#5","A5","G#5","G5","F#5","F5","E5","D#5","D5","C#5","C5"
)
private val BLACK_NOTES_FULL = setOf(
    "A#5","G#5","F#5","D#5","C#5",
    "A#4","G#4","F#4","D#4","C#4"
)
private val BLACK_NOTES_COMPACT = setOf("A#5","G#5","F#5","D#5","C#5")

@Composable
fun SheetMusicView(
    song: Song,
    stepIndex: Int,
    playMode: PlayMode,
    isPlaying: Boolean = false,
    tempoMultiplier: Float = 1.0f,
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val targetBeat = remember(song, stepIndex) {
        song.steps.take(stepIndex).sumOf { it.duration.toDouble() }.toFloat()
    }

    val animDurationMs = remember(song, stepIndex, tempoMultiplier) {
        if (!isPlaying) 220
        else {
            val d = song.steps.getOrNull(stepIndex)?.duration ?: 1f
            (d * 60000f / song.tempo / tempoMultiplier).toInt().coerceIn(60, 3000)
        }
    }

    val animBeat by animateFloatAsState(
        targetValue = targetBeat,
        animationSpec = tween(durationMillis = animDurationMs, easing = LinearEasing),
        label = "sheetScroll"
    )

    // 가로/세로 음표 목록 선택
    val rollNotes  = if (isLandscape) ROLL_NOTES_COMPACT  else ROLL_NOTES_FULL
    val blackNotes = if (isLandscape) BLACK_NOTES_COMPACT else BLACK_NOTES_FULL

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A))
    ) {
        val BW   = 110f
        val vw   = size.width
        val vh   = size.height

        val scrollX = vw / 2f - animBeat * BW

        // ══════════════════════════════════════════════════════
        // 상단: 오선 악보
        // 가로=50% (2옥타브 표시), 세로=38%
        // ══════════════════════════════════════════════════════
        val staffRatio = if (isLandscape) 0.50f else 0.38f
        val staffH  = vh * staffRatio

        // 적응형 SS: 가로모드는 C4(0)~B5(13) = 13 steps 을 staffH 안에 담음
        val SS = if (isLandscape) (staffH / 14f).coerceIn(6f, 11f) else 8.5f

        // 기준점: 가로=아래쪽(C4가 하단), 세로=중앙
        val centerY = if (isLandscape) staffH * 0.98f else staffH * 0.52f

        if (isLandscape) {
            // ── 가로모드: 2옥타브 두 세트 악보줄 ──
            // 배경 미세 가이드 라인 (각 오프셋 위치)
            for (off in 0..14) {
                drawLine(
                    color = Color(0xFF191C28),
                    start = Offset(0f, centerY - off * SS),
                    end   = Offset(vw, centerY - off * SS),
                    strokeWidth = 0.4f
                )
            }
            // 하단 5선 (E4=2, G4=4, B4=6, D5=8, F5=10)
            for (off in listOf(2, 4, 6, 8, 10)) {
                drawLine(
                    color = Color(0xFF3A3F60),
                    start = Offset(0f, centerY - off * SS),
                    end   = Offset(vw, centerY - off * SS),
                    strokeWidth = 1.1f
                )
            }
            // 상단 5선 (E5=9, G5=11, B5=13, + 2선 확장)
            for (off in listOf(9, 11, 13)) {
                drawLine(
                    color = Color(0xFF3A3F60),
                    start = Offset(0f, centerY - off * SS),
                    end   = Offset(vw, centerY - off * SS),
                    strokeWidth = 1.1f
                )
            }
            // 옥타브 구분 표시 (C5=7 위치에 가는 구분선)
            drawLine(
                color = Color(0xFF2A3068),
                start = Offset(vw * 0.02f, centerY - 7.5f * SS),
                end   = Offset(vw * 0.98f, centerY - 7.5f * SS),
                strokeWidth = 0.6f
            )
        } else {
            // ── 세로모드: 기존 단일 5선 ──
            for (li in -5..5) {
                drawLine(
                    color = Color(0xFF191C28),
                    start = Offset(0f, centerY - li * SS * 2),
                    end   = Offset(vw, centerY - li * SS * 2),
                    strokeWidth = 0.6f
                )
            }
            for (i in -2..2) {
                drawLine(
                    color = Color(0xFF353A58),
                    start = Offset(0f, centerY - i * SS),
                    end   = Offset(vw, centerY - i * SS),
                    strokeWidth = 1.1f
                )
            }
        }

        var xAcc = 0f
        song.steps.forEachIndexed { idx, step ->
            val noteW   = step.duration * BW
            val sx      = xAcc + scrollX
            val cx      = sx + noteW / 2f
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

                if (note == "C4") {
                    drawLine(color = noteColor.copy(alpha = 0.65f),
                        start = Offset(cx - 12f, noteY), end = Offset(cx + 12f, noteY), strokeWidth = 1.4f)
                }
                if (note == "C3") {
                    for (li in 0..2) {
                        drawLine(color = noteColor.copy(alpha = 0.5f),
                            start = Offset(cx - 12f, noteY + li * SS),
                            end   = Offset(cx + 12f, noteY + li * SS), strokeWidth = 1.2f)
                    }
                }
                if (note == "C5") {
                    drawLine(color = noteColor.copy(alpha = 0.65f),
                        start = Offset(cx - 12f, noteY), end = Offset(cx + 12f, noteY), strokeWidth = 1.4f)
                }

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

                if (isWhole || isHalf) {
                    drawOval(color = noteColor, topLeft = Offset(cx - 7f, noteY - 5f), size = Size(14f, 10f))
                    drawOval(color = Color(0xFF0F111A), topLeft = Offset(cx - 4f, noteY - 3f), size = Size(8f, 6f))
                } else {
                    drawOval(color = noteColor, topLeft = Offset(cx - 7f, noteY - 5f), size = Size(14f, 10f))
                }

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

        drawLine(color = Color(0xFF1E2235),
            start = Offset(0f, staffH), end = Offset(vw, staffH), strokeWidth = 1f)

        // ══════════════════════════════════════════════════════
        // 하단: 피아노롤 (가로=1옥타브 C5~B5, 세로=2옥타브 C4~B5)
        // ══════════════════════════════════════════════════════
        val rollTop = staffH + 1f
        val rollH   = vh - rollTop
        val rowH    = rollH / rollNotes.size

        // 균일한 다크 배경 (줄무늬 없음)
        drawRect(
            color   = Color(0xFF0B0D17),
            topLeft = Offset(0f, rollTop),
            size    = Size(vw, rollH)
        )

        // 행 구분선 (매우 연함) + 옥타브 경계선
        rollNotes.forEachIndexed { i, note ->
            val y = rollTop + i * rowH
            drawLine(
                color = Color(0xFF16192A),
                start = Offset(0f, y),
                end   = Offset(vw, y),
                strokeWidth = 0.5f
            )
            if (note == "C5" || note == "C4") {
                drawLine(
                    color = Color(0xFF2D3870),
                    start = Offset(0f, y),
                    end   = Offset(vw, y),
                    strokeWidth = 1.4f
                )
            }
        }

        // 옥타브 레이블
        rollNotes.forEachIndexed { i, note ->
            if (note == "C5" || note == "C4") {
                val y = rollTop + i * rowH
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color     = 0xFF3A4580.toInt()
                        textSize  = (rowH * 0.85f).coerceIn(6f, 11f)
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(note, 3f, y + rowH * 0.78f, paint)
                }
            }
        }

        // 음표 블록
        var bx = 0f
        song.steps.forEachIndexed { idx, step ->
            val noteW   = step.duration * BW
            val sx      = bx + scrollX
            bx += noteW

            if (sx + noteW < 0 || sx > vw) return@forEachIndexed

            val isPast    = idx < stepIndex
            val isCurrent = idx == stepIndex

            step.keys.forEach { note ->
                val rowIdx = rollNotes.indexOf(note)
                if (rowIdx < 0) return@forEach

                val color = when {
                    isPast    -> Color(0xFF252840).copy(alpha = 0.5f)
                    isCurrent -> if (playMode == PlayMode.AUTO) PianoColors.Blue else PianoColors.Emerald
                    else      -> PianoColors.Amber.copy(alpha = 0.85f)
                }
                val y = rollTop + rowIdx * rowH
                drawRoundRect(
                    color       = color,
                    topLeft     = Offset(sx + 1.5f, y + 1.5f),
                    size        = Size((noteW - 3f).coerceAtLeast(5f), rowH - 3f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
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

        // 커서 라인
        drawLine(color = PianoColors.Amber.copy(alpha = 0.15f),
            start = Offset(vw / 2f, 0f), end = Offset(vw / 2f, vh), strokeWidth = 18f)
        drawLine(color = PianoColors.Amber.copy(alpha = 0.55f),
            start = Offset(vw / 2f, 0f), end = Offset(vw / 2f, vh), strokeWidth = 3f)
        drawLine(color = PianoColors.Amber,
            start = Offset(vw / 2f, 0f), end = Offset(vw / 2f, vh), strokeWidth = 1.5f)
    }
}
