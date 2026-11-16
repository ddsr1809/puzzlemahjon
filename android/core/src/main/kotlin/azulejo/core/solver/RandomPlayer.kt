package azulejo.core.solver

import azulejo.core.board.Board
import azulejo.core.random.SeededRandom

/**
 * Jugador que retira pares al azar. No intenta ganar: mide lo fácil que es
 * equivocarse en un tablero.
 */
object RandomPlayer {

    /** Juega hasta vaciar el tablero o quedarse sin pares, y devuelve el tablero final. */
    fun playOnce(board: Board, random: SeededRandom): Board {
        var current = board
        while (true) {
            val moves = current.availableMoves()
            if (moves.isEmpty()) return current
            current = current.remove(moves[random.nextInt(moves.size)])
        }
    }

    /**
     * Fracción de [games] partidas al azar que terminan con el tablero vacío.
     *
     * Hace lo mismo que llamar [games] veces a [playOnce] con el mismo [random], pero sobre
     * arrays reutilizados en lugar de crear un tablero por movimiento: el generador lo llama
     * cientos de veces por nivel. Una prueba comprueba que los dos caminos coinciden.
     */
    fun winRate(board: Board, games: Int, random: SeededRandom): Double {
        require(games > 0) { "games debe ser positivo" }
        val tiles = board.tiles
        val geometry = board.geometry
        val n = tiles.size
        val faces = IntArray(n) { tiles[it].face }
        val start = BooleanArray(n) { board.isOnBoard(it) }
        val present = BooleanArray(n)
        val free = IntArray(n)
        val moves = IntArray(n * (n - 1) / 2) // cada par (a, b) se guarda como a * n + b

        var wins = 0
        repeat(games) {
            start.copyInto(present)
            var remaining = board.remainingCount
            while (remaining > 0) {
                var freeCount = 0
                for (id in 0 until n) {
                    if (present[id] && geometry.isFree(id) { present[it] }) free[freeCount++] = id
                }
                var moveCount = 0
                for (i in 0 until freeCount) {
                    for (j in i + 1 until freeCount) {
                        if (faces[free[i]] == faces[free[j]]) moves[moveCount++] = free[i] * n + free[j]
                    }
                }
                if (moveCount == 0) break

                val move = moves[random.nextInt(moveCount)]
                present[move / n] = false
                present[move % n] = false
                remaining -= 2
            }
            if (remaining == 0) wins++
        }
        return wins.toDouble() / games
    }
}
