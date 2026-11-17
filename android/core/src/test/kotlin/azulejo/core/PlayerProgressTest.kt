package azulejo.core

import azulejo.core.progress.CalendarDay
import azulejo.core.progress.DailyResult
import azulejo.core.progress.LevelCatalog
import azulejo.core.progress.Murals
import azulejo.core.progress.PlayerProgress
import azulejo.core.progress.PlayerProgressCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerProgressTest {

    private val today = CalendarDay.of(2026, 11, 16)

    // ---------- Niveles ----------

    @Test
    fun `al empezar solo esta abierto el nivel 1`() {
        val progress = PlayerProgress()
        assertEquals(1, progress.nextLevel)
        assertTrue(progress.isUnlocked(1))
        assertFalse(progress.isUnlocked(2))
        assertFalse(progress.isUnlocked(0))
        assertFalse(progress.isUnlocked(201))
        assertEquals(0, progress.totalPieces)
    }

    @Test
    fun `ganar un nivel abre el siguiente y coloca un azulejo`() {
        val win = PlayerProgress().completeLevel(1)
        assertTrue(win.firstTime)
        assertEquals(0, win.mural)
        assertEquals(1, win.muralPieces)
        assertFalse(win.muralCompleted)
        assertEquals(2, win.progress.nextLevel)
        assertTrue(win.progress.isUnlocked(2))
        assertFalse(win.progress.isUnlocked(3))
        assertFailsWith<IllegalArgumentException> { PlayerProgress().completeLevel(3) }
    }

    @Test
    fun `repetir un nivel ganado no coloca otro azulejo`() {
        val progress = PlayerProgress().completeLevel(1).progress.completeLevel(2).progress
        val again = progress.completeLevel(1)
        assertFalse(again.firstTime)
        assertEquals(progress, again.progress)
        assertEquals(2, again.muralPieces)
        assertEquals(3, again.progress.nextLevel)
    }

    @Test
    fun `los 200 niveles completan los 8 murales`() {
        var progress = PlayerProgress()
        var completedMurals = 0
        for (level in 1..LevelCatalog.COUNT) {
            assertEquals(level, progress.nextLevel)
            val win = progress.completeLevel(level)
            assertEquals(Murals.muralOf(level), win.mural)
            assertEquals(Murals.pieceOf(level) + 1, win.muralPieces)
            if (win.muralCompleted) completedMurals++
            assertEquals(level % 25 == 0, win.muralCompleted, "nivel $level")
            progress = win.progress
        }
        assertEquals(8, completedMurals)
        assertNull(progress.nextLevel)
        assertEquals(200, progress.totalPieces)
        for (mural in 0 until Murals.COUNT) assertEquals(25, progress.muralPieces(mural))
    }

    @Test
    fun `el siguiente nivel es el primer hueco`() {
        // Ganados 1–5 y 7 (por ejemplo, tras repetir niveles): el 6 está abierto y sin ganar.
        val progress = PlayerProgress(completedLevels = setOf(1, 2, 3, 4, 5, 7))
        assertEquals(6, progress.nextLevel)
        assertTrue(progress.isUnlocked(8))
        assertFalse(progress.isUnlocked(9))
    }

    // ---------- Murales ----------

    @Test
    fun `cada mural es simetrico y usa dibujos validos`() {
        for (mural in 0 until Murals.COUNT) {
            for (piece in 0 until Murals.PIECES) {
                val face = Murals.faceAt(mural, piece)
                assertTrue(face in 0 until 36, "mural $mural, pieza $piece")
                val row = piece / 5
                val column = piece % 5
                // Simetría de espejo horizontal, vertical y diagonal.
                assertEquals(face, Murals.faceAt(mural, row * 5 + (4 - column)))
                assertEquals(face, Murals.faceAt(mural, (4 - row) * 5 + column))
                assertEquals(face, Murals.faceAt(mural, column * 5 + row))
            }
            assertEquals(6, (0 until 25).map { Murals.faceAt(mural, it) }.toSet().size, "mural $mural usa 6 dibujos")
        }
        // Ningún par de murales es igual.
        val designs = (0 until Murals.COUNT).map { mural -> (0 until 25).map { Murals.faceAt(mural, it) } }
        assertEquals(designs.size, designs.toSet().size)
        assertEquals(37, Murals.levelOf(1, 11))
        assertEquals(1, Murals.muralOf(37))
        assertEquals(11, Murals.pieceOf(37))
    }

    // ---------- Reto diario y rachas ----------

    @Test
    fun `completar el reto de hoy suma racha`() {
        val win = PlayerProgress().completeDaily(today, today)
        assertEquals(DailyResult.ON_TIME, win.result)
        assertTrue(win.firstTime)
        assertEquals(1, win.streak)
        assertEquals(1, win.progress.bestStreak)
    }

    @Test
    fun `la racha cuenta dias seguidos y sobrevive hasta que acaba el dia`() {
        var progress = PlayerProgress()
        for (offset in 3 downTo 1) {
            val day = today.plusDays(-offset)
            progress = progress.completeDaily(day, day).progress
        }
        // Hoy aún no ha jugado: la racha de ayer sigue viva.
        assertEquals(3, progress.currentStreak(today))
        // Mañana sin haber jugado hoy, se ha perdido.
        assertEquals(0, progress.currentStreak(today.plusDays(1)))
        // Jugando hoy, sube a 4.
        val win = progress.completeDaily(today, today)
        assertEquals(4, win.streak)
        assertEquals(4, win.progress.bestStreak)
    }

    @Test
    fun `un dia pasado se juega pero no suma racha`() {
        val yesterday = today.plusDays(-1)
        val progress = PlayerProgress().completeDaily(today.plusDays(-2), today.plusDays(-2)).progress
        val late = progress.completeDaily(yesterday, today)
        assertEquals(DailyResult.LATE, late.result)
        assertEquals(0, late.streak)
        assertEquals(DailyResult.LATE, late.progress.dailyResult(yesterday))
        // Al jugar hoy, el hueco de ayer corta la racha.
        assertEquals(1, late.progress.completeDaily(today, today).streak)
        assertFailsWith<IllegalArgumentException> { progress.completeDaily(today.plusDays(1), today) }
    }

    @Test
    fun `repetir un reto completado a tiempo no lo estropea`() {
        val progress = PlayerProgress().completeDaily(today.plusDays(-1), today.plusDays(-1)).progress
        val again = progress.completeDaily(today.plusDays(-1), today)
        assertFalse(again.firstTime)
        assertEquals(DailyResult.ON_TIME, again.result)
        assertEquals(progress, again.progress)
    }

    @Test
    fun `la mejor racha recuerda rachas ya perdidas`() {
        var progress = PlayerProgress()
        for (offset in listOf(20, 19, 18, 17, 18, 10, 9, 0)) {
            val day = today.plusDays(-offset)
            progress = progress.completeDaily(day, day).progress
        }
        assertEquals(4, progress.bestStreak)
        assertEquals(1, progress.currentStreak(today))
    }

    // ---------- Guardado ----------

    @Test
    fun `el progreso se guarda y se recupera`() {
        var progress = PlayerProgress()
        for (level in 1..23) progress = progress.completeLevel(level).progress
        progress = progress.completeLevel(24).progress.completeLevel(25).progress
        progress = progress.completeDaily(today, today).progress
        progress = progress.completeDaily(today.plusDays(-1), today.plusDays(-1)).progress
        progress = progress.completeDaily(today.plusDays(-5), today).progress
        assertEquals(progress, PlayerProgressCodec.decode(PlayerProgressCodec.encode(progress)))
        assertEquals(PlayerProgress(), PlayerProgressCodec.decode(PlayerProgressCodec.encode(PlayerProgress())))
    }

    /** El texto exacto. Si esta prueba falla, una actualización dejaría de leer el progreso guardado. */
    @Test
    fun `el formato guardado no cambia entre versiones`() {
        val progress = PlayerProgress(
            completedLevels = (1..20).toSet() + 22,
            dailyResults = mapOf(
                20405 to DailyResult.ON_TIME,
                20406 to DailyResult.ON_TIME,
                20407 to DailyResult.ON_TIME,
                20390 to DailyResult.LATE,
            ),
        )
        val text = "azulejo-progreso 1\nlevels=1-20 22\ndaily=20405-20407\nlate=20390\n"
        assertEquals(text, PlayerProgressCodec.encode(progress))
        assertEquals(progress, PlayerProgressCodec.decode(text))
        assertEquals("levels=1-200", PlayerProgressCodec.encode(PlayerProgress((1..200).toSet())).lines()[1])
    }

    @Test
    fun `un progreso danado se rechaza sin lanzar`() {
        val good = PlayerProgressCodec.encode(PlayerProgress((1..5).toSet(), mapOf(20400 to DailyResult.ON_TIME)))
        val damaged = listOf(
            "",
            "azulejo-progreso 2\nlevels=\ndaily=\nlate=\n",
            good.replace("levels=1-5", "levels=1-500"),
            good.replace("levels=1-5", "levels=0"),
            good.replace("levels=1-5", "levels=5-1"),
            good.replace("levels=1-5", "levels=uno"),
            good.replace("late=", "late=20400"),
            good.replace("daily=20400", "daily=1-2000000000"),
            good.lines().take(2).joinToString("\n"),
        )
        for (text in damaged) assertNull(PlayerProgressCodec.decode(text), "debería rechazar:\n$text")
    }
}
