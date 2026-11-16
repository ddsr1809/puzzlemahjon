package azulejo.core

import azulejo.core.board.Board
import azulejo.core.board.Move
import azulejo.core.board.Position
import azulejo.core.generator.Difficulty
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.random.SeededRandom
import azulejo.core.session.GameEvent
import azulejo.core.session.GameSession
import azulejo.core.session.GameSnapshot
import azulejo.core.session.GameSnapshotCodec
import azulejo.core.session.GameStatus
import azulejo.core.session.HelpAllowance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameSnapshotTest {

    private val a = 1
    private val b = 2

    /** Fila de cuatro: A B B A. Solo los extremos están libres al principio. */
    private fun row(): Board = Board.of(
        positions = listOf(Position(0, 0, 0), Position(2, 0, 0), Position(4, 0, 0), Position(6, 0, 0)),
        faces = listOf(a, b, b, a),
    )

    private fun level(difficulty: Difficulty, seed: Long) =
        LevelGenerator().generate(LayoutCatalog.forDifficulty(difficulty).first(), difficulty, seed)

    /** Guarda como texto y vuelve a leer: el camino completo que hace la app. */
    private fun saveAndLoad(session: GameSession): GameSession {
        val snapshot = assertNotNull(GameSnapshotCodec.decode(GameSnapshotCodec.encode(session.snapshot())))
        return assertNotNull(GameSession.restore(snapshot))
    }

    private fun assertSameGame(expected: GameSession, actual: GameSession) {
        assertEquals(expected.snapshot(), actual.snapshot())
        assertEquals(expected.status, actual.status)
        assertEquals(expected.moveCount, actual.moveCount)
        assertEquals(expected.canUndo, actual.canUndo)
        assertTrue(expected.board.presence().contentEquals(actual.board.presence()))
    }

    // ---------- Guardar y recuperar ----------

    @Test
    fun `una partida recien empezada se recupera igual`() {
        val session = GameSession.start(level(Difficulty.MEDIUM, 3).board)
        val loaded = saveAndLoad(session)
        assertSameGame(session, loaded)
        assertFalse(loaded.canUndo)
        assertEquals(HelpAllowance.DEFAULT.hints, loaded.hintsLeft)
        assertEquals(HelpAllowance.DEFAULT.shuffles, loaded.shufflesLeft)
    }

    @Test
    fun `una partida a medias conserva pares, seleccion, pista y ayudas`() {
        val level = level(Difficulty.HARD, 7)
        var session = GameSession.start(level.board)
        for (move in level.solution.take(5)) session = session.tap(move.first).session.tap(move.second).session
        session = session.requestHint().session
        val free = session.board.freeTiles().first().id
        session = session.tap(free).session

        val loaded = saveAndLoad(session)
        assertSameGame(session, loaded)
        assertEquals(5, loaded.moveCount)
        assertEquals(free, loaded.selected)
        assertEquals(session.hint, loaded.hint)
        assertEquals(2, loaded.hintsLeft)
    }

    @Test
    fun `tras recuperar se puede deshacer hasta el principio`() {
        val level = level(Difficulty.MEDIUM, 11)
        var session = GameSession.start(level.board)
        for (move in level.solution.take(8)) session = session.tap(move.first).session.tap(move.second).session

        var loaded = saveAndLoad(session)
        repeat(8) {
            session = session.undo().session
            loaded = loaded.undo().session
            assertSameGame(session, loaded)
        }
        assertFalse(loaded.canUndo)
        assertEquals(level.board.tiles.size, loaded.board.remainingCount)
    }

    @Test
    fun `tras recuperar la solucion del generador sigue ganando`() {
        val level = level(Difficulty.HARD, 5)
        var session = GameSession.start(level.board)
        for (move in level.solution.take(10)) session = session.tap(move.first).session.tap(move.second).session

        var loaded = saveAndLoad(session)
        for (move in level.solution.drop(10)) loaded = loaded.tap(move.first).session.tap(move.second).session
        assertEquals(GameStatus.WON, loaded.status)
    }

    @Test
    fun `una partida barajada conserva el reparto nuevo y el tablero inicial`() {
        val level = level(Difficulty.MEDIUM, 9)
        var session = GameSession.start(level.board)
        for (move in level.solution.take(6)) session = session.tap(move.first).session.tap(move.second).session
        val shuffled = session.shuffle(1234)
        assertEquals(GameEvent.Shuffled, shuffled.event)
        session = shuffled.session
        assertNotEquals(session.snapshot().initialFaces, session.snapshot().faces)

        // Dos pares más después de barajar: son los únicos que deshacer puede devolver.
        repeat(2) {
            val move = session.board.availableMoves().first()
            session = session.tap(move.first).session.tap(move.second).session
        }

        var loaded = saveAndLoad(session)
        assertSameGame(session, loaded)
        assertEquals(0, loaded.shufflesLeft)
        assertEquals(2, loaded.moveCount)

        loaded = loaded.undo().session.undo().session
        assertFalse(loaded.canUndo, "no se puede deshacer hasta antes de barajar")
        assertEquals(level.board.tiles.size - 12, loaded.board.remainingCount)

        // Reiniciar vuelve al reparto original, no al barajado.
        val restarted = loaded.restart().session
        assertEquals(level.faces, restarted.board.tiles.map { it.face })
        assertEquals(level.board.tiles.size, restarted.board.remainingCount)
    }

    @Test
    fun `una partida ganada se recupera como ganada`() {
        var session = GameSession.start(row())
        session = session.tap(0).session.tap(3).session.tap(1).session.tap(2).session
        assertEquals(GameStatus.WON, session.status)
        assertEquals(GameStatus.WON, saveAndLoad(session).status)
    }

    /**
     * Juega acciones al azar y guarda y recupera después de cada una: la partida recuperada
     * tiene que ser indistinguible de la que estaba en memoria.
     */
    @Test
    fun `guardar tras cualquier accion recupera la misma partida`() {
        for (difficulty in Difficulty.entries) {
            for (seed in 1L..6L) {
                val random = SeededRandom(seed)
                var session = GameSession.start(level(difficulty, seed).board, HelpAllowance(hints = 10, shuffles = 10))
                repeat(250) {
                    session = when (random.nextInt(12)) {
                        0, 1 -> session.undo()
                        2 -> session.requestHint(nodeBudget = 20_000)
                        3 -> session.shuffle(random.nextLong())
                        4 -> if (random.nextInt(8) == 0) session.restart() else session.undo()
                        else -> {
                            val free = session.board.freeTiles()
                            if (free.isEmpty()) session.undo() else session.tap(free[random.nextInt(free.size)].id)
                        }
                    }.session

                    val loaded = saveAndLoad(session)
                    assertSameGame(session, loaded)
                    // Se sigue jugando con la partida recuperada, como hará la app.
                    session = if (loaded.status == GameStatus.WON) loaded.restart().session else loaded
                }
            }
        }
    }

    // ---------- Formato ----------

    /**
     * Texto exacto de una partida conocida. Si falla sin que hayas querido cambiar el formato,
     * las partidas guardadas por versiones anteriores dejarían de leerse: deshaz el cambio o
     * sube [GameSnapshotCodec.FORMAT].
     */
    @Test
    fun `el formato de guardado no cambia entre versiones`() {
        assertEquals(1, GameSnapshotCodec.FORMAT)
        val session = GameSession.start(row()).tap(0).session.tap(3).session.requestHint().session
        val expected = """
            azulejo-partida 1
            positions=0.0.0 2.0.0 4.0.0 6.0.0
            initial=1 2 2 1
            faces=1 2 2 1
            base=1111
            moves=0-3
            selected=
            hint=1-2
            hints=2
            shuffles=1
        """.trimIndent() + "\n"
        assertEquals(expected, GameSnapshotCodec.encode(session.snapshot()))
        assertEquals(session.snapshot(), GameSnapshotCodec.decode(expected))
    }

    @Test
    fun `las claves desconocidas y los saltos de linea de Windows se ignoran`() {
        val text = GameSnapshotCodec.encode(GameSession.start(row()).snapshot())
        val extended = text.replace("\n", "\r\n") + "futuro=algo\r\n"
        assertEquals(GameSnapshotCodec.decode(text), GameSnapshotCodec.decode(extended))
        assertNotNull(GameSnapshotCodec.decode(extended))
    }

    @Test
    fun `un texto que no es un guardado valido se rechaza sin lanzar`() {
        val good = GameSnapshotCodec.encode(GameSession.start(row()).tap(0).session.snapshot())
        assertNotNull(GameSnapshotCodec.decode(good))

        val bad = listOf(
            "",
            "hola",
            good.replace("azulejo-partida 1", "azulejo-partida 2"),
            good.replace("azulejo-partida 1", "otra-cosa 1"),
            good.replace("positions=0.0.0", "positions=0.0"),
            good.replace("positions=0.0.0", "positions=-2.0.0"),
            good.replace("initial=1 2", "initial=1 x"),
            good.replace("base=1111", "base=1121"),
            good.replace("moves=", "moves=3-0"),
            good.replace("moves=", "moves=3"),
            good.replace("selected=0", "selected=cero"),
            good.replace("hint=", "hint=1-1"),
            good.replace("hints=3", "hints=tres"),
            good.replace("shuffles=1\n", ""),
            good.replace("faces=1 2 2 1\n", ""),
            good.replace("hints=3", "sin separador"),
        )
        for (text in bad) assertNull(GameSnapshotCodec.decode(text), "debería rechazar:\n$text")

        // Cortar el texto por cualquier punto nunca lanza, y solo se acepta si está entero.
        for (length in 0 until good.length - 1) {
            assertNull(GameSnapshotCodec.decode(good.substring(0, length)), "aceptó un guardado cortado en $length")
        }
    }

    // ---------- Fotos imposibles ----------

    private fun valid(): GameSnapshot = GameSession.start(row()).tap(0).session.tap(3).session.snapshot()

    @Test
    fun `restore rechaza fotos que no describen una partida`() {
        assertNotNull(GameSession.restore(valid()))

        val impossible = listOf(
            // Tamaños que no cuadran.
            valid().copy(faces = listOf(a, b, b)),
            valid().copy(initialFaces = listOf(a, b, b, a, a, a)),
            valid().copy(basePresent = listOf(true, true)),
            GameSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), null, null, 3, 1),
            // Dos fichas en el mismo sitio.
            valid().copy(positions = listOf(Position(0, 0, 0), Position(0, 0, 0), Position(4, 0, 0), Position(6, 0, 0))),
            // Dibujos sin pareja o negativos.
            valid().copy(faces = listOf(a, b, a, a)),
            valid().copy(initialFaces = listOf(a, a, a, b)),
            valid().copy(faces = listOf(-1, b, b, -1)),
            valid().copy(basePresent = listOf(true, true, true, false)),
            // Pares que no se pudieron retirar: ficha bloqueada, dibujos distintos, repetido, fuera de rango.
            valid().copy(moves = listOf(Move(1, 2))),
            valid().copy(moves = listOf(Move(0, 1))),
            valid().copy(moves = listOf(Move(0, 3), Move(0, 3))),
            valid().copy(moves = listOf(Move(0, 9))),
            // Ayudas negativas.
            valid().copy(hintsLeft = -1),
            valid().copy(shufflesLeft = -1),
        )
        for (snapshot in impossible) assertNull(GameSession.restore(snapshot), "debería rechazar $snapshot")
    }

    @Test
    fun `una seleccion o una pista que ya no valen se descartan sin perder la partida`() {
        // Tras retirar 0 y 3 quedan 1 y 2, las dos libres.
        val withStale = valid().copy(selected = 0, hint = Move(0, 3))
        val restored = assertNotNull(GameSession.restore(withStale))
        assertNull(restored.selected)
        assertNull(restored.hint)
        assertEquals(2, restored.board.remainingCount)

        val outOfRange = assertNotNull(GameSession.restore(valid().copy(selected = 99, hint = Move(1, 99))))
        assertNull(outOfRange.selected)
        assertNull(outOfRange.hint)

        val stillGood = assertNotNull(GameSession.restore(valid().copy(selected = 1, hint = Move(1, 2))))
        assertEquals(1, stillGood.selected)
        assertEquals(Move(1, 2), stillGood.hint)
    }

    @Test
    fun `un tablero a medias solo contiene las fichas marcadas`() {
        val board = Board.of(row().tiles.map { it.position }, listOf(a, b, b, a), listOf(false, true, true, false))
        assertEquals(2, board.remainingCount)
        assertFalse(board.isOnBoard(0))
        assertTrue(board.isFree(1) && board.isFree(2))
        assertEquals(listOf(Move(1, 2)), board.availableMoves())
    }
}
