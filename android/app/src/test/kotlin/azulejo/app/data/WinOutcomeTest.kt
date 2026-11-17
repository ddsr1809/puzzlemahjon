package azulejo.app.data

import azulejo.core.generator.Difficulty
import azulejo.core.progress.CalendarDay
import azulejo.core.progress.DailyResult
import azulejo.core.progress.PlayerProgress
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WinOutcomeTest {

    private val today = CalendarDay.of(2026, 11, 16)

    @Test
    fun `ganar un nivel coloca su azulejo y propone el siguiente`() {
        val recorded = recordWin(PlayerProgress(), GameMode.Level(1), Difficulty.EASY, today)
        val outcome = assertIs<WinOutcome.Level>(recorded.outcome)
        assertEquals(WinOutcome.Level(1, firstTime = true, mural = 0, muralPieces = 1, muralCompleted = false, nextLevel = 2), outcome)
        assertTrue(recorded.progress.isCompleted(1))
    }

    @Test
    fun `el nivel 25 completa el primer mural`() {
        val progress = PlayerProgress((1..24).toSet())
        val outcome = assertIs<WinOutcome.Level>(recordWin(progress, GameMode.Level(25), Difficulty.EASY, today).outcome)
        assertTrue(outcome.muralCompleted)
        assertEquals(25, outcome.muralPieces)
        assertEquals(26, outcome.nextLevel)
    }

    @Test
    fun `repetir un nivel antiguo propone el primero sin ganar`() {
        val progress = PlayerProgress((1..10).toSet())
        val outcome = assertIs<WinOutcome.Level>(recordWin(progress, GameMode.Level(3), Difficulty.EASY, today).outcome)
        assertFalse(outcome.firstTime)
        assertEquals(11, outcome.nextLevel)
    }

    @Test
    fun `el ultimo nivel no propone otro`() {
        val progress = PlayerProgress((1..199).toSet())
        val outcome = assertIs<WinOutcome.Level>(recordWin(progress, GameMode.Level(200), Difficulty.HARD, today).outcome)
        assertTrue(outcome.muralCompleted)
        assertNull(outcome.nextLevel)
    }

    @Test
    fun `un nivel bloqueado no se apunta y no rompe nada`() {
        val recorded = recordWin(PlayerProgress(), GameMode.Level(50), Difficulty.MEDIUM, today)
        assertEquals(PlayerProgress(), recorded.progress)
        assertEquals(1, assertIs<WinOutcome.Level>(recorded.outcome).nextLevel)
    }

    @Test
    fun `el reto de hoy suma racha y uno pasado no`() {
        val first = recordWin(PlayerProgress(), GameMode.Daily(today.plusDays(-1)), Difficulty.MEDIUM, today)
        assertEquals(WinOutcome.Daily(today.plusDays(-1), DailyResult.LATE, streak = 0, bestStreak = 0), first.outcome)
        val second = recordWin(first.progress, GameMode.Daily(today), Difficulty.EASY, today)
        assertEquals(WinOutcome.Daily(today, DailyResult.ON_TIME, streak = 1, bestStreak = 1), second.outcome)
    }

    @Test
    fun `un reto del futuro (reloj atrasado) no se apunta`() {
        val recorded = recordWin(PlayerProgress(), GameMode.Daily(today.plusDays(1)), Difficulty.EASY, today)
        assertEquals(PlayerProgress(), recorded.progress)
        assertIs<WinOutcome.Daily>(recorded.outcome)
    }

    @Test
    fun `la partida rapida no toca el progreso`() {
        val progress = PlayerProgress((1..3).toSet())
        val recorded = recordWin(progress, GameMode.Quick, Difficulty.HARD, today)
        assertEquals(progress, recorded.progress)
        assertEquals(WinOutcome.Quick(Difficulty.HARD), recorded.outcome)
    }

    @Test
    fun `la fecha local sale del calendario del telefono`() {
        val calendar = GregorianCalendar(TimeZone.getTimeZone("America/Los_Angeles"))
        calendar.clear()
        calendar.set(2026, 10, 16, 23, 59) // los meses de Calendar empiezan en 0
        assertEquals(today, localToday(calendar))
    }
}
