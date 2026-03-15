package com.pianoacademy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import com.pianoacademy.data.*
import com.pianoacademy.ui.theme.PianoColors
import com.pianoacademy.viewmodel.FallingMode
import com.pianoacademy.viewmodel.PlayMode

@Composable
fun FallingNotesView(
    song: Song,
    stepIndex: Int,
    playMode: PlayMode,
    fallingMode: FallingMode,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val keyLayouts = remember(isLandscape) { computeKeyLayouts(isLandscape) }
    val keyLayoutMap = remember(keyLayouts) { keyLayouts.associateBy { it.note } }

    // Beat-based timeline (not pixel-based)
    data class FallingNote(
        val note: String,
        val topOffsetBeats: Float,   // cumulative beat offset from song start
        val durationBeats: Float,    // note duration in beats
        val stepIdx: Int
    )

    val timeline: List<List<FallingNote>> = remember(song) {
        var beatCur = 0f
        song.steps.mapIndexed { idx, step ->
            val beatTop = beatCur
            beatCur += step.duration
            step.keys.map { note ->
                FallingNote(note, beatTop, step.duration, idx)
            }
        }
    }

    // Current step's beat offset (for scroll anchor)
    val currentBeat = remember(timeline, stepIndex) {
        timeline.getOrNull(stepIndex)?.firstOrNull()?.topOffsetBeats ?: 0f
    }

    Box(modifier = modifier.background(Color(0xFF0B0D17))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewW = size.width
            val viewH = size.height

            // Pixels per beat — scaled to view height (show ~5-6 beats at once)
            val bvPx = viewH / 5.5f

            val hitLinePx = if (fallingMode == FallingMode.DOWN) viewH * 0.85f else viewH * 0.15f

            // Column guide lines (left edge of each white key) - very subtle
            keyLayouts.filter { it.type == KeyType.WHITE }.forEach { k ->
                drawLine(
                    color = Color(0xFF181A2C),
                    start = Offset(k.xFraction * viewW, 0f),
                    end = Offset(k.xFraction * viewW, viewH),
                    strokeWidth = 0.4f
                )
            }

            // Hit line (amber)
            drawLine(
                color = PianoColors.Amber.copy(alpha = 0.85f),
                start = Offset(0f, hitLinePx),
                end = Offset(viewW, hitLinePx),
                strokeWidth = 2.5f
            )

            // Note blocks
            timeline.forEachIndexed { stepIdx, notes ->
                val isPast    = stepIdx < stepIndex
                val isCurrent = stepIdx == stepIndex

                notes.forEach { fn ->
                    val kl = keyLayoutMap[fn.note] ?: return@forEach

                    val blockColor = when {
                        isPast    -> Color(0xFF2D3342).copy(alpha = 0.4f)
                        isCurrent -> if (playMode == PlayMode.AUTO) PianoColors.Blue
                                     else PianoColors.Emerald
                        else      -> PianoColors.Amber.copy(alpha = 0.90f)
                    }

                    // Correct vertical position using actual beat offsets
                    val deltaBeat = fn.topOffsetBeats - currentBeat
                    val noteY = when (fallingMode) {
                        FallingMode.DOWN -> hitLinePx - deltaBeat * bvPx
                        FallingMode.UP   -> hitLinePx + deltaBeat * bvPx
                        else             -> 0f
                    }

                    val noteH = (fn.durationBeats * bvPx * 0.82f).coerceAtLeast(6f)

                    // Slight inset so bars don't touch key borders
                    val inset = viewW * 0.003f
                    val x = kl.xFraction * viewW + inset
                    val w = kl.widthFraction * viewW - inset * 2f

                    drawRoundRect(
                        color = blockColor,
                        topLeft = Offset(x, noteY),
                        size = Size(w, noteH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Bright top edge for current note
                    if (isCurrent) {
                        drawRoundRect(
                            color = blockColor.copy(alpha = 1f),
                            topLeft = Offset(x, noteY),
                            size = Size(w, 3f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }
        }

        // Song + mode label
        Text(
            text = "${song.title} · ${if (playMode == PlayMode.AUTO) "▶ 재생" else if (playMode == PlayMode.PRACTICE) "🎓 혼자하기" else "✋ 따라하기"}",
            color = Color(0xFF3A4060),
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
        )
    }
}
