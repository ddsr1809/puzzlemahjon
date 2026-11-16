package azulejo.app.tiles

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/*
 * El arte de las 36 fichas, descrito con figuras simples. Kotlin puro: aquí no hay nada de
 * Android ni de Compose, así que se puede probar en la JVM y reutilizar en otra plataforma.
 * `TilePainter` es quien lo pinta en pantalla.
 *
 * Coordenadas: el origen es el centro de la cara de la ficha, x crece hacia la derecha e
 * y hacia abajo. La unidad es `TileArt.UNIT` veces el ancho de la cara, de modo que la
 * cara mide unas 2,4 unidades de ancho.
 */

/** Tinta de una figura. El color real lo pone la paleta ([TilePalette]). */
enum class Ink {
    /** Color de la familia. */
    MAIN,

    /** Detalle dorado, igual en todas las familias. */
    ACCENT,

    /** Color de fondo de la ficha: sirve para «recortar» huecos en una figura. */
    GROUND,
}

data class ArtPoint(val x: Float, val y: Float)

/** Las cuatro figuras con las que se dibuja todo. */
sealed interface ArtShape {
    val ink: Ink

    /** Círculo relleno. */
    data class Disc(val cx: Float, val cy: Float, val radius: Float, override val ink: Ink) : ArtShape

    /** Circunferencia de grosor [width]. */
    data class Ring(val cx: Float, val cy: Float, val radius: Float, val width: Float, override val ink: Ink) : ArtShape

    /** Polígono cerrado y relleno. Las curvas van aproximadas con muchos puntos. */
    data class Fill(val points: List<ArtPoint>, override val ink: Ink) : ArtShape

    /** Contorno cerrado de grosor [width], con las esquinas redondeadas. */
    data class Outline(val points: List<ArtPoint>, val width: Float, override val ink: Ink) : ArtShape
}

/**
 * Qué dibujo lleva cada cara: una de 4 familias y uno de 9 motivos.
 *
 * La familia se reconoce por el color **y** por el marco (esquinas, medallón, rombo o
 * recuadro); el motivo, por su silueta. Ningún par de fichas se distingue solo por el color.
 */
data class TileFace(val family: Int, val motif: Int) {
    companion object {
        const val FAMILIES = 4
        const val MOTIFS = 9
        const val COUNT = FAMILIES * MOTIFS

        fun of(face: Int): TileFace {
            require(face >= 0) { "Dibujo negativo: $face" }
            return TileFace(family = (face / MOTIFS) % FAMILIES, motif = face % MOTIFS)
        }
    }
}

object TileArt {

    /** Tamaño de la unidad de dibujo, en fracción del ancho de la cara. */
    const val UNIT = 0.42f

    /** Medio alto útil de la cara, en unidades: las figuras no pasan de aquí. */
    const val HALF_HEIGHT = 1.3f

    /** Medio ancho útil de la cara, en unidades. */
    const val HALF_WIDTH = 1.06f

    // Los 36 dibujos se calculan una vez, la primera vez que se pide uno.
    private val cache: List<List<ArtShape>> by lazy { List(TileFace.COUNT) { build(TileFace.of(it)) } }

    /** Figuras de un dibujo, en el orden en que se pintan. */
    fun shapes(face: Int): List<ArtShape> {
        require(face >= 0) { "Dibujo negativo: $face" }
        return cache[face % TileFace.COUNT]
    }

    private fun build(face: TileFace): List<ArtShape> {
        val shapes = ArrayList<ArtShape>()
        // En el medallón el motivo va «en negativo»: fondo de color y figura del color de la ficha.
        val negative = face.family == MEDALLION
        frame(face.family, shapes)
        val pen = Pen(
            scale = MOTIF_SCALE[face.family],
            figure = if (negative) Ink.GROUND else Ink.MAIN,
            hole = if (negative) Ink.MAIN else Ink.GROUND,
            into = shapes,
        )
        when (face.motif) {
            0 -> pen.star()
            1 -> pen.flower()
            2 -> pen.cross()
            3 -> pen.triangle()
            4 -> pen.crescent()
            5 -> pen.arch()
            6 -> pen.waves()
            7 -> pen.target()
            else -> pen.leaf()
        }
        return shapes
    }

