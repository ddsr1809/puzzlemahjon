package azulejo.app.data

import azulejo.core.generator.Difficulty
import azulejo.core.generator.LevelGenerator
import azulejo.core.layouts.LayoutCatalog
import azulejo.core.session.GameSession
import azulejo.core.session.GameStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppStorageTest {

    /** Almacén en memoria que hace de preferencias de Android. */
    private class MemoryStore : KeyValueStore {
        val values = HashMap<String, String>()
        var writes = 0

        override fun read(key: String): String? = values[key]

        override fun write(key: String, value: String?) {
            writes++
            if (value == null) values.remove(key) else values[key] = value
        }
    }

    private val store = MemoryStore()
    private val storage = AppStorage(store)

    private fun level(difficulty: Difficulty, seed: Long, compact: Boolean = false) =
        LevelGenerator().generate(LayoutCatalog.forDifficulty(difficulty, compact).first(), difficulty, seed)

    // ---------- Ajustes ----------

    @Test
    fun `la primera vez salen los ajustes por defecto`() {
        assertEquals(Settings(), storage.loadSettings())
        assertEquals(Settings(TileSize.NORMAL, highContrast = false, language = AppLanguage.SYSTEM), Settings())
        assertEquals(Difficulty.EASY, storage.loadDifficulty())
        assertNull(storage.loadGame())
    }

    @Test
    fun `los ajustes se guardan y se recuperan en todas sus combinaciones`() {
        for (tileSize in TileSize.entries) {
            for (highContrast in listOf(false, true)) {
                for (language in AppLanguage.entries) {
                    val settings = Settings(tileSize, highContrast, language)
                    storage.saveSettings(settings)
                    assertEquals(settings, AppStorage(store).loadSettings())
                }
            }
        }
    }

    @Test
    fun `un valor guardado que no se entiende vuelve al valor por defecto`() {
        storage.saveSettings(Settings(TileSize.LARGE, highContrast = true, language = AppLanguage.SPANISH))
        store.values[AppStorage.KEY_TILE_SIZE] = "enorme"
        store.values[AppStorage.KEY_HIGH_CONTRAST] = "quizá"
        store.values[AppStorage.KEY_LANGUAGE] = "fr"
        store.values[AppStorage.KEY_DIFFICULTY] = "imposible"
        assertEquals(Settings(), storage.loadSettings())
        assertEquals(Difficulty.EASY, storage.loadDifficulty())
    }

    /**
     * Claves y valores tal como quedan en el teléfono. Si esta prueba falla, una actualización
     * dejaría de leer los ajustes de quien ya tiene la app instalada.
     */
    @Test
    fun `las claves y los valores guardados no cambian entre versiones`() {
        storage.saveSettings(Settings(TileSize.LARGE, highContrast = true, language = AppLanguage.SPANISH))
        storage.saveDifficulty(Difficulty.HARD)
        assertEquals(
            mapOf(
                "settings.tileSize" to "large",
                "settings.highContrast" to "on",
                "settings.language" to "es",
                "game.difficulty" to "hard",
            ),
            store.values,
        )
        storage.saveSettings(Settings())
        storage.saveDifficulty(Difficulty.MEDIUM)
        assertEquals("normal", store.values["settings.tileSize"])
        assertEquals("off", store.values["settings.highContrast"])
        assertEquals("system", store.values["settings.language"])
        assertEquals("medium", store.values["game.difficulty"])
        assertEquals("easy", Difficulty.EASY.key)
        assertEquals(listOf(null, "en", "es"), AppLanguage.entries.map { it.tag })
        assertEquals("game.snapshot", AppStorage.KEY_GAME)
    }

    // ---------- Partida ----------

    @Test
    fun `cerrar la app a mitad de partida y recuperarla`() {
        val level = level(Difficulty.HARD, 12)
        var session = GameSession.start(level.board)
        for (move in level.solution.take(9)) session = session.tap(move.first).session.tap(move.second).session
        session = session.requestHint().session
        storage.saveGame(Difficulty.HARD, session)

        // Otra instancia sobre el mismo almacén: es lo que ocurre al abrir la app de nuevo.
        val loaded = assertNotNull(AppStorage(store).loadGame())
        assertEquals(Difficulty.HARD, loaded.difficulty)
        assertEquals(session.snapshot(), loaded.session.snapshot())
        assertEquals(level.board.tiles.size - 18, loaded.session.board.remainingCount)
        assertEquals(2, loaded.session.hintsLeft)
        assertTrue(loaded.session.canUndo)

        // Y se puede terminar la partida recuperada.
        var resumed = loaded.session
        for (move in level.solution.drop(9)) resumed = resumed.tap(move.first).session.tap(move.second).session
        assertEquals(GameStatus.WON, resumed.status)
    }

    @Test
    fun `las partidas con fichas grandes tambien se recuperan`() {
        for (difficulty in Difficulty.entries) {
            val session = GameSession.start(level(difficulty, 3, compact = true).board)
            storage.saveGame(difficulty, session)
            val loaded = assertNotNull(storage.loadGame())
            assertEquals(difficulty, loaded.difficulty)
            assertEquals(session.snapshot(), loaded.session.snapshot())
        }
    }

    @Test
    fun `una partida ganada no se guarda y borra la anterior`() {
        val level = level(Difficulty.EASY, 4)
        var session = GameSession.start(level.board)
        storage.saveGame(Difficulty.EASY, session)
        assertNotNull(storage.loadGame())

        for (move in level.solution) session = session.tap(move.first).session.tap(move.second).session
        assertEquals(GameStatus.WON, session.status)
        storage.saveGame(Difficulty.EASY, session)

        assertNull(storage.loadGame())
        assertFalse(AppStorage.KEY_GAME in store.values)
        // La dificultad sí se recuerda, para la partida siguiente.
        assertEquals(Difficulty.EASY, storage.loadDifficulty())
    }

    @Test
    fun `una partida danada se descarta y se borra`() {
        storage.saveGame(Difficulty.MEDIUM, GameSession.start(level(Difficulty.MEDIUM, 8).board))
        val good = store.values.getValue(AppStorage.KEY_GAME)

        val damaged = listOf(
            "",
            "esto no es una partida",
            good.take(good.length / 2),
            good.replace("azulejo-partida 1", "azulejo-partida 99"),
            // Un dibujo cambiado deja una ficha sin pareja: el texto se lee, pero no es una partida.
            good.replaceFirst(Regex("faces=\\d+"), "faces=35"),
        )
        for (text in damaged) {
            store.values[AppStorage.KEY_GAME] = text
            assertNull(storage.loadGame(), "debería descartar:\n$text")
            assertFalse(AppStorage.KEY_GAME in store.values, "la partida dañada sigue guardada")
        }
        assertEquals(Difficulty.MEDIUM, storage.loadDifficulty())
    }

    @Test
    fun `borrar la partida no toca los ajustes`() {
        val settings = Settings(TileSize.LARGE, highContrast = true, language = AppLanguage.ENGLISH)
        storage.saveSettings(settings)
        storage.saveGame(Difficulty.HARD, GameSession.start(level(Difficulty.HARD, 2).board))
        storage.clearGame()
        assertNull(storage.loadGame())
        assertEquals(settings, storage.loadSettings())
        assertEquals(Difficulty.HARD, storage.loadDifficulty())
    }
}
