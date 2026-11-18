package azulejo.core.session

import azulejo.core.board.Board
import azulejo.core.generator.Dismantler
import azulejo.core.random.SeededRandom

/**
 * Barajar: reparte de nuevo los dibujos de las fichas que quedan, sin moverlas,
 * de forma que el tablero resultante siempre tenga solución.
 *
 * Usa la misma idea que el generador: vacía «al revés» las posiciones que quedan y da el
 * mismo dibujo a cada par retirado. Los dibujos que se reparten son exactamente los que
 * había, así que el jugador no gana ni pierde fichas.
 */
object Shuffler {

    private const val MAX_ATTEMPTS = 200

    /**
     * Devuelve el tablero barajado, o `null` si las fichas que quedan no se pueden retirar
     * nunca, sea cual sea el reparto (por ejemplo, dos fichas apiladas). En ese caso solo
     * queda deshacer o reiniciar.
     */
    fun shuffle(board: Board, random: SeededRandom): Board? {
        if (board.isCleared) return board

        val present = board.presence()
        val pairFaces = pairFacesOf(board)

        repeat(MAX_ATTEMPTS) {
            val order = Dismantler.tryDismantle(board.geometry, present, random) ?: return@repeat

            val shuffledFaces = pairFaces.toMutableList()
            random.shuffle(shuffledFaces)

            val faces = board.tiles.map { it.face }.toMutableList()
            order.forEachIndexed { index, move ->
                faces[move.first] = shuffledFaces[index]
                faces[move.second] = shuffledFaces[index]
            }
            return board.withFaces(faces)
        }
        return null
    }

    /** Un dibujo por cada par que queda: si quedan cuatro fichas del dibujo 7, aparece dos veces. */
    private fun pairFacesOf(board: Board): List<Int> {
        val counts = board.remainingTiles().groupingBy { it.face }.eachCount()
        val pairs = ArrayList<Int>(board.remainingCount / 2)
        for ((face, count) in counts.entries.sortedBy { it.key }) {
            check(count % 2 == 0) { "El dibujo $face tiene un número impar de fichas: $count" }
            repeat(count / 2) { pairs += face }
        }
        return pairs
    }
}