    // ---------- Marcos: uno por familia ----------

    private const val CORNERS = 0
    private const val MEDALLION = 1
    private const val RHOMBUS = 2
    private const val BORDER = 3

    /** Radio máximo del motivo dentro de cada marco. */
    private val MOTIF_SCALE = floatArrayOf(0.78f, 0.74f, 0.63f, 0.72f)

    private const val LINE = 0.12f

    private fun frame(family: Int, into: MutableList<ArtShape>) {
        when (family) {
            CORNERS -> {
                // Un cuarto de círculo en cada esquina, como en los azulejos que forman un
                // dibujo mayor al juntarse.
                val cx = 1.0f
                val cy = 1.24f
                val r = 0.5f
                into += ArtShape.Fill(sector(-cx, -cy, r, 0f, 90f), Ink.MAIN)
                into += ArtShape.Fill(sector(cx, -cy, r, 90f, 180f), Ink.MAIN)
                into += ArtShape.Fill(sector(cx, cy, r, 180f, 270f), Ink.MAIN)
                into += ArtShape.Fill(sector(-cx, cy, r, 270f, 360f), Ink.MAIN)
            }
            MEDALLION -> into += ArtShape.Disc(0f, 0f, 1.0f, Ink.MAIN)
            RHOMBUS -> into += ArtShape.Outline(
                listOf(ArtPoint(0f, -1.22f), ArtPoint(0.98f, 0f), ArtPoint(0f, 1.22f), ArtPoint(-0.98f, 0f)),
                LINE,
                Ink.MAIN,
            )
            else -> into += ArtShape.Outline(
                listOf(ArtPoint(-0.94f, -1.18f), ArtPoint(0.94f, -1.18f), ArtPoint(0.94f, 1.18f), ArtPoint(-0.94f, 1.18f)),
                LINE,
                Ink.MAIN,
            )
        }
    }

    // ---------- Motivos ----------

