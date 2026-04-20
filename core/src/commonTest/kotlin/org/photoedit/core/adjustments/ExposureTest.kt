package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExposureTest {

    private fun buf(vararg px: Float) = ImageBuffer(1, px.size / 4, floatArrayOf(*px))

    @Test
    fun `zero EV is identity`() {
        val input = buf(0.25f, 0.5f, 0.75f, 1f)
        val output = Exposure(0f).apply(input)
        assertContentEquals(input.pixels, output.pixels)
    }

    @Test
    fun `plus one EV doubles linear rgb`() {
        val input = buf(0.1f, 0.2f, 0.3f, 1f)
        val out = Exposure(1f).apply(input).pixels
        assertEquals(0.2f, out[0], 1e-5f)
        assertEquals(0.4f, out[1], 1e-5f)
        assertEquals(0.6f, out[2], 1e-5f)
        assertEquals(1.0f, out[3], 1e-5f)  // alpha untouched
    }

    @Test
    fun `minus one EV halves linear rgb`() {
        val input = buf(0.4f, 0.6f, 0.8f, 1f)
        val out = Exposure(-1f).apply(input).pixels
        assertEquals(0.2f, out[0], 1e-5f)
        assertEquals(0.3f, out[1], 1e-5f)
        assertEquals(0.4f, out[2], 1e-5f)
        assertEquals(1.0f, out[3], 1e-5f)
    }

    @Test
    fun `isIdentity flags neutral parameters`() {
        assertTrue(Exposure(0f).isIdentity())
        assertFalse(Exposure(0.5f).isIdentity())
        assertFalse(Exposure(-1f).isIdentity())
    }

    @Test
    fun `apply does not mutate input`() {
        val input = buf(0.1f, 0.2f, 0.3f, 1f)
        val snapshot = input.pixels.copyOf()
        Exposure(1f).apply(input)
        assertContentEquals(snapshot, input.pixels)
    }
}
