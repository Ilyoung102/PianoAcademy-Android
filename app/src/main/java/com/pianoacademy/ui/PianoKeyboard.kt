package com.pianoacademy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
    val xFraction: Float,   // 0.0 ~ 1.0 (건반 영역 내 위치)
    val widthFraction: Float
)

fun computeKeyLayouts(isLandscape: Boolean): List<KeyLayout> {
    val whites = PIANO_NOTES.filter { it.type == KeyType.WHITE }
    val totalWhite = whites.size
    val ww = 1f / totalWhite
    val bkScale = if (isLandscape) 0.85f else 1.0f
    var wi = 0

    return PIANO_NOTES.map { note ->
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
    onNoteOn: (String) -> Unit,
    onNoteOff: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyLayouts = remember(isLandscape) { computeKeyLayouts(isLandscape) }
    val whites = remember(keyLayouts) { keyLayouts.filter { it.type == KeyType.WHITE } }
    val blacks = remember(keyLayouts) { keyLayouts.filter { it.type == KeyType.BLACK } }

    // 현재 터치 포인터 → 음 매핑
    val pointerNotes = remember { mutableStateMapOf<Long, String>() }
    var keyboardWidthPx by remember { mutableStateOf(0f) }
    var keyboardHeightPx by remember { mutableStateOf(0f) }

    fun noteAtPosition(offset: Offset): String? {
        if (keyboardWidthPx == 0f) return null
        val relX = offset.x / keyboardWidthPx
        val relY = offset.y / keyboardHeightPx

        // 검은 건반 먼저 체크 (위에 있으므로)
        val blackKey = blacks.firstOrNull { k ->
            relX >= k.xFraction && relX <= k.xFraction + k.widthFraction && relY < 0.62f
        }
        if (blackKey != null) return blackKey.note

        // 흰 건반 체크
        return whites.firstOrNull { k ->
            relX >= k.xFraction && relX <= k.xFraction + k.widthFraction
        }?.note
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
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
                                    // 새 터치 시작
                                    val note = noteAtPosition(change.position)
                                    if (note != null) {
                                        pointerNotes[pointerId] = note
                                        onNoteOn(note)
                                    }
                                    change.consume()
                                }
                                change.pressed && pointerNotes.containsKey(pointerId) -> {
                                    // 드래그 이동
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
                                    // 터치 종료
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
            val isActive = activeKeys.contains(key.note)
            val isHighlight = highlightKeys.contains(key.note)
            val isWrong = wrongKeys.contains(key.note)
            val isCorrect = correctKeys.contains(key.note)

            val bgColor = when {
                isWrong   -> PianoColors.KeyWrong
                isCorrect -> PianoColors.KeyCorrect
                isActive  -> PianoColors.WhiteKeyPress
                isHighlight -> PianoColors.KeyHighlight.copy(alpha = 0.4f)
                else -> PianoColors.WhiteKey
            }
            val borderColor = if (isHighlight && !isActive) PianoColors.KeyHighlight else Color(0xFFCCCCCC)

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(key.widthFraction)
                    .offset(x = with(LocalDensity.current) {
                        (key.xFraction * keyboardWidthPx).toDp()
                    })
                    .padding(horizontal = 0.5.dp)
                    .background(bgColor, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .border(0.5.dp, borderColor, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (showNoteNames || isHighlight) {
                    Text(
                        text = getKoreanName(key.note),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isWrong || isCorrect || isActive -> Color.White
                            isHighlight -> PianoColors.Amber
                            else -> Color(0xFF666666)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }

        // ── 검은 건반 ─────────────────────────────────────────
        blacks.forEach { key ->
            val isActive = activeKeys.contains(key.note)
            val isHighlight = highlightKeys.contains(key.note)
            val isWrong = wrongKeys.contains(key.note)
            val isCorrect = correctKeys.contains(key.note)

            val bgColor = when {
                isWrong   -> PianoColors.KeyWrong
                isCorrect -> PianoColors.KeyCorrect
                isActive  -> PianoColors.BlackKeyPress
                isHighlight -> PianoColors.KeyHighlight.copy(alpha = 0.7f)
                else -> PianoColors.BlackKey
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight(0.62f)
                    .fillMaxWidth(key.widthFraction)
                    .offset(x = with(LocalDensity.current) {
                        (key.xFraction * keyboardWidthPx).toDp()
                    })
                    .background(bgColor, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
            )
        }
    }
}
