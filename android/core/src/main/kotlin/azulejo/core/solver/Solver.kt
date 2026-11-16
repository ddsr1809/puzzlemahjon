package azulejo.core.solver

import azulejo.core.board.Board
import azulejo.core.board.Move
import azulejo.core.random.SeededRandom

sealed interface SolveResult {
    /** Hay solución; [moves] vacía el tablero desde el estado recibido. */
    data class Solved(val moves: List<Move>) : SolveResult

    /** Se han explorado todas las opciones y ninguna vacía el tablero. */
    data object Unsolvable : SolveResult

    /** Se agotó el presupuesto de búsqueda sin llegar a una respuesta. */
    data object Unknown : SolveResult
}

/**
 * Busca una solución para cualquier estado del tablero, sin saber cómo se generó.
 *
 * Lo usan las pruebas para comprobar los niveles de forma independiente del generador.
 * En el hito 2 servirá para que la pista no lleve a un bloqueo y para validar «barajar».
 *
 * Es una búsqueda en profundidad con tres ideas:
 * - Si todas las fichas que quedan de un dibujo están libres, retirar un par de ellas
 *   nunca perjudica, así que se hace sin abrir alternativas.
 * - Los estados explorados por completo sin éxito se recuerdan para no repetirlos.
 * - Una mala decisión al principio puede esconder la solución tras millones de estados.
 *   Por eso la búsqueda se reinicia con un presupuesto cada vez mayor y probando los
 *   pares en otro orden; lo aprendido sobre estados sin salida se conserva.
 *
 * El orden de prueba sale de una semilla fija: el mismo tablero da siempre el mismo resultado.
 */
object Solver {

    const val DEFAULT_NODE_BUDGET = 200_000

    fun solve(board: Board, nodeBudget: Int = DEFAULT_NODE_BUDGET): SolveResult {
        require(board.tiles.size <= MAX_TILES) { "El solucionador admite hasta $MAX_TILES fichas" }
        val search = Search(board, SeededRandom(ORDER_SEED))
        var budgetLeft = nodeBudget
        var slice = FIRST_SLICE
        while (true) {
            val budget = minOf(slice, budgetLeft)
            when (search.attempt(budget)) {
                Outcome.SOLVED -> return SolveResult.Solved(search.path.toList())
                Outcome.DEAD_END -> return SolveResult.Unsolvable
                Outcome.OUT_OF_BUDGET -> {
                    budgetLeft -= budget
                    if (budgetLeft <= 0) return SolveResult.Unknown
                    slice *= 2
                }
            }
        }
    }

    private const val MAX_TILES = 192
    private const val FIRST_SLICE = 500
    private const val ORDER_SEED = 0x417A756C656A6FL

    private enum class Outcome { SOLVED, DEAD_END, OUT_OF_BUDGET }

    private data class StateKey(val a: Long, val b: Long, val c: Long)

    private class Search(board: Board, private val random: SeededRandom) {
        private val tiles = board.tiles
        private val geometry = board.geometry
        private val present = BooleanArray(tiles.size) { board.isOnBoard(it) }
        private var remaining = board.remainingCount
        private val remainingOfFace = IntArray((tiles.maxOfOrNull { it.face } ?: 0) + 1).also { counts ->
            for (tile in tiles) if (present[tile.id]) counts[tile.face]++
        }
        private val deadStates = HashSet<StateKey>()
        private var budget = 0
        private var outOfBudget = false

        val path = ArrayList<Move>()

        /** Una búsqueda completa desde el estado inicial, visitando como mucho [nodeBudget] estados. */
        fun attempt(nodeBudget: Int): Outcome {
            budget = nodeBudget
            outOfBudget = false
            return when {
                run() -> Outcome.SOLVED
                outOfBudget -> Outcome.OUT_OF_BUDGET
                else -> Outcome.DEAD_END
            }
        }

        private fun run(): Boolean {
            if (remaining == 0) return true
            if (budget <= 0) {
                outOfBudget = true
                return false
            }
            budget--

            val key = stateKey()
            if (key in deadStates) return false

            // Fichas libres agrupadas por dibujo, en orden de id para que el resultado sea estable.
            val freeByFace = LinkedHashMap<Int, MutableList<Int>>()
            for (tile in tiles) {
                if (present[tile.id] && geometry.isFree(tile.id) { present[it] }) {
                    freeByFace.getOrPut(tile.face) { ArrayList(4) } += tile.id
                }
            }

            // Atajo: un dibujo con todas sus fichas restantes libres se puede retirar sin riesgo.
            for ((face, free) in freeByFace) {
                if (free.size >= 2 && free.size == remainingOfFace[face]) {
                    val solved = tryMove(free[0], free[1])
                    if (!solved && !outOfBudget) deadStates += key
                    return solved
                }
            }

            val candidates = ArrayList<Move>()
            for (free in freeByFace.values) {
                for (i in free.indices) {
                    for (j in i + 1 until free.size) candidates += Move.of(free[i], free[j])
                }
            }
            random.shuffle(candidates)

            for (move in candidates) {
                if (tryMove(move.first, move.second)) return true
                if (outOfBudget) return false
            }

            deadStates += key
            return false
        }

        private fun tryMove(a: Int, b: Int): Boolean {
            val face = tiles[a].face
            present[a] = false
            present[b] = false
            remaining -= 2
            remainingOfFace[face] -= 2
            path += Move.of(a, b)

            if (run()) return true

            path.removeAt(path.lastIndex)
            remainingOfFace[face] += 2
            remaining += 2
            present[a] = true
            present[b] = true
            return false
        }

        private fun stateKey(): StateKey {
            var a = 0L
            var b = 0L
            var c = 0L
            for (i in present.indices) {
                if (!present[i]) continue
                when (i / 64) {
                    0 -> a = a or (1L shl i)
                    1 -> b = b or (1L shl (i - 64))
                    else -> c = c or (1L shl (i - 128))
                }
            }
            return StateKey(a, b, c)
        }
    }
}
