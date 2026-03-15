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
import androidx.compose.ui.graphics.toArgb
import com.pianoacademy.data.*
import com.pianoacademy.ui.theme.PianoColors
import com.pianoacademy.viewmodel.PlayMode

// 세로모드 피아노롤: 2옥타브 (C4~B5, 24음)
private val ROLL_NOTES_FULL = listOf(
    "B5","A#5","A5","G#5","G5","F#5","F5","E5","D#5","D5","C#5","C5",
    "B4","A#4","A4","G#4","G4","F#4","F4","E4","D#4","D4","C#4","C4"
)
private val BLACK_NOTES_FULL = setOf(
    "A#5","G#5","F#5","D#5","C#5",
    "A#4","G#4","F#4","D#4","C#4"
)

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

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E18))
    ) {
        val BW  = 110f
        val vw  = size.width
        val vh  = size.height
        val scrollX = vw / 2f - animBeat * BW

        // ══════════════════════════════════════════════════════
        // 오선 악보 영역
        // 가로: 78% (아래 노트큐 바 22%), 세로: 100%
        // ══════════════════════════════════════════════════════
        val staffRatio = if (isLandscape) 0.78f else 1.0f
        val staffH = vh * staffRatio

        // SS: C3(-7)~B5(+13) = 20step + 양쪽 여백 → staffH에 맞게 적응
        // 가로모드는 전체 음역(3옥타브)을 표시, 세로모드는 고정값
        val SS = if (isLandscape) (staffH / 23f).coerceIn(4.5f, 9f) else 8.5f

        // 기준점:
        //   가로 - G4(offset=4) 기준, C3~B5 모두 보임
        //     C3(-7): centerY + 7*SS (아래)
        //     B5(+13): centerY - 13*SS (위)
        //   세로 - 전체 범위 중앙
        val centerY = if (isLandscape) staffH * 0.62f else staffH * 0.52f

        // ── 배경 가이드 라인 (매우 연함) ──
        if (isLandscape) {
            // 가로모드: C3(-7)~B5(+13) 범위의 모든 반음 위치에 가이드
            for (off in -8..14) {
                drawLine(
                    color = Color(0xFF1A1D2C),
                    start = Offset(0f, centerY - off * SS),
                    end   = Offset(vw, centerY - off * SS),
                    strokeWidth = 0.3f
                )
            }
        } else {
            for (li in -5..5) {
                drawLine(
                    color = Color(0xFF191C28),
                    start = Offset(0f, centerY - li * SS * 2),
                    end   = Offset(vw, centerY - li * SS * 2),
                    strokeWidth = 0.5f
                )
            }
        }

        // ── 오선 그리기 ──
        if (isLandscape) {
            // 확장 보조선: D3(-6), F3(-4), A3(-2), C4(0), A5(12)
            // 이 선들이 없으면 해당 음표가 허공에 떠 보임
            for (off in listOf(-6, -4, -2, 0, 12)) {
                drawLine(
                    color = Color(0xFF343870),
                    start = Offset(0f, centerY - off * SS),
                    end   = Offset(vw, centerY - off * SS),
                    strokeWidth = 1.0f
                )
            }
            // 표준 트레블 클레프 5선: E4(2), G4(4), B4(6), D5(8), F5(10) — 밝고 선명하게
            for (off in listOf(2, 4, 6, 8, 10)) {
                drawLine(
                    color = Color(0xFF5560A0),
                    start = Offset(0f, centerY - off * SS),
                    end   = Offset(vw, centerY - off * SS),
                    strokeWidth = 1.6f
                )
            }
        } else {
            // 세로 모드: 표준 5선
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

        // ── 음표 그리기 ──
        var xAcc = 0f
        song.steps.forEachIndexed { idx, step ->
            val noteW = step.duration * BW
            val sx    = xAcc + scrollX
            val cx    = sx + noteW / 2f
            xAcc += noteW

            if (cx < -noteW || cx > vw + noteW) return@forEachIndexed

            val isPast    = idx < stepIndex
            val isCurrent = idx == stepIndex

            val noteColor = when {
                isPast    -> Color(0xFF2A2E42)
                isCurrent -> if (playMode == PlayMode.AUTO) PianoColors.Blue else PianoColors.Emerald
                else      -> Color(0xFF505880)
            }

            step.keys.forEach { note ->
                val offset = NOTE_OFFSETS[note] ?: 0
                val noteY  = centerY - offset * SS

                // 임시표 (#) 기호
                if (note.contains("#")) {
                    drawLine(color = noteColor,
                        start = Offset(cx - 18f, noteY - 9f),
                        end   = Offset(cx - 18f, noteY + 6f), strokeWidth = 1.5f)
                    drawLine(color = noteColor,
                        start = Offset(cx - 21f, noteY - 5f),
                        end   = Offset(cx - 14f, noteY - 5f), strokeWidth = 1.1f)
                    drawLine(color = noteColor,
                        start = Offset(cx - 21f, noteY + 1f),
                        end   = Offset(cx - 14f, noteY + 1f), strokeWidth = 1.1f)
                }

                // C4 / C5 덧줄 (ledger line)
                if (note == "C4" || note == "C5") {
                    drawLine(color = noteColor.copy(alpha = 0.7f),
                        start = Offset(cx - 13f, noteY),
                        end   = Offset(cx + 13f, noteY), strokeWidth = 1.4f)
                }

                // 음표 머리 (SS에 비례 — 더 크고 선명하게)
                val isWhole = step.duration >= 4f
                val isHalf  = step.duration >= 2f && step.duration < 4f
                val headW   = (SS * 1.35f).coerceIn(6f, 12f)
                val headH   = (SS * 0.95f).coerceIn(4f, 9f)

                if (isWhole || isHalf) {
                    drawOval(color = noteColor,
                        topLeft = Offset(cx - headW, noteY - headH),
                        size = Size(headW * 2f, headH * 2f))
                    drawOval(color = Color(0xFF0C0E18),
                        topLeft = Offset(cx - headW * 0.5f, noteY - headH * 0.5f),
                        size = Size(headW, headH))
                } else {
                    drawOval(color = noteColor,
                        topLeft = Offset(cx - headW, noteY - headH),
                        size = Size(headW * 2f, headH * 2f))
                }

                // 음표 줄기 (stem) — B4(offset=6) 기준으로 위아래 방향 결정
                if (!isWhole) {
                    val stemUp  = offset <= 6
                    val stemLen = (SS * 3.5f).coerceIn(18f, 32f)
                    val tailLen = (SS * 1.4f).coerceIn(10f, 16f)
                    if (stemUp) {
                        drawLine(color = noteColor,
                            start = Offset(cx + headW - 1f, noteY),
                            end   = Offset(cx + headW - 1f, noteY - stemLen), strokeWidth = 1.5f)
                        if (step.duration < 1f) {
                            drawLine(color = noteColor,
                                start = Offset(cx + headW - 1f, noteY - stemLen),
                                end   = Offset(cx + headW + tailLen, noteY - stemLen + tailLen), strokeWidth = 2f)
                        }
                    } else {
                        drawLine(color = noteColor,
                            start = Offset(cx - headW + 1f, noteY),
                            end   = Offset(cx - headW + 1f, noteY + stemLen), strokeWidth = 1.5f)
                        if (step.duration < 1f) {
                            drawLine(color = noteColor,
                                start = Offset(cx - headW + 1f, noteY + stemLen),
                                end   = Offset(cx - headW + tailLen + 1f, noteY + stemLen - tailLen), strokeWidth = 2f)
                        }
                    }
                }
            }
        }

        // ── 커서 라인 ──
        drawLine(color = PianoColors.Amber.copy(alpha = 0.12f),
            start = Offset(vw / 2f, 0f), end = Offset(vw / 2f, staffH), strokeWidth = 22f)
        drawLine(color = PianoColors.Amber.copy(alpha = 0.6f),
            start = Offset(vw / 2f, 0f), end = Offset(vw / 2f, staffH), strokeWidth = 2.5f)
        drawLine(color = PianoColors.Amber,
            start = Offset(vw / 2f, 0f), end = Offset(vw / 2f, staffH), strokeWidth = 1.2f)

        // ══════════════════════════════════════════════════════
        // 하단: 가로모드=노트큐 바 / 세로모드=피아노롤
        // ══════════════════════════════════════════════════════
        val bottomTop = staffH + 1f
        val bottomH   = vh - bottomTop

        drawLine(color = Color(0xFF252840),
            start = Offset(0f, staffH), end = Offset(vw, staffH), strokeWidth = 1f)

        if (isLandscape) {
            // ── 노트 큐 바 (HTML 스타일) ──
            drawRect(Color(0xFF090B14), Offset(0f, bottomTop), Size(vw, bottomH))

            val cardPad = 2.5f
            val cardH   = bottomH - cardPad * 2

            var qx = 0f
            song.steps.forEachIndexed { idx, step ->
                val noteW = step.duration * BW
                val sx    = qx + scrollX
                qx += noteW

                if (sx + noteW < -10f || sx > vw + 10f) return@forEachIndexed

                val isPast    = idx < stepIndex
                val isCurrent = idx == stepIndex

                val bgColor = when {
                    isCurrent -> if (playMode == PlayMode.AUTO) PianoColors.Blue else PianoColors.Emerald
                    isPast    -> Color(0xFF111420)
                    else      -> Color(0xFF181B2C)
                }
                val textAlpha = when {
                    isCurrent -> 1.0f
                    isPast    -> 0.3f
                    else      -> 0.65f
                }
                val textArgb = when {
                    isCurrent -> Color.White.toArgb()
                    isPast    -> Color(0xFF4A4E6A).toArgb()
                    else      -> Color(0xFF8890B8).toArgb()
                }

                // 카드 배경
                drawRoundRect(
                    color       = bgColor,
                    topLeft     = Offset(sx + cardPad, bottomTop + cardPad),
                    size        = Size((noteW - cardPad * 2).coerceAtLeast(4f), cardH),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // 텍스트 (너비가 충분할 때만)
                if (noteW > 22f) {
                    val firstNote = step.keys.firstOrNull() ?: return@forEachIndexed
                    val regex = Regex("^([A-G])(#?)([0-9])$")
                    val m = regex.find(firstNote) ?: return@forEachIndexed
                    val korName = KOREAN_NAMES[m.groupValues[1]] ?: m.groupValues[1]
                    val sharp   = if (m.groupValues[2].isNotEmpty()) "#" else ""
                    val oct     = m.groupValues[3]
                    val label   = "$korName$sharp$oct ♩"

                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color     = textArgb
                            textSize  = (cardH * 0.42f).coerceIn(9f, 15f)
                            isAntiAlias = true
                            textAlign   = android.graphics.Paint.Align.CENTER
                            typeface    = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        canvas.nativeCanvas.drawText(
                            label,
                            sx + noteW / 2f,
                            bottomTop + bottomH * 0.63f,
                            paint
                        )
                    }
                }
            }

            // 큐 커서 라인
            drawLine(color = PianoColors.Amber.copy(alpha = 0.5f),
                start = Offset(vw / 2f, bottomTop + 3f),
                end   = Offset(vw / 2f, vh - 3f),
                strokeWidth = 2f)

        } else {
            // ── 세로모드: 피아노롤 ──
            drawRect(Color(0xFF0B0D17), Offset(0f, bottomTop), Size(vw, bottomH))

            val rowH = bottomH / ROLL_NOTES_FULL.size

            // 행 구분선 + 옥타브 경계
            ROLL_NOTES_FULL.forEachIndexed { i, note ->
                val y = bottomTop + i * rowH
                drawLine(color = Color(0xFF16192A),
                    start = Offset(0f, y), end = Offset(vw, y), strokeWidth = 0.5f)
                if (note == "C5" || note == "C4") {
                    drawLine(color = Color(0xFF2D3870),
                        start = Offset(0f, y), end = Offset(vw, y), strokeWidth = 1.4f)
                }
            }

            // 옥타브 레이블
            ROLL_NOTES_FULL.forEachIndexed { i, note ->
                if (note == "C5" || note == "C4") {
                    val y = bottomTop + i * rowH
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = 0xFF3A4580.toInt()
                            textSize = (rowH * 0.85f).coerceIn(6f, 11f)
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawText(note, 3f, y + rowH * 0.78f, paint)
                    }
                }
            }

            // 음표 블록
            var bx = 0f
            song.steps.forEachIndexed { idx, step ->
                val noteW = step.duration * BW
                val sx    = bx + scrollX
                bx += noteW

                if (sx + noteW < 0 || sx > vw) return@forEachIndexed

                val isPast    = idx < stepIndex
                val isCurrent = idx == stepIndex

                step.keys.forEach { note ->
                    val rowIdx = ROLL_NOTES_FULL.indexOf(note)
                    if (rowIdx < 0) return@forEach

                    val color = when {
                        isPast    -> Color(0xFF252840).copy(alpha = 0.5f)
                        isCurrent -> if (playMode == PlayMode.AUTO) PianoColors.Blue else PianoColors.Emerald
                        else      -> PianoColors.Amber.copy(alpha = 0.85f)
                    }
                    val y = bottomTop + rowIdx * rowH
                    drawRoundRect(
                        color        = color,
                        topLeft      = Offset(sx + 1.5f, y + 1.5f),
                        size         = Size((noteW - 3f).coerceAtLeast(5f), rowH - 3f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    if (isCurrent) {
                        drawRoundRect(
                            color        = color.copy(alpha = 1f),
                            topLeft      = Offset(sx + 1.5f, y + 1.5f),
                            size         = Size((noteW - 3f).coerceAtLeast(5f), 2.5f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }
                }
            }

            // 세로모드 커서 라인 (롤 영역)
            drawLine(color = PianoColors.Amber.copy(alpha = 0.15f),
                start = Offset(vw / 2f, bottomTop), end = Offset(vw / 2f, vh), strokeWidth = 18f)
            drawLine(color = PianoColors.Amber.copy(alpha = 0.55f),
                start = Offset(vw / 2f, bottomTop), end = Offset(vw / 2f, vh), strokeWidth = 3f)
            drawLine(color = PianoColors.Amber,
                start = Offset(vw / 2f, bottomTop), end = Offset(vw / 2f, vh), strokeWidth = 1.5f)
        }
    }
}
