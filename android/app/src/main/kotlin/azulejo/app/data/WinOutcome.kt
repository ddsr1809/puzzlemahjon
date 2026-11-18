package azulejo.app.data

import azulejo.core.generator.Difficulty
import azulejo.core.progress.CalendarDay
import azulejo.core.progress.DailyChallenge
import azulejo.core.progress.DailyResult
import azulejo.core.progress.LevelCatalog
import azulejo.core.progress.Murals
import azulejo.core.progress.PlayerProgress

/** Lo que se le cuenta al jugador al vaciar el tablero. Kotlin puro. */
sealed interface WinOutcome {

    data class Level(
        val level: Int,
        /** `false` al repetir un nivel ya ganado. */
        val firstTime: Boolean,
        /** Mural del nivel, de 0 a 7. */
        val mural: Int,
        val muralPieces: Int,
        val muralCompleted: Boolean,
        /** Nivel que toca ahora, o `null` si ya están todos ganados. */
        val nextLevel: Int?,
    ) : WinOutcome

    data class Daily(
        val day: CalendarDay,
        val result: DailyResult,
        val streak: Int,
        val bestStreak: Int,
    ) : WinOutcome

    data class Quick(val difficulty: Difficulty) : WinOutcome
}

/** El progreso tras ganar y lo que se le cuenta al jugador. */
data class RecordedWin(val progress: PlayerProgress, val outcome: WinOutcome)

/**
 * Apunta una victoria en el progreso.
 *
 * Nunca lanza: si el modo no encaja con el progreso (por ejemplo, el reloj del teléfono se
 * ha atrasado y el reto parece del futuro) no se apunta nada, pero se felicita igual.
 */
fun recordWin(progress: PlayerProgress, mode: GameMode, difficulty: Difficulty, today: CalendarDay): RecordedWin =
    when (mode) {
        GameMode.Quick -> RecordedWin(progress, WinOutcome.Quick(difficulty))

        is GameMode.Level -> {
            val win = if (progress.isUnlocked(mode.number)) progress.completeLevel(mode.number) else null
            val next = win?.progress ?: progress
            val outcome = WinOutcome.Level(
                level = mode.number,
                firstTime = win?.firstTime ?: false,
                mural = win?.mural ?: Murals.muralOf(mode.number),
                muralPieces = win?.muralPieces ?: 0,
                muralCompleted = win?.muralCompleted ?: false,
                // Tras repetir un nivel antiguo, lo natural es volver al primero sin ganar.
                nextLevel = if (mode.number < LevelCatalog.COUNT && next.isUnlocked(mode.number + 1) &&
                    !next.isCompleted(mode.number + 1)
                ) {
                    mode.number + 1
                } else {
                    next.nextLevel
                },
            )
            RecordedWin(next, outcome)
        }

        is GameMode.Daily -> {
            val win = if (DailyChallenge.isPlayable(mode.day, today)) progress.completeDaily(mode.day, today) else null
            val next = win?.progress ?: progress
            val outcome = WinOutcome.Daily(
                day = mode.day,
                result = win?.result ?: DailyResult.LATE,
                streak = next.currentStreak(today),
                bestStreak = next.bestStreak,
            )
            RecordedWin(next, outcome)
        }
    }
