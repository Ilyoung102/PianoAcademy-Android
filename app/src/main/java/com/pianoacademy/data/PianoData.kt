package com.pianoacademy.data

// ── 음표 정의 ──────────────────────────────────────────────────
data class NoteKey(
    val note: String,
    val type: KeyType,   // white / black
    val frequency: Double
)

enum class KeyType { WHITE, BLACK }

// 음이름 → 주파수 (A4 = 440Hz 기준)
fun noteToFrequency(note: String): Double {
    val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val regex = Regex("^([A-G]#?)([0-9])$")
    val match = regex.find(note) ?: return 440.0
    val name = match.groupValues[1]
    val octave = match.groupValues[2].toInt()
    val semitone = noteNames.indexOf(name)
    // A4 = MIDI 69
    val midi = (octave + 1) * 12 + semitone
    return 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
}

val PIANO_NOTES: List<NoteKey> = listOf(
    NoteKey("C3",  KeyType.WHITE,  noteToFrequency("C3")),
    NoteKey("C#3", KeyType.BLACK,  noteToFrequency("C#3")),
    NoteKey("D3",  KeyType.WHITE,  noteToFrequency("D3")),
    NoteKey("D#3", KeyType.BLACK,  noteToFrequency("D#3")),
    NoteKey("E3",  KeyType.WHITE,  noteToFrequency("E3")),
    NoteKey("F3",  KeyType.WHITE,  noteToFrequency("F3")),
    NoteKey("F#3", KeyType.BLACK,  noteToFrequency("F#3")),
    NoteKey("G3",  KeyType.WHITE,  noteToFrequency("G3")),
    NoteKey("G#3", KeyType.BLACK,  noteToFrequency("G#3")),
    NoteKey("A3",  KeyType.WHITE,  noteToFrequency("A3")),
    NoteKey("A#3", KeyType.BLACK,  noteToFrequency("A#3")),
    NoteKey("B3",  KeyType.WHITE,  noteToFrequency("B3")),
    NoteKey("C4",  KeyType.WHITE,  noteToFrequency("C4")),
    NoteKey("C#4", KeyType.BLACK,  noteToFrequency("C#4")),
    NoteKey("D4",  KeyType.WHITE,  noteToFrequency("D4")),
    NoteKey("D#4", KeyType.BLACK,  noteToFrequency("D#4")),
    NoteKey("E4",  KeyType.WHITE,  noteToFrequency("E4")),
    NoteKey("F4",  KeyType.WHITE,  noteToFrequency("F4")),
    NoteKey("F#4", KeyType.BLACK,  noteToFrequency("F#4")),
    NoteKey("G4",  KeyType.WHITE,  noteToFrequency("G4")),
    NoteKey("G#4", KeyType.BLACK,  noteToFrequency("G#4")),
    NoteKey("A4",  KeyType.WHITE,  noteToFrequency("A4")),
    NoteKey("A#4", KeyType.BLACK,  noteToFrequency("A#4")),
    NoteKey("B4",  KeyType.WHITE,  noteToFrequency("B4")),
    NoteKey("C5",  KeyType.WHITE,  noteToFrequency("C5")),
    NoteKey("C#5", KeyType.BLACK,  noteToFrequency("C#5")),
    NoteKey("D5",  KeyType.WHITE,  noteToFrequency("D5")),
    NoteKey("D#5", KeyType.BLACK,  noteToFrequency("D#5")),
    NoteKey("E5",  KeyType.WHITE,  noteToFrequency("E5")),
    NoteKey("F5",  KeyType.WHITE,  noteToFrequency("F5")),
    NoteKey("F#5", KeyType.BLACK,  noteToFrequency("F#5")),
    NoteKey("G5",  KeyType.WHITE,  noteToFrequency("G5")),
    NoteKey("G#5", KeyType.BLACK,  noteToFrequency("G#5")),
    NoteKey("A5",  KeyType.WHITE,  noteToFrequency("A5")),
    NoteKey("A#5", KeyType.BLACK,  noteToFrequency("A#5")),
    NoteKey("B5",  KeyType.WHITE,  noteToFrequency("B5")),
)