    /** Dibuja un motivo de radio 1 escalado a [scale], con las tintas de la familia. */
    private class Pen(
        private val scale: Float,
        private val figure: Ink,
        private val hole: Ink,
        private val into: MutableList<ArtShape>,
    ) {
        private fun disc(x: Float, y: Float, r: Float, ink: Ink) {
            into += ArtShape.Disc(x * scale, y * scale, r * scale, ink)
        }

        private fun ring(r: Float, width: Float, ink: Ink) {
            into += ArtShape.Ring(0f, 0f, r * scale, width * scale, ink)
        }

        private fun fill(points: List<ArtPoint>, ink: Ink) {
            into += ArtShape.Fill(points.map { ArtPoint(it.x * scale, it.y * scale) }, ink)
        }

        /** Estrella de ocho puntas con el centro dorado. */
        fun star() {
            fill(starPoints(points = 8, outer = 1f, inner = 0.5f), figure)
            disc(0f, 0f, 0.24f, Ink.ACCENT)
        }

        /** Flor de cuatro pétalos redondos. */
        fun flower() {
            val d = 0.52f
            val r = 0.46f
            disc(0f, -d, r, figure)
            disc(d, 0f, r, figure)
            disc(0f, d, r, figure)
            disc(-d, 0f, r, figure)
            disc(0f, 0f, 0.26f, Ink.ACCENT)
        }

        /** Cruz de brazos que se ensanchan hacia fuera. */
        fun cross() {
            val narrow = 0.15f
            val wide = 0.44f
            val arm = listOf(ArtPoint(-narrow, -narrow), ArtPoint(-wide, -1f), ArtPoint(wide, -1f), ArtPoint(narrow, -narrow))
            val points = ArrayList<ArtPoint>()
            for (quarter in 0 until 4) points += arm.map { rotate(it, quarter * 90f) }
            fill(points, figure)
            fill(regularPolygon(4, 0.2f, 0f), hole)
        }

        /** Triángulo hueco. */
        fun triangle() {
            val outer = listOf(ArtPoint(0f, -0.94f), ArtPoint(0.86f, 0.6f), ArtPoint(-0.86f, 0.6f))
            // El hueco es el mismo triángulo, encogido hacia su centro.
            val centerY = (-0.94f + 0.6f + 0.6f) / 3f
            fill(outer, figure)
            fill(outer.map { ArtPoint(it.x * 0.4f, centerY + (it.y - centerY) * 0.4f) }, hole)
        }

        /** Luna creciente con un lucero. */
        fun crescent() {
            fill(crescentPoints(outerX = 0f, outerR = 0.86f, innerX = 0.44f, innerR = 0.7f), figure)
            disc(0.62f, 0f, 0.2f, figure)
        }

        /** Arco de herradura, como una puerta. */
        fun arch() {
            fill(archPoints(halfWidth = 0.64f, top = -1f, bottom = 0.74f), figure)
            // El hueco baja un poco más que el arco para que no quede un filo en el umbral.
            fill(archPoints(halfWidth = 0.3f, top = -0.5f, bottom = 0.78f), hole)
        }

        /** Tres olas: la del centro, más larga. */
        fun waves() {
            fill(wavePoints(centerY = -0.56f, halfWidth = 0.6f), figure)
            fill(wavePoints(centerY = 0f, halfWidth = 0.95f), figure)
            fill(wavePoints(centerY = 0.56f, halfWidth = 0.6f), figure)
        }

        /** Diana: un aro y un punto. */
        fun target() {
            ring(0.8f, 0.3f, figure)
            disc(0f, 0f, 0.34f, figure)
            disc(0f, 0f, 0.14f, Ink.ACCENT)
        }

        /** Hoja en diagonal con su nervio. */
        fun leaf() {
            fill(leafPoints(halfLength = 1f, halfWidth = 0.5f).map { rotate(it, -45f) }, figure)
            fill(leafPoints(halfLength = 0.66f, halfWidth = 0.08f).map { rotate(it, -45f) }, hole)
        }
    }

    // ---------- Geometría ----------

    private const val CURVE_STEPS = 24

    private fun radians(degrees: Float): Double = degrees * PI / 180.0

    private fun degrees(radians: Double): Float = (radians * 180.0 / PI).toFloat()

    private fun onCircle(cx: Float, cy: Float, r: Float, degrees: Float): ArtPoint {
        val a = radians(degrees)
        return ArtPoint(cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat())
    }

    /** Gira un punto alrededor del origen. Con y hacia abajo, los grados positivos giran en el sentido del reloj. */
    private fun rotate(point: ArtPoint, degrees: Float): ArtPoint {
        val a = radians(degrees)
        val c = cos(a).toFloat()
        val s = sin(a).toFloat()
        return ArtPoint(point.x * c - point.y * s, point.x * s + point.y * c)
    }

    /** Porción de círculo: el centro y el arco de [from] a [to] grados. */
    private fun sector(cx: Float, cy: Float, r: Float, from: Float, to: Float): List<ArtPoint> {
        val points = ArrayList<ArtPoint>(CURVE_STEPS + 2)
        points += ArtPoint(cx, cy)
        for (i in 0..CURVE_STEPS) points += onCircle(cx, cy, r, from + (to - from) * i / CURVE_STEPS)
        return points
    }

    /** Polígono regular con el primer vértice hacia arriba, girado [degrees]. */
    private fun regularPolygon(sides: Int, r: Float, degrees: Float): List<ArtPoint> =
        List(sides) { onCircle(0f, 0f, r, -90f + degrees + 360f * it / sides) }

    private fun starPoints(points: Int, outer: Float, inner: Float): List<ArtPoint> =
        List(points * 2) { onCircle(0f, 0f, if (it % 2 == 0) outer else inner, -90f + 180f * it / points) }

