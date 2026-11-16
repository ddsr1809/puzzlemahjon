package azulejo.core.generator

import azulejo.core.board.Board
import azulejo.core.board.BoardGeometry
import azulejo.core.board.Layout
import azulejo.core.board.Move
import azulejo.core.random.SeededRandom
import azulejo.core.solver.RandomPlayer
import kotlin.math.abs

/**
 * Nivel listo para jugar.
 *
 * @property solution una forma de vaciar el tablero, par a par. Es la prueba de que
 *   el nivel tiene solución; el jugador puede encontrar otras.
 * @property randomWinRate fracción de partidas que gana un jugador que elige pares al
 *   azar (entre 0 y 1). Sirve para comparar tableros; con [Difficulty.EASY] es siempre 1.
 */
class GeneratedLevel(
    val layoutId: String,
    val difficulty: Difficulty,
    val seed: Long,
    val board: Board,
    val solution: List<Move>,
    val randomWinRate: Double,
) {
    /** Dibujo de cada ficha, en orden de id. Dos niveles son iguales si esto coincide. */
    val faces: List<Int> get() = board.tiles.map { it.face }
}

/**
 * Genera niveles con solución garantizada.
 *
 * La idea es jugar la partida al revés antes de repartir los dibujos:
 *
 * 1. Con el tablero lleno y sin dibujos, se eligen al azar dos fichas libres y se
 *    retiran. Se repite hasta vaciarlo, apuntando el orden.
 * 2. A cada par retirado se le da el mismo dibujo.
 *
 * El orden apuntado es, por construcción, una solución válida. Si en el paso 1 quedan
 * fichas pero menos de dos libres (por ejemplo, una encima de otra), ese intento se
 * descarta y se prueba de nuevo.
 *
 * El resultado depende solo de la plantilla, la dificultad y la semilla: la misma
 * terna da siempre el mismo nivel, en cualquier dispositivo.
 *
 * @param faceCount dibujos distintos disponibles en el juego de fichas.
 */
class LevelGenerator(private val faceCount: Int = DEFAULT_FACE_COUNT) {

    init {
        require(faceCount > 0) { "faceCount debe ser positivo" }
    }

    fun generate(layout: Layout, difficulty: Difficulty, seed: Long): GeneratedLevel {
        val pairCount = layout.size / 2
        val pairsPerFace = difficulty.copiesPerFace / 2
        val facesNeeded = (pairCount + pairsPerFace - 1) / pairsPerFace
        require(facesNeeded <= faceCount) {
            "'${layout.id}' en $difficulty necesita $facesNeeded dibujos y solo hay $faceCount"
        }

        val random = SeededRandom(seed)
        val geometry = BoardGeometry(layout.positions)

        // Se generan varios candidatos y se elige el que más se acerca a la dificultad buscada.
        // En caso de empate gana el primero, para que el resultado no dependa de nada más.
        var chosen: Candidate? = null
        var chosenDistance = Double.MAX_VALUE
        repeat(difficulty.candidates) {
            val candidate = buildCandidate(layout, geometry, pairsPerFace, difficulty, random.fork())
            val distance = abs(candidate.randomWinRate - difficulty.targetRandomWinRate)
            if (distance < chosenDistance) {
                chosen = candidate
                chosenDistance = distance
            }
        }

        val result = checkNotNull(chosen)
        return GeneratedLevel(layout.id, difficulty, seed, result.board, result.solution, result.randomWinRate)
    }

    private class Candidate(val board: Board, val solution: List<Move>, val randomWinRate: Double)

    private fun buildCandidate(
        layout: Layout,
        geometry: BoardGeometry,
        pairsPerFace: Int,
        difficulty: Difficulty,
        random: SeededRandom,
    ): Candidate {
        val solution = dismantle(layout, geometry, random)
        val pairFaces = dealFaces(solution.size, pairsPerFace, random)

        val faces = IntArray(layout.size)
        solution.forEachIndexed { index, move ->
            faces[move.first] = pairFaces[index]
            faces[move.second] = pairFaces[index]
        }
        val board = Board.of(layout, faces.asList())

        // Con un solo par por dibujo no hay forma de equivocarse: no hace falta medirlo.
        val winRate = if (difficulty.copiesPerFace == 2) {
            1.0
        } else {
            RandomPlayer.winRate(board, PLAYOUTS_PER_CANDIDATE, random.fork())
        }
        return Candidate(board, solution, winRate)
    }

    /** Paso 1: vaciar la plantilla sin dibujos y devolver el orden en que salieron los pares. */
    private fun dismantle(layout: Layout, geometry: BoardGeometry, random: SeededRandom): List<Move> {
        repeat(MAX_DISMANTLE_ATTEMPTS) {
            Dismantler.tryDismantle(geometry, BooleanArray(geometry.size) { true }, random)?.let { return it }
        }
        // Solo ocurre con plantillas que no se pueden vaciar (p. ej. una torre de dos fichas).
        // Las pruebas comprueban que ninguna plantilla del catálogo llega aquí.
        error("No se pudo vaciar la plantilla '${layout.id}' en $MAX_DISMANTLE_ATTEMPTS intentos")
    }

    /** Paso 2: un dibujo por par, usando cada dibujo [pairsPerFace] veces. */
    private fun dealFaces(pairCount: Int, pairsPerFace: Int, random: SeededRandom): List<Int> {
        val shuffledFaces = (0 until faceCount).toMutableList()
        random.shuffle(shuffledFaces)
        val pairFaces = MutableList(pairCount) { shuffledFaces[it / pairsPerFace] }
        random.shuffle(pairFaces)
        return pairFaces
    }

    companion object {
        /** Dibujos del juego de fichas del MVP. */
        const val DEFAULT_FACE_COUNT = 36

        /**
         * Versión del algoritmo. Súbela si un cambio hace que la misma semilla dé otro
         * tablero. Las partidas en curso no dependen de ella: se guardan con los dibujos
         * de cada ficha, no con la semilla (ver `GameSnapshot`).
         */
        const val VERSION = 1

        private const val MAX_DISMANTLE_ATTEMPTS = 200
        private const val PLAYOUTS_PER_CANDIDATE = 40
    }
}