val NOTE_MAP: Map<String, NoteKey> = PIANO_NOTES.associateBy { it.note }

// 건반 옥타브 이동을 위한 동적 노트 생성 (fromOctave: 시작 옥타브, 3옥타브 범위)
fun generatePianoNotes(fromOctave: Int): List<NoteKey> {
    val names  = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
    val types  = listOf(KeyType.WHITE,KeyType.BLACK,KeyType.WHITE,KeyType.BLACK,KeyType.WHITE,
                        KeyType.WHITE,KeyType.BLACK,KeyType.WHITE,KeyType.BLACK,KeyType.WHITE,
                        KeyType.BLACK,KeyType.WHITE)
    return buildList {
        for (oct in fromOctave until fromOctave + 3) {
            names.forEachIndexed { i, n ->
                add(NoteKey("$n$oct", types[i], noteToFrequency("$n$oct")))
            }
        }
    }
}

val KOREAN_NAMES = mapOf(
    "C" to "도", "D" to "레", "E" to "미", "F" to "파",
    "G" to "솔", "A" to "라", "B" to "시"
)

fun getKoreanName(note: String): String {
    val regex = Regex("^([A-G])(#?)([0-9])$")
    val match = regex.find(note) ?: return note
    val name = match.groupValues[1]
    val sharp = match.groupValues[2]
    return "${KOREAN_NAMES[name] ?: name}${if (sharp.isNotEmpty()) "♯" else ""}"
}

// ── 악보 오프셋 (악보 뷰 세로 위치) ───────────────────────────
val NOTE_OFFSETS: Map<String, Int> = mapOf(
    "B5" to 13, "A#5" to 12, "A5" to 12, "G#5" to 11, "G5" to 11,
    "F#5" to 10, "F5" to 10, "E5" to 9, "D#5" to 8, "D5" to 8,
    "C#5" to 7, "C5" to 7, "B4" to 6, "A#4" to 5, "A4" to 5,
    "G#4" to 4, "G4" to 4, "F#4" to 3, "F4" to 3, "E4" to 2,
    "D#4" to 1, "D4" to 1, "C#4" to 0, "C4" to 0, "B3" to -1,
    "A#3" to -2, "A3" to -2, "G#3" to -3, "G3" to -3, "F#3" to -4,
    "F3" to -4, "E3" to -5, "D#3" to -6, "D3" to -6, "C#3" to -7, "C3" to -7
)

// ── 음표 데이터 파싱 ────────────────────────────────────────────
data class SongStep(val keys: List<String>, val duration: Float)

fun parseSongSteps(raw: String): List<SongStep> {
    return raw.split(",").mapNotNull { token ->
        val t = token.trim()
        if (t.isEmpty()) return@mapNotNull null
        val colonIdx = t.lastIndexOf(':')
        if (colonIdx < 0) return@mapNotNull null
        val notePart = t.substring(0, colonIdx)
        val durPart = t.substring(colonIdx + 1)
        val keys = notePart.split("+")
        val duration = durPart.toFloatOrNull() ?: return@mapNotNull null
        SongStep(keys, duration)
    }
}

// ── 곡 데이터 ──────────────────────────────────────────────────
data class Song(
    val id: String,
    val level: Int,
    val title: String,
    val tempo: Int,
    val steps: List<SongStep>
)

fun getNoteSymbol(d: Float): String = when {
    d >= 4f  -> "𝅝"
    d >= 3f  -> "𝅗𝅥."
    d >= 2f  -> "𝅗𝅥"
    d >= 1.5f -> "♩."
    d >= 1f  -> "♩"
    d >= 0.75f -> "♪."
    d >= 0.5f -> "♪"
    else -> "♬"
}
