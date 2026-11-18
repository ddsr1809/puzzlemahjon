package azulejo.core.progress

/**
 * Los 8 murales de la colección. Cada nivel ganado coloca un azulejo: el nivel 1 es la
 * primera pieza del mural 1 y el nivel 200, la última del mural 8.
 *
 * Cada mural es un panel de 5 × 5 azulejos con simetría de cuadrado, como los paneles de
 * azulejo de verdad: el centro, el anillo interior y el exterior repiten dibujos. Las
 * piezas se colocan por filas, de arriba abajo y de izquierda a derecha.
 *
 * Los dibujos son los mismos 36 de las fichas: `familia * 9 + motivo` (ver `TileArt` en la
 * app). Familias: 0 Esquinas (azul), 1 Medallón (terracota), 2 Rombo (verde), 3 Recuadro
 * (morado). Motivos: 0 estrella, 1 flor, 2 cruz, 3 triángulo, 4 luna, 5 arco, 6 olas,
 * 7 diana, 8 hoja.
 */
object Murals {

    const val COUNT = 8
    const val SIDE = 5
    const val PIECES = SIDE * SIDE

    /** Mural del nivel, de 0 a [COUNT] - 1. */
    fun muralOf(level: Int): Int {
        require(level in 1..LevelCatalog.COUNT) { "No existe el nivel $level" }
        return (level - 1) / PIECES
    }

    /** Pieza del mural que coloca el nivel, de 0 a [PIECES] - 1, por filas. */
    fun pieceOf(level: Int): Int {
        require(level in 1..LevelCatalog.COUNT) { "No existe el nivel $level" }
        return (level - 1) % PIECES
    }

    fun levelOf(mural: Int, piece: Int): Int {
        require(mural in 0 until COUNT && piece in 0 until PIECES) { "Pieza fuera de rango: $mural/$piece" }
        return mural * PIECES + piece + 1
    }

    /** Dibujo del azulejo [piece] del mural [mural]. */
    fun faceAt(mural: Int, piece: Int): Int {
        require(mural in 0 until COUNT && piece in 0 until PIECES) { "Pieza fuera de rango: $mural/$piece" }
        return DESIGNS[mural][ringClass(piece)]
    }

    /**
     * A qué grupo simétrico pertenece una pieza:
     * 0 centro, 1 esquinas del anillo interior, 2 lados del anillo interior,
     * 3 esquinas del borde, 4 piezas del borde junto a una esquina, 5 centros del borde.
     */
    internal fun ringClass(piece: Int): Int {
        val dx = kotlin.math.abs(piece % SIDE - SIDE / 2)
        val dy = kotlin.math.abs(piece / SIDE - SIDE / 2)
        return when {
            dx == 0 && dy == 0 -> 0
            dx == 1 && dy == 1 -> 1
            dx + dy == 1 -> 2
            dx == 2 && dy == 2 -> 3
            dx + dy == 3 -> 4
            else -> 5
        }
    }

    private fun face(family: Int, motif: Int) = family * 9 + motif

    /**
     * Diseño de cada mural: un dibujo por grupo de [ringClass]. Cada mural combina una
     * familia principal con otra de acento. No cambies un diseño publicado: el jugador ya
     * lo ha ido completando.
     */
    private val DESIGNS: List<IntArray> = listOf(
        // 1. Estrella azul: azul con acentos terracota.
        intArrayOf(face(0, 0), face(1, 1), face(0, 2), face(1, 7), face(0, 8), face(0, 5)),
        // 2. Jardín: verde con flores terracota.
        intArrayOf(face(1, 1), face(2, 8), face(2, 1), face(2, 0), face(1, 8), face(2, 6)),
        // 3. Mar: azul con olas y lunas moradas.
        intArrayOf(face(3, 4), face(0, 6), face(0, 7), face(3, 0), face(0, 4), face(3, 6)),
        // 4. Medallones: terracota con verde.
        intArrayOf(face(1, 7), face(2, 2), face(1, 0), face(1, 2), face(2, 3), face(1, 5)),
        // 5. Celosía morada con azul.
        intArrayOf(face(3, 2), face(0, 1), face(3, 3), face(0, 0), face(3, 8), face(3, 7)),
        // 6. Arcos: verde y morado.
        intArrayOf(face(2, 5), face(3, 1), face(2, 7), face(3, 5), face(2, 4), face(3, 2)),
        // 7. Noche: azul y morado, lunas y estrellas.
        intArrayOf(face(0, 4), face(3, 0), face(3, 4), face(0, 3), face(3, 6), face(0, 0)),
        // 8. Gran final: las cuatro familias.
        intArrayOf(face(1, 0), face(0, 1), face(2, 7), face(3, 0), face(1, 8), face(0, 5)),
    )
}
