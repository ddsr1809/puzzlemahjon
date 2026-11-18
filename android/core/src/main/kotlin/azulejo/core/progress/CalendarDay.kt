package azulejo.core.progress

/**
 * Un día del calendario, sin hora ni zona horaria.
 *
 * `core` no puede usar `java.time` (regla 1), y además `java.time` no existe en Android 7
 * sin desugaring. Esta clase hace lo poco que necesita el reto diario: contar días, saber
 * el día de la semana y recorrer un mes. La app la construye con la fecha local del teléfono.
 *
 * El calendario es el gregoriano. [epochDay] cuenta los días desde el 1 de enero de 1970,
 * igual que `LocalDate.toEpochDay()`.
 */
class CalendarDay private constructor(val epochDay: Int) : Comparable<CalendarDay> {

    val year: Int
    val month: Int
    val day: Int

    init {
        // Algoritmo «civil_from_days» de Howard Hinnant.
        val z = epochDay + 719_468
        val era = (if (z >= 0) z else z - 146_096) / 146_097
        val doe = z - era * 146_097
        val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        day = doy - (153 * mp + 2) / 5 + 1
        month = if (mp < 10) mp + 3 else mp - 9
        year = yoe + era * 400 + if (month <= 2) 1 else 0
    }

    /** Día de la semana según ISO: 1 = lunes … 7 = domingo. */
    val dayOfWeek: Int get() = (epochDay + 3).mod(7) + 1

    fun plusDays(days: Int): CalendarDay = CalendarDay(epochDay + days)

    /** El día 1 de este mes. */
    fun firstOfMonth(): CalendarDay = of(year, month, 1)

    /** El día 1 del mes que está [months] meses después (o antes, si es negativo). */
    fun plusMonths(months: Int): CalendarDay {
        val index = year * 12 + (month - 1) + months
        return of(index.floorDiv(12), index.mod(12) + 1, 1)
    }

    val lengthOfMonth: Int get() = lengthOfMonth(year, month)

    override fun compareTo(other: CalendarDay): Int = epochDay.compareTo(other.epochDay)

    override fun equals(other: Any?): Boolean = other is CalendarDay && other.epochDay == epochDay

    override fun hashCode(): Int = epochDay

    /** Formato ISO, `2026-11-16`. Solo para depurar y para las pruebas. */
    override fun toString(): String =
        "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    companion object {
        fun ofEpochDay(epochDay: Int): CalendarDay = CalendarDay(epochDay)

        fun of(year: Int, month: Int, day: Int): CalendarDay {
            require(month in 1..12) { "Mes fuera de rango: $month" }
            require(day in 1..lengthOfMonth(year, month)) { "Día fuera de rango: $year-$month-$day" }
            // Algoritmo «days_from_civil» de Howard Hinnant.
            val y = if (month <= 2) year - 1 else year
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val mp = if (month > 2) month - 3 else month + 9
            val doy = (153 * mp + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return CalendarDay(era * 146_097 + doe - 719_468)
        }

        fun isLeapYear(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

        fun lengthOfMonth(year: Int, month: Int): Int = when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
}
