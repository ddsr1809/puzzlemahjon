package azulejo.core

import azulejo.core.board.Board
import azulejo.core.board.Position
import azulejo.core.generator.Difficulty
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.random.SeededRandom
import azulejo.core.solver.RandomPlayer
import azulejo.core.solver.SolveResult
import azulejo.core.solver.Solver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SolverTest {

    private val a = 1
    private val b = 2

    /**
     * Tablero con trampa. Hay cuatro fichas A y dos B:
     *
     * - A1 está encima de B2.
     * - B1 está encima de A4.
     * - A2 y A3 están sueltas.
     *
     * Emparejar A2 con A3 (lo más tentador) deja A1 y B1 sin pareja libre: bloqueo.
     * Hay que usar A1 primero para destapar B2.
     */
    private fun trapBoard(): Board = Board.of(
        positions = listOf(
            Position(0, 0, 0),  // 0: B2
            Position(0, 0, 1),  // 1: A1
            Position(4, 0, 0),  // 2: A2
            Position(8, 0, 0),  // 3: A3
            Position(12, 0, 0), // 4: A4
            Position(12, 0, 1), // 5: B1
        ),
        faces = listOf(b, a, a, a, a, b),
    )

    @Test
    fun `encuentra la solucion aunque exista un movimiento que bloquea`() {
        val board = trapBoard()
        val result = Solver.solve(board)
        assertIs<SolveResult.Solved>(result)
        assertEquals(3, result.moves.size)
        assertTrue(board.play(result.moves).isCleared)
    }

    @Test
    fun `el movimiento tentador de la trampa bloquea el tablero`() {
        val afterMistake = trapBoard().remove(2, 3)
        assertTrue(afterMistake.isStuck)
        assertIs<SolveResult.Unsolvable>(Solver.solve(afterMistake))
    }

    @Test
    fun `un jugador al azar a veces cae en la trampa y a veces no`() {
        val rate = RandomPlayer.winRate(trapBoard(), games = 300, random = SeededRandom(1))
        // Dos de los tres primeros movimientos posibles ganan.
        assertTrue(rate in 0.5..0.8, "tasa de victoria inesperada: $rate")
    }

    @Test
    fun `winRate coincide con jugar partida a partida`() {
        val level = LevelGenerator().generate(LayoutCatalog.portal, Difficulty.MEDIUM, 11)
        val games = 200
        val fast = RandomPlayer.winRate(level.board, games, SeededRandom(5))

        val random = SeededRandom(5)
        val wins = (1..games).count { RandomPlayer.playOnce(level.board, random).isCleared }
        assertEquals(wins.toDouble() / games, fast)
        assertTrue(fast > 0.0 && fast < 1.0, "se esperaba un tablero donde a veces se pierde: $fast")
    }

    @Test
    fun `reconoce un tablero sin solucion`() {
        val stacked = Board.of(
            positions = listOf(Position(0, 0, 0), Position(0, 0, 1)),
            faces = listOf(a, a),
        )
        assertIs<SolveResult.Unsolvable>(Solver.solve(stacked))
    }

    @Test
    fun `un tablero vacio ya esta resuelto`() {
        val cleared = Board.of(
            positions = listOf(Position(0, 0, 0), Position(4, 0, 0)),
            faces = listOf(a, a),
        ).remove(0, 1)
        val result = Solver.solve(cleared)
        assertIs<SolveResult.Solved>(result)
        assertTrue(result.moves.isEmpty())
    }

    @Test
    fun `resuelve desde mitad de partida`() {
        val level = LevelGenerator().generate(LayoutCatalog.rombo, Difficulty.MEDIUM, 7)
        val midGame = level.board.play(level.solution.take(10))
        val result = Solver.solve(midGame)
        assertIs<SolveResult.Solved>(result)
        assertTrue(midGame.play(result.moves).isCleared)
    }

    @Test
    fun `sin presupuesto responde que no lo sabe`() {
        val level = LevelGenerator().generate(LayoutCatalog.rombo, Difficulty.MEDIUM, 7)
        assertIs<SolveResult.Unknown>(Solver.solve(level.board, nodeBudget = 0))
    }
}
