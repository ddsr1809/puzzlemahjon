package azulejo.app.data

import azulejo.core.generator.Difficulty
import azulejo.core.session.GameSession
import azulejo.core.session.GameSnapshotCodec
import azulejo.core.session.GameStatus

/**
 * Almacén de textos por clave. En el teléfono es [PrefsStore]; en las pruebas, un mapa en
 * memoria.
 */
interface KeyValueStore {
    fun read(key: String): String?

    /** Guarda [value], o borra la clave si es `null`. */
    fun write(key: String, value: String?)
}

/** Partida guardada: la sesión y la dificultad con la que se empezó. */
class SavedGame(val difficulty: Difficulty, val session: GameSession)

/**
 * Lo que el juego guarda en el teléfono: los ajustes, la última dificultad elegida y la
 * partida en curso. Kotlin puro; el acceso al disco está detrás de [KeyValueStore].
 *
 * Leer nunca falla: un valor que falta, está dañado o viene de una versión incompatible se
 * sustituye por el valor por defecto, y una partida que no se puede leer se descarta.
 */
class AppStorage(private val store: KeyValueStore) {

    // ---------- Ajustes ----------

    fun loadSettings(): Settings {
        val defaults = Settings()
        return Settings(
            tileSize = TileSize.entries.firstOrNull { it.key == store.read(KEY_TILE_SIZE) } ?: defaults.tileSize,
            highContrast = when (store.read(KEY_HIGH_CONTRAST)) {
                VALUE_ON -> true
                VALUE_OFF -> false
                else -> defaults.highContrast
            },
            language = AppLanguage.entries.firstOrNull { it.key == store.read(KEY_LANGUAGE) } ?: defaults.language,
        )
    }

    fun saveSettings(settings: Settings) {
        store.write(KEY_TILE_SIZE, settings.tileSize.key)
        store.write(KEY_HIGH_CONTRAST, if (settings.highContrast) VALUE_ON else VALUE_OFF)
        store.write(KEY_LANGUAGE, settings.language.key)
    }

    // ---------- Dificultad ----------

    /** Última dificultad elegida; [Difficulty.EASY] la primera vez. */
    fun loadDifficulty(): Difficulty =
        Difficulty.entries.firstOrNull { it.key == store.read(KEY_DIFFICULTY) } ?: Difficulty.EASY

    fun saveDifficulty(difficulty: Difficulty) {
        store.write(KEY_DIFFICULTY, difficulty.key)
    }

    // ---------- Partida en curso ----------

    /** Guarda la partida. Una partida ya ganada no se guarda: no hay nada que continuar. */
    fun saveGame(difficulty: Difficulty, session: GameSession) {
        saveDifficulty(difficulty)
        if (session.status == GameStatus.WON) {
            clearGame()
        } else {
            store.write(KEY_GAME, GameSnapshotCodec.encode(session.snapshot()))
        }
    }

    /** Partida en curso, o `null` si no hay ninguna que se pueda continuar. */
    fun loadGame(): SavedGame? {
        val text = store.read(KEY_GAME) ?: return null
        val session = GameSnapshotCodec.decode(text)?.let { GameSession.restore(it) }
        if (session == null || session.status == GameStatus.WON) {
            // Dañada, de otra versión o ya terminada: se borra para no tropezar con ella otra vez.
            clearGame()
            return null
        }
        return SavedGame(loadDifficulty(), session)
    }

    fun clearGame() {
        store.write(KEY_GAME, null)
    }

    companion object {
        const val KEY_TILE_SIZE = "settings.tileSize"
        const val KEY_HIGH_CONTRAST = "settings.highContrast"
        const val KEY_LANGUAGE = "settings.language"
        const val KEY_DIFFICULTY = "game.difficulty"
        const val KEY_GAME = "game.snapshot"

        private const val VALUE_ON = "on"
        private const val VALUE_OFF = "off"
    }
}
