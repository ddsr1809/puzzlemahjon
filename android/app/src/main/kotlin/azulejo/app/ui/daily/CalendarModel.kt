package azulejo.app.ui.daily

import azulejo.core.progress.CalendarDay
import azulejo.core.progress.DailyChallenge
import azulejo.core.progress.DailyResult
import azulejo.core.progress.PlayerProgress

/** Cómo se muestra un día en el calendario del reto. */
enum class DayStatus {
    /** Completado ese mismo día: cuenta para la racha. */
    DONE_ON_TIME,

    /** Completado otro día. */
    DONE_LATE,

    /** Hoy, aún sin completar. */
    TODAY,

    /** Día pasado sin completar: se puede jugar, pero no suma racha. */
    MISSED,

    /** Futuro, o anterior al primer reto: no se puede jugar. */
    LOCKED,
}

data class CalendarCell(val day: CalendarDay, val status: DayStatus) {
    val playable: Boolean get() = status != DayStatus.LOCKED
}

/** La cuadrícula del calendario del reto diario. Kotlin puro. */
object CalendarModel {

    /**
     * Días de la semana en el orden de las columnas, en numeración ISO (1 = lunes).
     *
     * @param firstDayOfWeek primer día de la semana, ISO: 7 (domingo) en EE. UU., 1 (lunes)
     *   en España y casi toda Latinoamérica.
     */
    fun weekdays(firstDayOfWeek: Int): List<Int> {
        require(firstDayOfWeek in 1..7) { "Día de la semana fuera de rango: $firstDayOfWeek" }
        return (0 until 7).map { (firstDayOfWeek - 1 + it) % 7 + 1 }
    }

    /**
     * Celdas del mes de [month], fila a fila. Las primeras son `null` para que el día 1 caiga
     * en su columna.
     */
    fun cells(month: CalendarDay, firstDayOfWeek: Int, progress: PlayerProgress, today: CalendarDay): List<CalendarCell?> {
        val first = month.firstOfMonth()
        val padding = (first.dayOfWeek - firstDayOfWeek + 7) % 7
        val cells = ArrayList<CalendarCell?>(padding + first.lengthOfMonth)
        repeat(padding) { cells += null }
        for (offset in 0 until first.lengthOfMonth) {
            val day = first.plusDays(offset)
            cells += CalendarCell(day, status(day, progress, today))
        }
        return cells
    }

    fun status(day: CalendarDay, progress: PlayerProgress, today: CalendarDay): DayStatus =
        when (progress.dailyResult(day)) {
            DailyResult.ON_TIME -> DayStatus.DONE_ON_TIME
            DailyResult.LATE -> DayStatus.DONE_LATE
            null -> when {
                !DailyChallenge.isPlayable(day, today) -> DayStatus.LOCKED
                day == today -> DayStatus.TODAY
                else -> DayStatus.MISSED
            }
        }

    /** Se puede ir al mes anterior: aún hay retos en él. */
    fun hasPreviousMonth(month: CalendarDay): Boolean = month.firstOfMonth() > DailyChallenge.FIRST_DAY.firstOfMonth()

    /** Se puede ir al mes siguiente: no es el mes de hoy. */
    fun hasNextMonth(month: CalendarDay, today: CalendarDay): Boolean = month.firstOfMonth() < today.firstOfMonth()
}
