package azulejo.core

import azulejo.core.progress.CalendarDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CalendarDayTest {

    @Test
    fun `fechas conocidas`() {
        assertEquals(0, CalendarDay.of(1970, 1, 1).epochDay)
        assertEquals(4, CalendarDay.of(1970, 1, 1).dayOfWeek) // jueves
        val today = CalendarDay.of(2026, 11, 16)
        assertEquals(20_773, today.epochDay)
        assertEquals(1, today.dayOfWeek) // lunes
        assertEquals("2026-11-16", today.toString())
        assertEquals(7, CalendarDay.of(2026, 11, 1).dayOfWeek) // domingo
        assertEquals(10_957, CalendarDay.of(2000, 1, 1).epochDay)
        assertEquals(-1, CalendarDay.of(1969, 12, 31).epochDay)
    }

    /** Ida y vuelta día a día durante 500 años, incluidos los bisiestos de 1900, 2000 y 2100. */
    @Test
    fun `cada dia se convierte y vuelve igual`() {
        var day = CalendarDay.of(1800, 1, 1)
        val end = CalendarDay.of(2300, 1, 1)
        var weekday = day.dayOfWeek
        while (day < end) {
            assertEquals(day, CalendarDay.of(day.year, day.month, day.day))
            assertEquals(day, CalendarDay.ofEpochDay(day.epochDay))
            val next = day.plusDays(1)
            weekday = weekday % 7 + 1
            assertEquals(weekday, next.dayOfWeek)
            if (next.month == day.month) {
                assertEquals(day.day + 1, next.day)
            } else {
                assertEquals(day.lengthOfMonth, day.day)
                assertEquals(1, next.day)
            }
            day = next
        }
    }

    @Test
    fun `bisiestos y largo de los meses`() {
        assertEquals(29, CalendarDay.lengthOfMonth(2000, 2))
        assertEquals(28, CalendarDay.lengthOfMonth(1900, 2))
        assertEquals(29, CalendarDay.lengthOfMonth(2028, 2))
        assertEquals(28, CalendarDay.lengthOfMonth(2026, 2))
        assertEquals(30, CalendarDay.lengthOfMonth(2026, 11))
        assertEquals(31, CalendarDay.lengthOfMonth(2026, 12))
    }

    @Test
    fun `meses anteriores y siguientes`() {
        val day = CalendarDay.of(2026, 11, 16)
        assertEquals(CalendarDay.of(2026, 11, 1), day.firstOfMonth())
        assertEquals(CalendarDay.of(2026, 12, 1), day.plusMonths(1))
        assertEquals(CalendarDay.of(2027, 1, 1), day.plusMonths(2))
        assertEquals(CalendarDay.of(2026, 10, 1), day.plusMonths(-1))
        assertEquals(CalendarDay.of(2025, 11, 1), day.plusMonths(-12))
        assertEquals(CalendarDay.of(2025, 12, 1), CalendarDay.of(2026, 1, 31).plusMonths(-1))
    }

    @Test
    fun `fechas imposibles fallan`() {
        assertFailsWith<IllegalArgumentException> { CalendarDay.of(2026, 2, 29) }
        assertFailsWith<IllegalArgumentException> { CalendarDay.of(2026, 13, 1) }
        assertFailsWith<IllegalArgumentException> { CalendarDay.of(2026, 4, 31) }
        assertFailsWith<IllegalArgumentException> { CalendarDay.of(2026, 1, 0) }
        assertTrue(CalendarDay.of(2026, 1, 1) < CalendarDay.of(2026, 1, 2))
    }
}