    /**
     * Luna: lo que queda de un círculo de radio [outerR] centrado en ([outerX], 0) al
     * quitarle otro de radio [innerR] centrado en ([innerX], 0), más a la derecha.
     */
    private fun crescentPoints(outerX: Float, outerR: Float, innerX: Float, innerR: Float): List<ArtPoint> {
        // Los dos círculos se cortan en x = cut; de ahí salen los ángulos donde empieza cada arco.
        val distance = innerX - outerX
        val cut = (distance * distance + outerR * outerR - innerR * innerR) / (2 * distance)
        val outerAngle = degrees(acos((cut / outerR).toDouble()))
        val innerAngle = degrees(acos(((cut - distance) / innerR).toDouble()))

        val steps = CURVE_STEPS * 2
        val points = ArrayList<ArtPoint>(steps * 2 + 2)
        // Arco exterior, por la izquierda, de un corte al otro…
        for (i in 0..steps) points += onCircle(outerX, 0f, outerR, outerAngle + (360f - 2 * outerAngle) * i / steps)
        // …y vuelta por el arco interior.
        for (i in 0..steps) points += onCircle(innerX, 0f, innerR, 360f - innerAngle - (360f - 2 * innerAngle) * i / steps)
        return points
    }

    /** Arco: un rectángulo rematado con medio círculo. */
    private fun archPoints(halfWidth: Float, top: Float, bottom: Float): List<ArtPoint> {
        val points = ArrayList<ArtPoint>(CURVE_STEPS + 3)
        points += ArtPoint(-halfWidth, bottom)
        for (i in 0..CURVE_STEPS) points += onCircle(0f, top + halfWidth, halfWidth, 180f + 180f * i / CURVE_STEPS)
        points += ArtPoint(halfWidth, bottom)
        return points
    }

    /** Banda ondulada horizontal de lado a lado. */
    private fun wavePoints(centerY: Float, halfWidth: Float): List<ArtPoint> {
        val thickness = 0.28f
        val amplitude = 0.12f
        val steps = CURVE_STEPS * 2
        // La misma ola en las tres bandas: lo que cambia es cuánto se ve de ella.
        fun y(x: Float): Float = centerY + amplitude * sin(x / 0.95 * 1.5 * PI).toFloat()
        val top = List(steps + 1) { val x = -halfWidth + 2 * halfWidth * it / steps; ArtPoint(x, y(x) - thickness / 2) }
        val bottom = top.reversed().map { ArtPoint(it.x, it.y + thickness) }
        return top + bottom
    }

    /** Hoja horizontal: dos arcos que se juntan en punta a izquierda y derecha. */
    private fun leafPoints(halfLength: Float, halfWidth: Float): List<ArtPoint> {
        // Cada lado es un arco de un círculo que pasa por las dos puntas y por (0, ±halfWidth).
        val r = (halfLength * halfLength + halfWidth * halfWidth) / (2 * halfWidth)
        val centerY = r - halfWidth
        val half = degrees(asin((halfLength / r).toDouble()))
        val points = ArrayList<ArtPoint>(CURVE_STEPS * 2 + 2)
        for (i in 0..CURVE_STEPS) points += onCircle(0f, centerY, r, 270f - half + 2 * half * i / CURVE_STEPS)
        for (i in 0..CURVE_STEPS) points += onCircle(0f, -centerY, r, 90f - half + 2 * half * i / CURVE_STEPS)
        return points
    }

    /** Hasta dónde llega una figura desde el centro, en cada eje. Para las pruebas. */
    fun extent(shape: ArtShape): ArtPoint = when (shape) {
        is ArtShape.Disc -> ArtPoint(abs(shape.cx) + shape.radius, abs(shape.cy) + shape.radius)
        is ArtShape.Ring -> ArtPoint(abs(shape.cx) + shape.radius + shape.width / 2, abs(shape.cy) + shape.radius + shape.width / 2)
        is ArtShape.Fill -> ArtPoint(shape.points.maxOf { abs(it.x) }, shape.points.maxOf { abs(it.y) })
        is ArtShape.Outline -> ArtPoint(
            shape.points.maxOf { abs(it.x) } + shape.width / 2,
            shape.points.maxOf { abs(it.y) } + shape.width / 2,
        )
    }
}
