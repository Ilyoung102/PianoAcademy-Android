package com.pianoacademy.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── 앱 색상 ───────────────────────────────────────────────────
object PianoColors {
    val Background     = Color(0xFF0F1117)
    val Surface        = Color(0xFF1A1D27)
    val SurfaceVariant = Color(0xFF22253A)
    val Border         = Color(0xFF2A2D3E)
    val TextPrimary    = Color(0xFFE2E8F0)
    val TextSecondary  = Color(0xFF94A3B8)
    val TextMuted      = Color(0xFF4A5568)
    val Amber          = Color(0xFFF59E0B)
    val Blue           = Color(0xFF3B82F6)
    val Emerald        = Color(0xFF10B981)
    val Purple         = Color(0xFFA855F7)
    val Teal           = Color(0xFF14B8A6)
    val Violet         = Color(0xFF8B5CF6)
    val Rose           = Color(0xFFF43F5E)
    val WhiteKey       = Color(0xFFF8F9FA)
    val WhiteKeyPress  = Color(0xFFBFD7FF)
    val BlackKey       = Color(0xFF1A1A2E)
    val BlackKeyPress  = Color(0xFF2D4A8A)
    val KeyHighlight   = Color(0xFFF59E0B)  // 다음 음 힌트
    val KeyCorrect     = Color(0xFF10B981)
    val KeyWrong       = Color(0xFFF43F5E)
    val NoteActive     = Color(0xFF3B82F6)
    val NoteQueued     = Color(0xFFF59E0B)
    val NotePassed     = Color(0xFF374151)
}

private val DarkColorScheme = darkColorScheme(
    primary        = PianoColors.Amber,
    onPrimary      = Color(0xFF1A1A1A),
    secondary      = PianoColors.Blue,
    onSecondary    = Color.White,
    background     = PianoColors.Background,
    onBackground   = PianoColors.TextPrimary,
    surface        = PianoColors.Surface,
    onSurface      = PianoColors.TextPrimary,
    surfaceVariant = PianoColors.SurfaceVariant,
    onSurfaceVariant = PianoColors.TextSecondary,
    outline        = PianoColors.Border
)

@Composable
fun PianoAcademyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
