package com.pianoacademy.ui

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.*
import com.pianoacademy.data.*
import com.pianoacademy.ui.theme.PianoColors
import com.pianoacademy.viewmodel.FallingMode
import com.pianoacademy.viewmodel.PlayMode

// ── 폭포수 뷰 (Falling Notes) ─────────────────────────────────
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

    // 노트 블록 높이 단위 (픽셀 당 비트)
    val bv = 70f

    // 전체 타임라인 계산
    data class FallingNote(
        val note: String,
        val topOffset: Float,  // bv 단위
        val heightPx: Float,
        val stepIdx: Int
    )

    val timeline: List<List<FallingNote>> = remember(song) {
        var cur = 0f
        song.steps.mapIndexed { idx, step ->
            val top = cur
            cur += step.duration * bv
            step.keys.map { note ->
                FallingNote(note, top, (step.duration * bv * 0.41f).coerceAtLeast(4f), idx)
            }
        }
    }

    val totalHeight = remember(song) {
        song.steps.sumOf { it.duration.toDouble() }.toFloat() * bv
    }

    Box(modifier = modifier.background(Color(0xFF15171E))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewW = size.width
            val viewH = size.height
            val hitLinePx = if (fallingMode == FallingMode.DOWN) viewH - 10f else 90f

            // ── 컬럼 가이드라인 ──────────────────────────────
            keyLayouts.filter { it.type == KeyType.WHITE }.forEach { k ->
                drawLine(
                    color = Color(0xFF2A2D3E),
                    start = Offset(k.xFraction * viewW, 0f),
                    end = Offset(k.xFraction * viewW, viewH),
                    strokeWidth = 0.5f
                )
            }

            // ── 히트라인 ─────────────────────────────────────
            drawLine(
                color = PianoColors.Amber.copy(alpha = 0.8f),
                start = Offset(0f, hitLinePx),
                end = Offset(viewW, hitLinePx),
                strokeWidth = 2f
            )

            // ── 노트 블록 그리기 ─────────────────────────────
            timeline.forEachIndexed { stepIdx, notes ->
                val isPast    = stepIdx < stepIndex
                val isCurrent = stepIdx == stepIndex

                notes.forEach { fn ->
                    val kl = keyLayoutMap[fn.note] ?: return@forEach
                    val isBlack = kl.type == KeyType.BLACK

                    val blockColor = when {
                        isPast -> Color(0xFF374151).copy(alpha = 0.5f)
                        isCurrent -> if (playMode == PlayMode.AUTO) PianoColors.Blue
                                     else PianoColors.Emerald
                        else -> PianoColors.Amber.copy(alpha = 0.8f)
                    }

                    // 폭포수 위치 계산
                    val noteY = when (fallingMode) {
                        FallingMode.DOWN -> {
                            val offset = totalHeight - fn.topOffset
                            hitLinePx - offset + (stepIndex * bv)
                        }
                        FallingMode.UP -> {
                            hitLinePx + fn.topOffset - (stepIndex * bv)
                        }
                        else -> 0f
                    }

                    val x = kl.xFraction * viewW + viewW * 0.003f
                    val w = kl.widthFraction * viewW - viewW * 0.006f

                    drawRoundRect(
                        color = blockColor,
                        topLeft = Offset(x, noteY),
                        size = Size(w, fn.heightPx),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }

        // 곡명 + 모드 표시
        Text(
            text = "${song.title} · ${if (playMode == PlayMode.AUTO) "▶ 재생" else "🎓 따라하기"}",
            color = Color(0xFF4A5568),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
        )
    }
}
