package azulejo.app.data

import azulejo.core.progress.CalendarDay
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * La fecha de hoy según el reloj y la zona horaria del teléfono.
 *
 * Usa `java.util.Calendar` y no `java.time`, que no existe en Android 7 sin desugaring.
 * Siempre gregoriano: `Calendar.getInstance()` daría el calendario budista o el japonés en
 * teléfonos configurados así, con otro número de año.
 */
fun localToday(calendar: Calendar = GregorianCalendar()): CalendarDay =
    CalendarDay.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
