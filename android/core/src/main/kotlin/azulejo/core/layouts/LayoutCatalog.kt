package azulejo.core.layouts

import azulejo.core.board.Layout
import azulejo.core.board.layout
import azulejo.core.generator.Difficulty

/**
 * Plantillas de tablero del juego, pensadas para pantalla vertical.
 *
 * Todas caben en [MAX_COLUMNS] × [MAX_ROWS] fichas. Las coordenadas van en
 * medias fichas (ver [azulejo.core.board.Position]): `block(x, y, columnas, filas, capa)`.
 *
 * Cambiar una plantilla ya publicada cambia los niveles y retos diarios que la usan.
 * Para variar un diseño, añade una plantilla nueva con otro id.
 */
object LayoutCatalog {

    const val MAX_COLUMNS = 6
    const val MAX_ROWS = 8

    // ---- Fáciles: 36 a 48 fichas ----

    /** 36 fichas, 4 columnas. Rectángulo con un patio elevado en el centro. */
    val patio: Layout = layout("patio") {
        block(0, 0, columns = 4, rows = 6, z = 0)
        block(2, 1, columns = 2, rows = 5, z = 1)
        block(3, 4, columns = 1, rows = 2, z = 2)
    }

    /** 40 fichas. Cruz con el eje central reforzado. */
    val cruz: Layout = layout("cruz") {
        block(4, 0, columns = 2, rows = 8, z = 0)
        block(0, 4, columns = 2, rows = 4, z = 0)
        block(8, 4, columns = 2, rows = 4, z = 0)
        block(5, 2, columns = 1, rows = 6, z = 1)
        tile(1, 7, 1)
        tile(9, 7, 1)
    }

    /** 48 fichas. Anillo con una fuente en el centro. */
    val fuente: Layout = layout("fuente") {
        block(0, 0, columns = 6, rows = 2, z = 0)
        block(0, 12, columns = 6, rows = 2, z = 0)
        block(0, 4, columns = 1, rows = 4, z = 0)
        block(10, 4, columns = 1, rows = 4, z = 0)
        block(4, 6, columns = 2, rows = 2, z = 0)
        block(1, 1, columns = 5, rows = 1, z = 1)
        block(1, 13, columns = 5, rows = 1, z = 1)
        tile(0, 7, 1)
        tile(10, 7, 1)
    }

    // ---- Medias: 56 a 64 fichas ----

    /** 56 fichas, 3 capas. Dintel y dos pilares. */
    val portal: Layout = layout("portal") {
        block(0, 0, columns = 6, rows = 2, z = 0)
        block(0, 4, columns = 2, rows = 6, z = 0)
        block(8, 4, columns = 2, rows = 6, z = 0)
        block(1, 1, columns = 5, rows = 1, z = 1)
        block(1, 5, columns = 1, rows = 5, z = 1)
        block(9, 5, columns = 1, rows = 5, z = 1)
        block(3, 1, columns = 3, rows = 1, z = 2)
        tile(1, 8, 2)
        tile(9, 8, 2)
    }

    /** 60 fichas, 3 capas. Rombo escalonado. */
    val rombo: Layout = layout("rombo") {
        block(4, 0, columns = 2, rows = 1, z = 0)
        block(2, 2, columns = 4, rows = 1, z = 0)
        block(0, 4, columns = 6, rows = 4, z = 0)
        block(2, 12, columns = 4, rows = 1, z = 0)
        block(4, 14, columns = 2, rows = 1, z = 0)
        block(3, 3, columns = 3, rows = 1, z = 1)
        block(1, 5, columns = 5, rows = 3, z = 1)
        block(3, 11, columns = 3, rows = 1, z = 1)
        block(3, 7, columns = 3, rows = 1, z = 2)
    }

    /** 64 fichas. Tablero lleno con una torre en cada esquina. */
    val torres: Layout = layout("torres") {
        block(0, 0, columns = 6, rows = 8, z = 0)
        block(0, 0, columns = 2, rows = 2, z = 1)
        block(8, 0, columns = 2, rows = 2, z = 1)
        block(0, 12, columns = 2, rows = 2, z = 1)
        block(8, 12, columns = 2, rows = 2, z = 1)
    }

    // ---- Difíciles: 72 fichas ----

    /** 72 fichas, 4 capas. Pirámide con base. */
    val piramide: Layout = layout("piramide") {
        block(0, 0, columns = 6, rows = 7, z = 0)
        block(4, 14, columns = 2, rows = 1, z = 0)
        block(2, 2, columns = 4, rows = 5, z = 1)
        block(4, 4, columns = 2, rows = 3, z = 2)
        block(5, 5, columns = 1, rows = 2, z = 3)
    }

    /** 72 fichas, 3 capas. Cuatro peldaños en diagonal sobre un tablero lleno. */
    val escalera: Layout = layout("escalera") {
        block(0, 0, columns = 6, rows = 8, z = 0)
        block(0, 0, columns = 2, rows = 2, z = 1)
        block(2, 4, columns = 2, rows = 2, z = 1)
        block(4, 8, columns = 2, rows = 2, z = 1)
        block(8, 12, columns = 2, rows = 2, z = 1)
        block(1, 0, columns = 1, rows = 2, z = 2)
        block(3, 4, columns = 1, rows = 2, z = 2)
        block(5, 8, columns = 1, rows = 2, z = 2)
        block(9, 12, columns = 1, rows = 2, z = 2)
    }

    /** 72 fichas, 3 capas. Tablero lleno con una alfombra central. */
    val mosaico: Layout = layout("mosaico") {
        block(0, 0, columns = 6, rows = 8, z = 0)
        block(1, 4, columns = 5, rows = 4, z = 1)
        block(4, 6, columns = 2, rows = 2, z = 2)
    }

    val easy: List<Layout> = listOf(patio, cruz, fuente)
    val medium: List<Layout> = listOf(portal, rombo, torres)
    val hard: List<Layout> = listOf(piramide, escalera, mosaico)

    val all: List<Layout> = easy + medium + hard

    fun forDifficulty(difficulty: Difficulty): List<Layout> = when (difficulty) {
        Difficulty.EASY -> easy
        Difficulty.MEDIUM -> medium
        Difficulty.HARD -> hard
    }

    fun byId(id: String): Layout =
        all.firstOrNull { it.id == id } ?: throw NoSuchElementException("No existe la plantilla '$id'")
}
