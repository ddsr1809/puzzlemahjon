package azulejo.core.generator

import azulejo.core.board.BoardGeometry
import azulejo.core.board.Move
import azulejo.core.random.SeededRandom

/**
 * Vacía un tablero «sin dibujos»: retira al azar dos fichas libres cada vez, hasta que no
 * queda ninguna, y devuelve el orden. Si en algún momento quedan fichas pero menos de dos
 * libres, el intento falla y devuelve `null`.
 *
 * Es la base de la solución garantizada: si a cada par retirado se le da un mismo dibujo,
 * el orden devuelto es una solución. Lo usan el generador (con el tablero lleno) y barajar
 * (con las fichas que quedan).
 */
internal object Dismantler {

    /**
     * @param present qué fichas cuentan como presentes; no se modifica.
     * @param random se consume exactamente igual que en la versión 1 del generador, para que
     *   las semillas publicadas den los mismos niveles.
     */
    fun tryDismantle(geometry: BoardGeometry, present: BooleanArray, random: SeededRandom): List<Move>? {
        require(present.size == geometry.size) { "La máscara no corresponde a la geometría" }
        val state = present.copyOf()
        var remaining = state.count { it }
        val order = ArrayList<Move>(remaining / 2)
        val free = ArrayList<Int>()

        while (remaining > 0) {
            free.clear()
            for (index in 0 until geometry.size) {
                if (state[index] && geometry.isFree(index) { state[it] }) free += index
            }
            if (free.size < 2) return null

            val a = free.removeAt(random.nextInt(free.size))
            val b = free[random.nextInt(free.size)]
            state[a] = false
            state[b] = false
            remaining -= 2
            order += Move.of(a, b)
        }
        return order
    }
}
