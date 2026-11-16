package azulejo.core

import azulejo.core.board.Board
import azulejo.core.board.Move
import azulejo.core.board.MoveCheck
import azulejo.core.board.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardTest {

    private fun board(vararg tiles: Pair<Position, Int>): Board =
        Board.of(tiles.map { it.first }, tiles.map { it.second })

    private fun p(x: Int, y: Int, z: Int = 0) = Position(x, y, z)

    // ---------- Regla de ficha libre ----------

    @Test
    fun `en una fila solo estan libres los extremos`() {
        val board = board(p(0, 0) to 1, p(2, 0) to 1, p(4, 0) to 1, p(6, 0) to 1)
        assertTrue(board.isFree(0))
        assertFalse(board.isFree(1))
        assertFalse(board.isFree(2))
        assertTrue(board.isFree(3))
    }

    @Test
    fun `una vecina desplazada media ficha tambien bloquea el lado`() {
        // La ficha 1 tiene a la izquierda una vecina medio escalón más abajo y a la derecha otra medio más arriba.
        val board = board(p(0, 2) to 1, p(2, 1) to 1, p(4, 0) to 1)
        assertFalse(board.isFree(1))
        assertTrue(board.isFree(0))
        assertTrue(board.isFree(2))
    }

    @Test
    fun `las vecinas de arriba y abajo no bloquean`() {
        val board = board(p(0, 0) to 1, p(0, 2) to 1, p(0, 4) to 1)
        assertTrue(board.tiles.all { board.isFree(it.id) })
    }

    @Test
    fun `una vecina en diagonal no bloquea`() {
        // (0,2) y (4,2) están en la fila siguiente a la de la ficha central: no tocan sus lados.
        val board = board(p(0, 2) to 1, p(2, 0) to 1, p(4, 2) to 1)
        assertTrue(board.isFree(1))
    }

    @Test
    fun `una ficha con otra encima no esta libre`() {
        val board = board(p(0, 0) to 1, p(0, 0, 1) to 1)
        assertFalse(board.isFree(0))
        assertTrue(board.isFree(1))
    }

    @Test
    fun `una ficha apoyada en cuatro las tapa a todas`() {
        val board = board(
            p(0, 0) to 1, p(2, 0) to 1, p(0, 2) to 2, p(2, 2) to 2,
            p(1, 1, 1) to 3, p(8, 0) to 3,
        )
        for (id in 0..3) assertFalse(board.isFree(id), "la ficha $id debería estar tapada")
        assertTrue(board.isFree(4))

        val uncovered = board.remove(4, 5)
        for (id in 0..3) assertTrue(uncovered.isFree(id), "la ficha $id debería quedar libre")
    }

    @Test
    fun `retirar un extremo libera a la siguiente`() {
        val board = board(p(0, 0) to 1, p(2, 0) to 2, p(4, 0) to 2, p(6, 0) to 1)
        assertFalse(board.isFree(1))
        val next = board.remove(0, 3)
        assertTrue(next.isFree(1))
        assertTrue(next.isFree(2))
    }

    // ---------- Movimientos ----------

    @Test
    fun `check explica por que un par no se puede retirar`() {
        val board = board(p(0, 0) to 1, p(2, 0) to 2, p(4, 0) to 1, p(8, 0) to 2)
        assertEquals(MoveCheck.OK, board.check(0, 2))
        assertEquals(MoveCheck.SAME_TILE, board.check(0, 0))
        assertEquals(MoveCheck.NOT_FREE, board.check(1, 3))
        assertEquals(MoveCheck.DIFFERENT_FACE, board.check(0, 3))
        assertEquals(MoveCheck.NOT_ON_BOARD, board.check(0, 99))

        val next = board.remove(0, 2)
        assertEquals(MoveCheck.NOT_ON_BOARD, next.check(0, 2))
        assertEquals(MoveCheck.OK, next.check(1, 3))
    }

    @Test
    fun `remove rechaza los movimientos ilegales`() {
        val board = board(p(0, 0) to 1, p(2, 0) to 2, p(4, 0) to 1, p(8, 0) to 2)
        assertFailsWith<IllegalArgumentException> { board.remove(1, 3) }
        assertFailsWith<IllegalArgumentException> { board.remove(0, 3) }
        assertFailsWith<IllegalArgumentException> { Move.of(2, 2) }
    }

    @Test
    fun `remove no modifica el tablero original`() {
        val board = board(p(0, 0) to 1, p(4, 0) to 1)
        val next = board.remove(0, 1)
        assertEquals(2, board.remainingCount)
        assertTrue(board.isOnBoard(0))
        assertEquals(0, next.remainingCount)
        assertTrue(next.isCleared)
        assertFalse(board.isCleared)
    }

    @Test
    fun `availableMoves lista todos los pares posibles`() {
        // Tres fichas libres del dibujo 1 dan tres pares; el dibujo 2 no tiene pareja libre.
        val board = board(p(0, 0) to 1, p(4, 0) to 1, p(8, 0) to 1, p(12, 0) to 2)
        assertEquals(
            listOf(Move.of(0, 1), Move.of(0, 2), Move.of(1, 2)),
            board.availableMoves(),
        )
    }

    @Test
    fun `un tablero sin pares disponibles esta bloqueado`() {
        val stacked = board(p(0, 0) to 1, p(0, 0, 1) to 1)
        assertTrue(stacked.isStuck)
        assertFalse(stacked.isCleared)

        val playable = board(p(0, 0) to 1, p(4, 0) to 1)
        assertFalse(playable.isStuck)
        assertFalse(playable.remove(0, 1).isStuck)
    }

    @Test
    fun `Move guarda siempre primero el id menor`() {
        assertEquals(Move.of(3, 7), Move.of(7, 3))
        assertEquals(3, Move.of(7, 3).first)
    }
}
