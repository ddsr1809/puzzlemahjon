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

/**
 * Primer día de la semana según la región del teléfono, en numeración ISO (1 = lunes …
 * 7 = domingo).
 *
 * Depende de la región y no del idioma: en EE. UU. y en México la semana empieza en domingo
 * también para quien usa el teléfono en español; en España, en lunes. Por eso no sale de los
 * textos traducidos ni del idioma elegido en Ajustes.
 */
fun localFirstDayOfWeek(calendar: Calendar = Calendar.getInstance()): Int =
    // Calendar cuenta 1 = domingo … 7 = sábado.
    (calendar.firstDayOfWeek + 5) % 7 + 1
