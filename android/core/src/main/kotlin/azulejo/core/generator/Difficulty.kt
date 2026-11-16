package azulejo.core.generator

/**
 * Las tres dificultades del juego.
 *
 * La dificultad sale de tres cosas: el tamaño de la plantilla (lo decide
 * [azulejo.core.layouts.LayoutCatalog]), cuántas copias hay de cada dibujo y
 * qué tablero elige el generador entre varios candidatos.
 *
 * Para comparar candidatos se usa la «tasa de victoria al azar»: la fracción de
 * partidas que gana alguien que retira pares sin pensar. Un jugador real, con pistas
 * y deshacer, gana mucho más; el número solo ordena los tableros de más a menos amables.
 *
 * @property copiesPerFace fichas con el mismo dibujo. Con 2, cada dibujo forma un único
 *   par y es imposible equivocarse. Con 4 hay dos pares por dibujo, y emparejar las
 *   fichas «equivocadas» puede dejar el tablero bloqueado.
 * @property candidates tableros que se generan antes de elegir uno.
 * @property targetRandomWinRate tasa de victoria al azar que se busca; se elige el
 *   candidato que más se acerca. Evita tanto los tableros triviales como los crueles.
 */
enum class Difficulty(
    val copiesPerFace: Int,
    val candidates: Int,
    val targetRandomWinRate: Double,
) {
    /** Un par por dibujo: cualquier par disponible es correcto. */
    EASY(copiesPerFace = 2, candidates = 1, targetRandomWinRate = 1.0),

    /** Dos pares por dibujo. Con las plantillas medias, jugando al azar se gana unas 3 de cada 4 veces. */
    MEDIUM(copiesPerFace = 4, candidates = 4, targetRandomWinRate = 0.80),

    /** Dos pares por dibujo. Con las plantillas difíciles, jugando al azar se gana unas 2 de cada 5 veces. */
    HARD(copiesPerFace = 4, candidates = 6, targetRandomWinRate = 0.30),
}
