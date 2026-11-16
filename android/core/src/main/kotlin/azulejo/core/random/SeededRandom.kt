package azulejo.core.random

/**
 * Generador pseudoaleatorio propio (SplitMix64).
 *
 * No se usa `kotlin.random.Random` a propósito: su algoritmo puede cambiar entre
 * versiones de Kotlin, y aquí la misma semilla tiene que dar el mismo tablero
 * en Android, en iOS y dentro de cinco años. De eso dependen el reto diario
 * y los 200 niveles. Las pruebas fijan la secuencia con valores conocidos.
 */
class SeededRandom(seed: Long) {

    private var state: Long = seed

    fun nextLong(): Long {
        state += GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    /** Entero uniforme en `0 until bound`. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound debe ser positivo" }
        val bits = nextLong() ushr 33 // 31 bits altos, siempre no negativos
        return ((bits * bound) ushr 31).toInt()
    }

    /** Baraja la lista en el sitio (Fisher–Yates). */
    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.lastIndex downTo 1) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }

    /** Generador hijo independiente; el padre avanza un paso. */
    fun fork(): SeededRandom = SeededRandom(nextLong())

    private companion object {
        val GAMMA = 0x9E3779B97F4A7C15uL.toLong()
        val MIX_1 = 0xBF58476D1CE4E5B9uL.toLong()
        val MIX_2 = 0x94D049BB133111EBuL.toLong()
    }
}
