package azulejo.core.progress

/** Cómo se completó el reto de un día. */
enum class DailyResult {
    /** El mismo día: cuenta para la racha. */
    ON_TIME,

    /** Otro día, desde el calendario: queda marcado pero no suma racha. */
    LATE,
}

/** Lo que cambia al ganar un nivel. */
data class LevelWin(
    val progress: PlayerProgress,
    val level: Int,
    /** `false` si el nivel ya estaba ganado: se puede repetir, pero no coloca otro azulejo. */
    val firstTime: Boolean,
    val mural: Int,
    /** Azulejos del mural tras ganar. */
    val muralPieces: Int,
    /** Este nivel ha colocado el último azulejo del mural. */
    val muralCompleted: Boolean,
)

/** Lo que cambia al completar un reto diario. */
data class DailyWin(
    val progress: PlayerProgress,
    val day: CalendarDay,
    val result: DailyResult,
    /** `false` si ese día ya estaba completado. */
    val firstTime: Boolean,
    /** Racha tras ganar. */
    val streak: Int,
)

/**
 * Progreso del jugador: niveles ganados y retos diarios completados. Inmutable.
 *
 * Los niveles se desbloquean en orden: se puede jugar el 1, cualquiera ya ganado y el que
 * sigue a uno ganado. Los ganados se pueden repetir.
 *
 * La racha cuenta los días seguidos con el reto completado el mismo día. Si hoy aún no se ha
 * jugado, la racha de ayer sigue viva: se pierde al acabar el día sin jugar.
 *
 * @property dailyResults resultado de cada día, por [CalendarDay.epochDay].
 */
data class PlayerProgress(
    val completedLevels: Set<Int> = emptySet(),
    val dailyResults: Map<Int, DailyResult> = emptyMap(),
) {
    init {
        require(completedLevels.all { it in 1..LevelCatalog.COUNT }) { "Nivel fuera de rango" }
    }

    // ---------- Niveles ----------

    fun isCompleted(level: Int): Boolean = level in completedLevels

    fun isUnlocked(level: Int): Boolean =
        level in 1..LevelCatalog.COUNT && (level == 1 || level in completedLevels || (level - 1) in completedLevels)

    /** El primer nivel desbloqueado sin ganar, o `null` si están todos ganados. */
    val nextLevel: Int?
        get() = (1..LevelCatalog.COUNT).firstOrNull { !isCompleted(it) && isUnlocked(it) }

    /** Azulejos colocados en el mural [mural] (de 0 a 7). */
    fun muralPieces(mural: Int): Int {
        val first = Murals.levelOf(mural, 0)
        return (first until first + Murals.PIECES).count { it in completedLevels }
    }

    val totalPieces: Int get() = completedLevels.size

    fun completeLevel(level: Int): LevelWin {
        require(isUnlocked(level)) { "El nivel $level no está desbloqueado" }
        val firstTime = level !in completedLevels
        val next = if (firstTime) copy(completedLevels = completedLevels + level) else this
        val mural = Murals.muralOf(level)
        val pieces = next.muralPieces(mural)
        return LevelWin(next, level, firstTime, mural, pieces, firstTime && pieces == Murals.PIECES)
    }

    // ---------- Reto diario ----------

    fun dailyResult(day: CalendarDay): DailyResult? = dailyResults[day.epochDay]

    /**
     * Marca el reto de [day] como completado. Si [day] es [today] cuenta para la racha. Un día
     * ya completado a tiempo no pasa a «tarde» por volver a jugarlo.
     */
    fun completeDaily(day: CalendarDay, today: CalendarDay): DailyWin {
        require(day <= today) { "No se puede completar un reto futuro: $day" }
        val previous = dailyResult(day)
        val result = if (day == today || previous == DailyResult.ON_TIME) DailyResult.ON_TIME else DailyResult.LATE
        val next = copy(dailyResults = dailyResults + (day.epochDay to result))
        return DailyWin(next, day, result, previous == null, next.currentStreak(today))
    }

    /** Días seguidos con el reto completado a tiempo, terminando hoy o, si hoy falta, ayer. */
    fun currentStreak(today: CalendarDay): Int {
        var day = if (dailyResult(today) == DailyResult.ON_TIME) today else today.plusDays(-1)
        var streak = 0
        while (dailyResult(day) == DailyResult.ON_TIME) {
            streak++
            day = day.plusDays(-1)
        }
        return streak
    }

    /** La racha más larga de la historia. */
    val bestStreak: Int
        get() {
            var best = 0
            var run = 0
            var previous: Int? = null
            for (epochDay in dailyResults.filterValues { it == DailyResult.ON_TIME }.keys.sorted()) {
                run = if (previous != null && epochDay == previous + 1) run + 1 else 1
                if (run > best) best = run
                previous = epochDay
            }
            return best
        }
}

