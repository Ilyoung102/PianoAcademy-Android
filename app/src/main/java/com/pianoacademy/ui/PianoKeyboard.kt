package com.pianoacademy.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.pianoacademy.data.*
import com.pianoacademy.ui.theme.PianoColors

// ── 건반 배치 계산 ─────────────────────────────────────────────
data class KeyLayout(
    val note: String,
    val type: KeyType,
    val xFraction: Float,
    val widthFraction: Float
)

fun computeKeyLayouts(isLandscape: Boolean, octaveShift: Int = 0): List<KeyLayout> {
    val noteList = if (octaveShift == 0) PIANO_NOTES else generatePianoNotes(3 + octaveShift)
    val whites = noteList.filter { it.type == KeyType.WHITE }
    val totalWhite = whites.size
    val ww = 1f / totalWhite
    val bkScale = if (isLandscape) 0.85f else 1.0f
    var wi = 0

    return noteList.map { note ->
        if (note.type == KeyType.WHITE) {
            val x = wi * ww
            wi++
            KeyLayout(note.note, KeyType.WHITE, x, ww)
        } else {
            val lp = (wi.toFloat() / totalWhite)
            KeyLayout(
                note.note, KeyType.BLACK,
                lp - (1f / totalWhite) * 0.30f * bkScale,
                (1f / totalWhite) * 0.60f * bkScale
            )
        }
    }
}

