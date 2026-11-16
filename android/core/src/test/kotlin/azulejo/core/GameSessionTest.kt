package azulejo.core

import azulejo.core.board.Board
import azulejo.core.board.Move
import azulejo.core.board.MoveCheck
import azulejo.core.board.Position
import azulejo.core.generator.Difficulty
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.random.SeededRandom
import azulejo.core.session.GameEvent
import azulejo.core.session.GameSession
import azulejo.core.session.GameStatus
import azulejo.core.session.HelpAllowance
import azulejo.core.solver.SolveResult
import azulejo.core.solver.Solver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GameSessionTest {

    private val a = 1
    private val b = 2

    /** Fila de cuatro: A B B A. Solo los extremos están libres al principio. */
    private fun row(): Board = Board.of(
        positions = listOf(Position(0, 0, 0), Position(2, 0, 0), Position(4, 0, 0), Position(6, 0, 0)),
        faces = listOf(a, b, b, a),
    )

    /** El tablero con trampa de SolverTest: emparejar 2 con 3 deja el tablero sin solución. */
    private fun trapBoard(): Board = Board.of(
        positions = listOf(
            Position(0, 0, 0), Position(0, 0, 1), Position(4, 0, 0),
            Position(8, 0, 0), Position(12, 0, 0), Position(12, 0, 1),
        ),
        faces = listOf(b, a, a, a, a, b),
    )

    private fun level(difficulty: Difficulty, seed: Long) =
        LevelGenerator().generate(LayoutCatalog.forDifficulty(difficulty).first(), difficulty, seed)

    // ---------- Tocar fichas ----------

    @Test
    fun `seleccionar, deseleccionar y emparejar`() {
        var session = GameSession.start(row())

        var step = session.tap(0)
        assertEquals(GameEvent.Selected(0), step.event)
        assertEquals(0, step.session.selected)

        step = step.session.tap(0)
        assertEquals(GameEvent.Deselected(0), step.event)
        assertNull(step.session.selected)

        session = step.session.tap(0).session
        step = session.tap(3)
        assertEquals(GameEvent.Matched(Move.of(0, 3)), step.event)
        assertNull(step.session.selected)
        assertEquals(2, step.session.board.remainingCount)
        assertEquals(1, step.session.moveCount)
        assertTrue(step.session.canUndo)
    }

    @Test
    fun `una ficha bloqueada no cambia la seleccion`() {
        val session = GameSession.start(row()).tap(0).session
        val step = session.tap(1)
        assertEquals(GameEvent.Blocked(1), step.event)
        assertEquals(0, step.session.selected)
    }

    @Test
    fun `dibujos distintos pasan la seleccion a la ficha nueva`() {
        val board = Board.of(
            positions = listOf(Position(0, 0, 0), Position(4, 0, 0), Position(8, 0, 0), Position(12, 0, 0)),
            faces = listOf(a, b, a, b),
        )
        val step = GameSession.start(board).tap(0).session.tap(1)
        assertEquals(GameEvent.NoMatch(0, 1), step.event)
        assertEquals(1, step.session.selected)
        assertEquals(4, step.session.board.remainingCount)
    }

    @Test
    fun `tocar una ficha retirada o fuera de rango no hace nada`() {
        val session = GameSession.start(row()).tap(0).session.tap(3).session
        assertSame(session, session.tap(0).session)
        assertEquals(GameEvent.Ignored, session.tap(0).event)
        assertEquals(GameEvent.Ignored, session.tap(99).event)
    }

    @Test
    fun `vaciar el tablero da la victoria y bloquea el resto de acciones`() {
        var session = GameSession.start(row())
        for ((x, y) in listOf(0 to 3, 1 to 2)) session = session.tap(x).session.tap(y).session
        assertEquals(GameStatus.WON, session.status)
        assertFalse(session.canUndo)
        assertEquals(GameEvent.Ignored, session.undo().event)
        assertEquals(GameEvent.Ignored, session.requestHint().event)
        assertEquals(GameEvent.Ignored, session.shuffle(1).event)
    }

    @Test
    fun `jugar la solucion del generador gana el nivel`() {
        for (difficulty in Difficulty.entries) {
            val level = level(difficulty, 3)
            var session = GameSession.start(level.board)
            for (move in level.solution) {
                session = session.tap(move.first).session
                val step = session.tap(move.second)
                assertEquals(GameEvent.Matched(move), step.event)
                session = step.session
            }
            assertEquals(GameStatus.WON, session.status, "$difficulty")
        }
    }

    // ---------- Deshacer y reiniciar ----------

    @Test
    fun `deshacer devuelve el tablero anterior paso a paso`() {
        val start = GameSession.start(row())
        val one = start.tap(0).session.tap(3).session
        val two = one.tap(1).session.tap(2).session
        assertEquals(GameStatus.WON, two.status)

        val back = one.undo()
        assertEquals(GameEvent.Undone, back.event)
        assertSame(start.board, back.session.board)
        assertEquals(0, back.session.moveCount)
        assertFalse(back.session.canUndo)
        assertEquals(GameEvent.Ignored, back.session.undo().event)
    }

    @Test
    fun `deshacer borra la seleccion y la pista`() {
        val session = GameSession.start(row()).tap(0).session.tap(3).session.tap(1).session
        assertEquals(1, session.selected)
        val undone = session.undo().session
        assertNull(undone.selected)
        assertNull(undone.hint)
    }

    @Test
    fun `reiniciar vuelve al principio sin devolver las ayudas`() {
        val level = level(Difficulty.MEDIUM, 9)
        var session = GameSession.start(level.board)
        session = session.requestHint().session
        val move = level.solution.first()
        session = session.tap(move.first).session.tap(move.second).session

        val restarted = session.restart()
        assertEquals(GameEvent.Restarted, restarted.event)
        assertSame(level.board, restarted.session.board)
        assertEquals(HelpAllowance.DEFAULT.hints - 1, restarted.session.hintsLeft)
        assertFalse(restarted.session.canUndo)
        assertEquals(0, restarted.session.moveCount)
    }

    // ---------- Pista ----------

    @Test
    fun `la pista marca un par legal que mantiene la solucion`() {
        for (difficulty in Difficulty.entries) {
            for (seed in 1L..10L) {
                val level = level(difficulty, seed)
                val step = GameSession.start(level.board).requestHint()
                val event = assertIs<GameEvent.HintShown>(step.event)
                assertEquals(MoveCheck.OK, level.board.check(event.move))
                assertEquals(event.move, step.session.hint)
                assertEquals(HelpAllowance.DEFAULT.hints - 1, step.session.hintsLeft)
                val after = level.board.remove(event.move)
                assertIs<SolveResult.Solved>(Solver.solve(after), "$difficulty semilla $seed")
            }
        }
    }

    @Test
    fun `la pista evita la trampa`() {
        val step = GameSession.start(trapBoard()).requestHint()
        val event = assertIs<GameEvent.HintShown>(step.event)
        // Emparejar 2 con 3 bloquea; cualquier otro par libre es seguro.
        assertTrue(event.move != Move.of(2, 3), "la pista llevó a la trampa")
    }

    @Test
    fun `pedir otra pista con la anterior en pantalla no gasta`() {
        val first = GameSession.start(trapBoard()).requestHint().session
        val second = first.requestHint()
        assertEquals(GameEvent.HintShown(first.hint!!), second.event)
        assertEquals(first.hintsLeft, second.session.hintsLeft)
    }

    @Test
    fun `sin pistas restantes avisa y no cambia nada`() {
        val session = GameSession.start(row(), HelpAllowance(hints = 0, shuffles = 0))
        val step = session.requestHint()
        assertEquals(GameEvent.NoHintsLeft, step.event)
        assertSame(session, step.session)
    }

    @Test
    fun `tras un error sin vuelta atras la pista pide deshacer y no gasta`() {
        // Tras 2 con 3 quedan pares sin usar, pero todos tapados: el tablero queda bloqueado.
        val mistake = GameSession.start(trapBoard()).tap(2).session.tap(3).session
        assertEquals(GameStatus.STUCK, mistake.status)
        assertEquals(GameEvent.NoMovesLeft, mistake.requestHint().event)

        // Caso con pares disponibles pero sin solución: A1 sobre B1, A2 suelta, B2 sobre A3, A4 suelta.
        // Emparejar A2 con A4 deja A1 y B2 encima de las únicas parejas que les quedan.
        val board = Board.of(
            positions = listOf(
                Position(0, 0, 0), Position(0, 0, 1), Position(4, 0, 0),
                Position(8, 0, 0), Position(8, 0, 1), Position(12, 0, 0),
                Position(16, 0, 0), Position(20, 0, 0),
            ),
            faces = listOf(b, a, a, a, b, a, 3, 3),
        )
        val wrong = GameSession.start(board).tap(2).session.tap(5).session
        assertEquals(GameStatus.PLAYING, wrong.status)
        val step = wrong.requestHint()
        assertEquals(GameEvent.NeedsUndo, step.event)
        assertEquals(wrong.hintsLeft, step.session.hintsLeft)
    }

    @Test
    fun `tocar una ficha borra la pista al emparejar`() {
        val hinted = GameSession.start(row()).requestHint().session
        val move = hinted.hint!!
        val matched = hinted.tap(move.first).session.tap(move.second).session
        assertNull(matched.hint)
    }

    // ---------- Barajar ----------

    @Test
    fun `barajar conserva fichas y dibujos y deja el tablero con solucion`() {
        for (difficulty in Difficulty.entries) {
            for (seed in 1L..15L) {
                val level = level(difficulty, seed)
                // Avanza unos pares para barajar a mitad de partida.
                var session = GameSession.start(level.board, HelpAllowance(hints = 0, shuffles = 5))
                for (move in level.solution.take(6)) session = session.tap(move.first).session.tap(move.second).session

                val step = session.shuffle(seed * 31)
                assertEquals(GameEvent.Shuffled, step.event)
                val before = session.board
                val after = step.session.board

                assertTrue(before.presence().contentEquals(after.presence()), "cambiaron las fichas presentes")
                assertEquals(
                    before.remainingTiles().map { it.face }.sorted(),
                    after.remainingTiles().map { it.face }.sorted(),
                    "cambiaron los dibujos",
                )
                assertIs<SolveResult.Solved>(Solver.solve(after), "$difficulty semilla $seed")
                assertEquals(4, step.session.shufflesLeft)
                assertFalse(step.session.canUndo)
            }
        }
    }

    @Test
    fun `barajar saca de un bloqueo`() {
        val stuck = GameSession.start(trapBoard()).tap(2).session.tap(3).session
        assertEquals(GameStatus.STUCK, stuck.status)
        for (seed in 1L..20L) {
            val step = stuck.shuffle(seed)
            assertEquals(GameEvent.Shuffled, step.event)
            assertIs<SolveResult.Solved>(Solver.solve(step.session.board))
            assertEquals(GameStatus.PLAYING, step.session.status)
        }
    }

    @Test
    fun `sin barajadas avisa y no cambia nada`() {
        val session = GameSession.start(row(), HelpAllowance(hints = 3, shuffles = 0))
        val step = session.shuffle(1)
        assertEquals(GameEvent.NoShufflesLeft, step.event)
        assertSame(session, step.session)
    }

    @Test
    fun `barajar fichas que no se pueden retirar no gasta la barajada`() {
        // Dos pares apilados en la misma columna: nunca hay dos fichas libres a la vez.
        val tower = Board.of(
            positions = listOf(Position(0, 0, 0), Position(0, 0, 1), Position(0, 0, 2), Position(0, 0, 3)),
            faces = listOf(a, b, a, b),
        )
        val session = GameSession.start(tower)
        assertEquals(GameStatus.STUCK, session.status)
        val step = session.shuffle(1)
        assertEquals(GameEvent.ShuffleImpossible, step.event)
        assertEquals(session.shufflesLeft, step.session.shufflesLeft)
    }

    @Test
    fun `la misma semilla baraja igual`() {
        val level = level(Difficulty.HARD, 4)
        val session = GameSession.start(level.board)
        assertEquals(
            session.shuffle(77).session.board.tiles.map { it.face },
            session.shuffle(77).session.board.tiles.map { it.face },
        )
    }

    @Test
    fun `grant suma ayudas`() {
        val session = GameSession.start(row(), HelpAllowance(hints = 0, shuffles = 0)).grant(hints = 2, shuffles = 1)
        assertEquals(2, session.hintsLeft)
        assertEquals(1, session.shufflesLeft)
    }

    // ---------- Partidas al azar ----------

    /**
     * Juega cientos de acciones al azar (tocar, deshacer, pista, barajar, reiniciar) y comprueba
     * que la sesión nunca queda en un estado incoherente.
     */
    @Test
    fun `acciones al azar nunca rompen la sesion`() {
        for (difficulty in Difficulty.entries) {
            for (seed in 1L..8L) {
                val level = level(difficulty, seed)
                val random = SeededRandom(seed)
                var session = GameSession.start(level.board, HelpAllowance(hints = 20, shuffles = 20))
                repeat(400) {
                    val step = when (random.nextInt(10)) {
                        0 -> session.undo()
                        1 -> session.requestHint(nodeBudget = 20_000)
                        2 -> session.shuffle(random.nextLong())
                        3 -> if (random.nextInt(10) == 0) session.restart() else session.undo()
                        else -> {
                            val free = session.board.freeTiles()
                            if (free.isEmpty()) session.undo() else session.tap(free[random.nextInt(free.size)].id)
                        }
                    }
                    session = step.session
                    val board = session.board

                    assertTrue(board.remainingCount % 2 == 0)
                    assertEquals(board.remainingCount, board.remainingTiles().size)
                    session.selected?.let { assertTrue(board.isFree(it), "seleccionada una ficha no libre") }
                    session.hint?.let { assertEquals(MoveCheck.OK, board.check(it)) }
                    if (step.event == GameEvent.Shuffled) assertIs<SolveResult.Solved>(Solver.solve(board))
                    if (session.status == GameStatus.WON) session = session.restart().session
                }
            }
        }
    }
}
