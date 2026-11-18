package azulejo.app.ui.daily

import azulejo.app.data.GameMode
import azulejo.app.ui.game.GameUiState
import azulejo.core.generator.Difficulty
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.progress.CalendarDay
import azulejo.core.progress.DailyChallenge
import azulejo.core.progress.PlayerProgress
import azulejo.core.session.GameSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarModelTest {

    private val today = CalendarDay.of(2026, 11, 16)

    @Test
    fun `las columnas empiezan en domingo o en lunes`() {
        assertEquals(listOf(7, 1, 2, 3, 4, 5, 6), CalendarModel.weekdays(7))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), CalendarModel.weekdays(1))
    }

    @Test
    fun `el dia 1 cae en su columna`() {
        // El 1 de noviembre de 2026 es domingo.
        val sundayFirst = CalendarModel.cells(today, 7, PlayerProgress(), today)
        assertEquals(30, sundayFirst.size)
        assertEquals(1, sundayFirst[0]?.day?.day)
        val mondayFirst = CalendarModel.cells(today, 1, PlayerProgress(), today)
        assertEquals(6, mondayFirst.count { it == null })
        assertNull(mondayFirst[5])
        assertEquals(CalendarDay.of(2026, 11, 1), mondayFirst[6]?.day)
        assertEquals(CalendarDay.of(2026, 11, 30), mondayFirst.last()?.day)
    }

    @Test
    fun `estado de cada dia`() {
        val progress = PlayerProgress()
            .completeDaily(today.plusDays(-3), today.plusDays(-3)).progress
            .completeDaily(today.plusDays(-2), today).progress
        val cells = CalendarModel.cells(today, 7, progress, today).filterNotNull().associate { it.day.day to it.status }
        assertEquals(DayStatus.DONE_ON_TIME, cells[13])
        assertEquals(DayStatus.DONE_LATE, cells[14])
        assertEquals(DayStatus.MISSED, cells[15])
        assertEquals(DayStatus.TODAY, cells[16])
        assertEquals(DayStatus.LOCKED, cells[17])
        assertEquals(DayStatus.MISSED, cells[1])
        // Octubre es anterior al primer reto: todo bloqueado.
        val october = CalendarModel.cells(today.plusMonths(-1), 7, progress, today).filterNotNull()
        assertTrue(october.all { it.status == DayStatus.LOCKED && !it.playable })
    }

    @Test
    fun `no se navega antes del primer reto ni despues de hoy`() {
        assertFalse(CalendarModel.hasPreviousMonth(DailyChallenge.FIRST_DAY))
        assertTrue(CalendarModel.hasPreviousMonth(CalendarDay.of(2026, 12, 1)))
        assertFalse(CalendarModel.hasNextMonth(today, today))
        assertTrue(CalendarModel.hasNextMonth(CalendarDay.of(2026, 10, 1), today))
    }

    // ---------- Continuar o abandonar ----------

    private fun session(): GameSession =
        GameSession.start(LevelGenerator().generate(LayoutCatalog.patio, Difficulty.EASY, 1).board)

    @Test
    fun `pedir el mismo nivel continua y otro pregunta solo si hay algo que perder`() {
        val fresh = session()
        val level = LevelGenerator().generate(LayoutCatalog.patio, Difficulty.EASY, 1)
        val played = fresh.tap(level.solution[0].first).session.tap(level.solution[0].second).session

        val state = GameUiState(today = today, mode = GameMode.Level(4), session = played)
        assertTrue(state.canContinue)
        assertTrue(state.continues(GameMode.Level(4)))
        assertFalse(state.wouldAbandon(GameMode.Level(4)))
        assertTrue(state.wouldAbandon(GameMode.Level(5)))
        assertTrue(state.wouldAbandon(GameMode.Daily(today)))
        assertTrue(state.wouldAbandon(GameMode.Quick))

        // Sin ninguna jugada no hay nada que perder.
        assertFalse(state.copy(session = fresh).wouldAbandon(GameMode.Level(5)))
        // La partida rápida pedida desde el inicio siempre es nueva.
        val quick = state.copy(mode = GameMode.Quick)
        assertFalse(quick.continues(GameMode.Quick))
        assertTrue(quick.wouldAbandon(GameMode.Quick))
        assertFalse(GameUiState(today = today).wouldAbandon(GameMode.Level(1)))
    }
}