// ── 건반 UI ────────────────────────────────────────────────────
@Composable
fun PianoKeyboard(
    activeKeys: Set<String>,
    highlightKeys: Set<String>,
    wrongKeys: Set<String>,
    correctKeys: Set<String>,
    showNoteNames: Boolean,
    isLandscape: Boolean,
    octaveShift: Int = 0,
    onNoteOn: (String) -> Unit,
    onNoteOff: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyLayouts = remember(isLandscape, octaveShift) { computeKeyLayouts(isLandscape, octaveShift) }
    val whites = remember(keyLayouts) { keyLayouts.filter { it.type == KeyType.WHITE } }
    val blacks = remember(keyLayouts) { keyLayouts.filter { it.type == KeyType.BLACK } }

    val pointerNotes = remember { mutableStateMapOf<Long, String>() }
    var keyboardWidthPx by remember { mutableStateOf(0f) }
    var keyboardHeightPx by remember { mutableStateOf(0f) }

    fun noteAtPosition(offset: Offset): String? {
        if (keyboardWidthPx == 0f) return null
        val relX = offset.x / keyboardWidthPx
        val relY = offset.y / keyboardHeightPx

        val blackKey = blacks.firstOrNull { k ->
            relX >= k.xFraction && relX <= k.xFraction + k.widthFraction && relY < 0.60f
        }
        if (blackKey != null) return blackKey.note

        return whites.firstOrNull { k ->
            relX >= k.xFraction && relX <= k.xFraction + k.widthFraction
        }?.note
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0C14), Color(0xFF151825))
                )
            )
            .onGloballyPositioned { coords ->
                keyboardWidthPx = coords.size.width.toFloat()
                keyboardHeightPx = coords.size.height.toFloat()
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            val pointerId = change.id.value
                            when {
                                change.pressed && !pointerNotes.containsKey(pointerId) -> {
                                    val note = noteAtPosition(change.position)
                                    if (note != null) {
                                        pointerNotes[pointerId] = note
                                        onNoteOn(note)
                                    }
                                    change.consume()
                                }
                                change.pressed && pointerNotes.containsKey(pointerId) -> {
                                    val newNote = noteAtPosition(change.position)
                                    val oldNote = pointerNotes[pointerId]
                                    if (newNote != oldNote) {
                                        if (oldNote != null) onNoteOff(oldNote)
                                        if (newNote != null) {
                                            pointerNotes[pointerId] = newNote
                                            onNoteOn(newNote)
                                        } else {
                                            pointerNotes.remove(pointerId)
                                        }
                                    }
                                    change.consume()
                                }
                                !change.pressed && pointerNotes.containsKey(pointerId) -> {
                                    val note = pointerNotes.remove(pointerId)
                                    if (note != null) onNoteOff(note)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // ── 흰 건반 ──────────────────────────────────────────
        whites.forEach { key ->
            val isActive    = activeKeys.contains(key.note)
            val isHighlight = highlightKeys.contains(key.note)
            val isWrong     = wrongKeys.contains(key.note)
            val isCorrect   = correctKeys.contains(key.note)

            val bgBrush: Brush = when {
                isWrong   -> Brush.verticalGradient(listOf(PianoColors.KeyWrong, PianoColors.KeyWrong.copy(alpha = 0.7f)))
                isCorrect -> Brush.verticalGradient(listOf(PianoColors.KeyCorrect, PianoColors.KeyCorrect.copy(alpha = 0.8f)))
                isActive  -> Brush.verticalGradient(listOf(Color(0xFFB8D4FF), PianoColors.WhiteKeyPress))
                isHighlight -> Brush.verticalGradient(
                    listOf(
                        PianoColors.Amber.copy(alpha = 0.55f),
                        PianoColors.WhiteKey.copy(alpha = 0.92f),
                        PianoColors.WhiteKey
                    )
                )
                else -> Brush.verticalGradient(
                    listOf(Color(0xFFF0F1F3), Color(0xFFE8E9EC), Color(0xFFDFE0E4))
                )
            }

            val borderColor = when {
                isHighlight && !isActive -> PianoColors.Amber
                isWrong                 -> PianoColors.KeyWrong
                isCorrect               -> PianoColors.KeyCorrect
                else                    -> Color(0xFFBBBCC0)
            }
            val borderWidth = if (isHighlight || isWrong || isCorrect) 1.5.dp else 0.5.dp

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(key.widthFraction)
                    .offset(x = with(LocalDensity.current) {
                        (key.xFraction * keyboardWidthPx).toDp()
                    })
                    .padding(horizontal = 0.5.dp)
                    .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                    .background(bgBrush)
                    .border(
                        borderWidth,
                        borderColor,
                        RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp)
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (showNoteNames || isHighlight) {
                    val noteName = getKoreanName(key.note)
                    // 힌트 원형 배지
                    if (isHighlight && !isActive && !isWrong && !isCorrect) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .size(18.dp)
                                .clip(RoundedCornerShape(50))
                                .background(PianoColors.Amber),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                noteName,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1A0A00),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = noteName,
                            fontSize = 7.sp,
                            fontWeight = if (isActive || isHighlight) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isWrong   -> Color.White
                                isCorrect -> Color.White
                                isActive  -> Color(0xFF1A3A6A)
                                else      -> Color(0xFF777A82)
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }

        // ── 검은 건반 ─────────────────────────────────────────
        blacks.forEach { key ->
            val isActive    = activeKeys.contains(key.note)
            val isHighlight = highlightKeys.contains(key.note)
            val isWrong     = wrongKeys.contains(key.note)
            val isCorrect   = correctKeys.contains(key.note)

            val bgBrush: Brush = when {
                isWrong   -> Brush.verticalGradient(listOf(PianoColors.KeyWrong, PianoColors.KeyWrong.copy(alpha = 0.6f)))
                isCorrect -> Brush.verticalGradient(listOf(PianoColors.KeyCorrect, PianoColors.KeyCorrect.copy(alpha = 0.7f)))
                isActive  -> Brush.verticalGradient(listOf(Color(0xFF2D4A8A), Color(0xFF1A2E5A)))
                isHighlight -> Brush.verticalGradient(
                    listOf(PianoColors.Amber.copy(alpha = 0.9f), Color(0xFF92400E))
                )
                else -> Brush.verticalGradient(
                    listOf(Color(0xFF1E2030), Color(0xFF141620), Color(0xFF0E101A))
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight(0.60f)
                    .fillMaxWidth(key.widthFraction)
                    .offset(x = with(LocalDensity.current) {
                        (key.xFraction * keyboardWidthPx).toDp()
                    })
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(bgBrush)
                    .then(
                        if (isHighlight && !isActive)
                            Modifier.border(
                                1.5.dp,
                                PianoColors.Amber,
                                RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                            )
                        else Modifier
                    )
            )
        }

        // ── 옥타브 마커 ─────────────────────────
        val baseOctave = 3 + octaveShift
        listOf("C${'$'}baseOctave", "C${'$'}{baseOctave+1}", "C${'$'}{baseOctave+2}").forEach { cNote ->
            val cKey = whites.firstOrNull { it.note == cNote }
            if (cKey != null && keyboardWidthPx > 0f) {
                val xDp = with(LocalDensity.current) { (cKey.xFraction * keyboardWidthPx).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = xDp, y = 2.dp)
                        .clip(RoundedCornerShape(bottomEnd = 4.dp))
                        .background(Color(0xFF1A2040).copy(alpha = 0.75f))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = cNote,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B8CC4)
                    )
                }
            }
        }
    }
}
