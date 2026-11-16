package azulejo.core.session

import azulejo.core.board.Move
import azulejo.core.board.Position

/**
 * Foto de una partida en datos simples, para guardarla y recuperarla.
 *
 * Guarda los dibujos de cada ficha y no la semilla: así una partida a medias se recupera
 * igual aunque cambie el generador de niveles en una versión futura.
 *
 * El «tablero base» es el del principio del nivel o, si se ha barajado, el que dejó la
 * última barajada. Los pares de [moves] se retiraron a partir de él y son lo que deshacer
 * puede devolver.
 *
 * @property initialFaces dibujo de cada ficha al empezar el nivel, para reiniciar.
 * @property faces dibujo de cada ficha ahora; cambia respecto a [initialFaces] al barajar.
 * @property basePresent qué fichas había en el tablero base, en orden de id.
 * @property moves pares retirados desde el tablero base, del primero al último.
 */
data class GameSnapshot(
    val positions: List<Position>,
    val initialFaces: List<Int>,
    val faces: List<Int>,
    val basePresent: List<Boolean>,
    val moves: List<Move>,
    val selected: Int?,
    val hint: Move?,
    val hintsLeft: Int,
    val shufflesLeft: Int,
)

/**
 * Convierte un [GameSnapshot] en texto y al revés.
 *
 * El formato es de líneas `clave=valor`, legible y sin dependencias, para que lo pueda
 * escribir y leer cualquier plataforma:
 *
 * ```
 * azulejo-partida 1
 * positions=0.0.0 2.0.0 4.0.0 6.0.0
 * initial=1 2 2 1
 * faces=1 2 2 1
 * base=1111
 * moves=0-3
 * selected=
 * hint=1-2
 * hints=2
 * shuffles=1
 * ```
 *
 * Si el formato cambia de forma incompatible, sube [FORMAT]: las partidas antiguas se
 * descartan al leerlas y el juego empieza una nueva.
 */
object GameSnapshotCodec {

    const val FORMAT = 1

    private const val HEADER = "azulejo-partida"

    fun encode(snapshot: GameSnapshot): String = buildString {
        append(HEADER).append(' ').append(FORMAT).append('\n')
        line("positions", snapshot.positions.joinToString(" ") { "${it.x}.${it.y}.${it.z}" })
        line("initial", snapshot.initialFaces.joinToString(" "))
        line("faces", snapshot.faces.joinToString(" "))
        line("base", snapshot.basePresent.joinToString("") { if (it) "1" else "0" })
        line("moves", snapshot.moves.joinToString(" ") { encodeMove(it) })
        line("selected", snapshot.selected?.toString().orEmpty())
        line("hint", snapshot.hint?.let { encodeMove(it) }.orEmpty())
        line("hints", snapshot.hintsLeft.toString())
        line("shuffles", snapshot.shufflesLeft.toString())
    }

    /**
     * Lee el texto de [encode]. Devuelve `null` si no es un guardado de este formato o está
     * dañado; nunca lanza. Las claves desconocidas se ignoran.
     */
    fun decode(text: String): GameSnapshot? {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.firstOrNull() != "$HEADER $FORMAT") return null

        val values = HashMap<String, String>()
        for (line in lines.drop(1)) {
            val separator = line.indexOf('=')
            if (separator <= 0) return null
            values[line.substring(0, separator)] = line.substring(separator + 1)
        }

        val positions = words(values["positions"] ?: return null).map { decodePosition(it) ?: return null }
        val initialFaces = words(values["initial"] ?: return null).map { it.toIntOrNull() ?: return null }
        val faces = words(values["faces"] ?: return null).map { it.toIntOrNull() ?: return null }
        val basePresent = (values["base"] ?: return null).map {
            when (it) {
                '1' -> true
                '0' -> false
                else -> return null
            }
        }
        val moves = words(values["moves"] ?: return null).map { decodeMove(it) ?: return null }
        val selected = (values["selected"] ?: return null).let { if (it.isEmpty()) null else it.toIntOrNull() ?: return null }
        val hint = (values["hint"] ?: return null).let { if (it.isEmpty()) null else decodeMove(it) ?: return null }
        val hintsLeft = values["hints"]?.toIntOrNull() ?: return null
        val shufflesLeft = values["shuffles"]?.toIntOrNull() ?: return null

        return GameSnapshot(positions, initialFaces, faces, basePresent, moves, selected, hint, hintsLeft, shufflesLeft)
    }

    private fun StringBuilder.line(key: String, value: String) {
        append(key).append('=').append(value).append('\n')
    }

    private fun words(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(' ')

    private fun encodeMove(move: Move): String = "${move.first}-${move.second}"

    private fun decodeMove(text: String): Move? {
        val parts = text.split('-')
        if (parts.size != 2) return null
        val a = parts[0].toIntOrNull() ?: return null
        val b = parts[1].toIntOrNull() ?: return null
        return if (a in 0 until b) Move(a, b) else null
    }

    private fun decodePosition(text: String): Position? {
        val parts = text.split('.')
        if (parts.size != 3) return null
        val x = parts[0].toIntOrNull() ?: return null
        val y = parts[1].toIntOrNull() ?: return null
        val z = parts[2].toIntOrNull() ?: return null
        return if (x >= 0 && y >= 0 && z >= 0) Position(x, y, z) else null
    }
}
