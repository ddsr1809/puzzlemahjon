package azulejo.core

import azulejo.core.random.SeededRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SeededRandomTest {

    /**
     * Valores de referencia de SplitMix64 con semilla 0. Si esta prueba falla, la secuencia
     * ha cambiado y todos los niveles y retos diarios ya publicados serían distintos.
     */
    @Test
    fun `la secuencia coincide con los valores de referencia de SplitMix64`() {
        val random = SeededRandom(0)
        assertEquals(0xE220A8397B1DCDAFuL.toLong(), random.nextLong())
        assertEquals(0x6E789E6AA1B965F4uL.toLong(), random.nextLong())
        assertEquals(0x06C45D188009454FuL.toLong(), random.nextLong())
    }

    @Test
    fun `la misma semilla da la misma secuencia`() {
        val a = SeededRandom(20261116)
        val b = SeededRandom(20261116)
        repeat(100) { assertEquals(a.nextLong(), b.nextLong()) }
    }

    @Test
    fun `semillas distintas dan secuencias distintas`() {
        assertNotEquals(SeededRandom(1).nextLong(), SeededRandom(2).nextLong())
    }

    @Test
    fun `nextInt respeta el limite y usa todos los valores`() {
        val random = SeededRandom(7)
        val seen = BooleanArray(6)
        repeat(1_000) {
            val value = random.nextInt(6)
            assertTrue(value in 0 until 6, "fuera de rango: $value")
            seen[value] = true
        }
        assertTrue(seen.all { it }, "algún valor no apareció nunca")
    }

    @Test
    fun `shuffle conserva los elementos`() {
        val original = (0 until 50).toList()
        val shuffled = original.toMutableList()
        SeededRandom(3).shuffle(shuffled)
        assertEquals(original, shuffled.sorted())
        assertNotEquals(original, shuffled)
    }

    @Test
    fun `fork es reproducible e independiente del padre`() {
        val parentA = SeededRandom(99)
        val parentB = SeededRandom(99)
        val childA = parentA.fork()
        val childB = parentB.fork()
        assertEquals(childA.nextLong(), childB.nextLong())
        assertNotEquals(childA.nextLong(), parentA.nextLong())
    }
}