/**
 * Convierte el [PlayerProgress] en texto y al revés, con el mismo estilo que
 * `GameSnapshotCodec`:
 *
 * ```
 * azulejo-progreso 1
 * levels=1-20 22
 * daily=20405-20407
 * late=20390
 * ```
 *
 * Los números seguidos se guardan como intervalos, así que 200 niveles ganados ocupan
 * `levels=1-200`. Los días son [CalendarDay.epochDay]. Si el formato cambia de forma
 * incompatible, sube [FORMAT].
 */
object PlayerProgressCodec {

    const val FORMAT = 1

    private const val HEADER = "azulejo-progreso"

    fun encode(progress: PlayerProgress): String = buildString {
        append(HEADER).append(' ').append(FORMAT).append('\n')
        line("levels", ranges(progress.completedLevels))
        line("daily", ranges(progress.dailyResults.filterValues { it == DailyResult.ON_TIME }.keys))
        line("late", ranges(progress.dailyResults.filterValues { it == DailyResult.LATE }.keys))
    }

    /** Lee el texto de [encode]. Devuelve `null` si no es de este formato o está dañado; nunca lanza. */
    fun decode(text: String): PlayerProgress? {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.firstOrNull() != "$HEADER $FORMAT") return null

        val values = HashMap<String, String>()
        for (line in lines.drop(1)) {
            val separator = line.indexOf('=')
            if (separator <= 0) return null
            values[line.substring(0, separator)] = line.substring(separator + 1)
        }

        val levels = parseRanges(values["levels"] ?: return null) ?: return null
        if (levels.any { it !in 1..LevelCatalog.COUNT }) return null
        val onTime = parseRanges(values["daily"] ?: return null) ?: return null
        val late = parseRanges(values["late"] ?: return null) ?: return null
        if (onTime.any { it in late }) return null

        val daily = HashMap<Int, DailyResult>()
        onTime.forEach { daily[it] = DailyResult.ON_TIME }
        late.forEach { daily[it] = DailyResult.LATE }
        return PlayerProgress(levels, daily)
    }

    private fun StringBuilder.line(key: String, value: String) {
        append(key).append('=').append(value).append('\n')
    }

    private fun ranges(numbers: Collection<Int>): String {
        val sorted = numbers.sorted()
        val parts = ArrayList<String>()
        var i = 0
        while (i < sorted.size) {
            var j = i
            while (j + 1 < sorted.size && sorted[j + 1] == sorted[j] + 1) j++
            parts += if (i == j) "${sorted[i]}" else "${sorted[i]}-${sorted[j]}"
            i = j + 1
        }
        return parts.joinToString(" ")
    }

    private fun parseRanges(text: String): Set<Int>? {
        if (text.isEmpty()) return emptySet()
        val result = HashSet<Int>()
        for (part in text.split(' ')) {
            // Un guion después del primer carácter separa el intervalo; al principio es un signo.
            val dash = part.indexOf('-', startIndex = 1)
            if (dash < 0) {
                result += part.toIntOrNull() ?: return null
            } else {
                val from = part.substring(0, dash).toIntOrNull() ?: return null
                val to = part.substring(dash + 1).toIntOrNull() ?: return null
                if (to < from || to.toLong() - from > MAX_RANGE) return null
                for (n in from..to) result += n
            }
        }
        return result
    }

    /** Ningún intervalo legítimo es tan largo; evita que un texto dañado llene la memoria. */
    private const val MAX_RANGE = 100_000
}
