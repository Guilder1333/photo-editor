package org.photoedit.core

import org.photoedit.core.adjustments.Brightness
import org.photoedit.core.adjustments.Exposure
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MaskedAdjustmentTest {

    private fun buf(vararg px: Float) = ImageBuffer(1, px.size / 4, floatArrayOf(*px))
    private fun buf2x1(vararg px: Float) = ImageBuffer(2, 1, floatArrayOf(*px))

    // --- Mask construction ---

    @Test
    fun `Mask full produces all-ones values`() {
        val mask = Mask.full(2, 3)
        assertEquals(6, mask.values.size)
        mask.values.forEach { assertEquals(1f, it) }
    }

    @Test
    fun `Mask empty produces all-zeros values`() {
        val mask = Mask.empty(2, 3)
        assertEquals(6, mask.values.size)
        mask.values.forEach { assertEquals(0f, it) }
    }

    @Test
    fun `Mask rejects wrong values size`() {
        assertFailsWith<IllegalArgumentException> {
            Mask(2, 2, FloatArray(3))
        }
    }

    // --- MaskedAdjustment blending ---

    @Test
    fun `full mask produces same result as unmasked adjustment`() {
        val source = buf(0.5f, 0.5f, 0.5f, 1f)
        val adj = Brightness(0.2f)
        val masked = adj.withMask(Mask.full(1, 1))

        assertContentEquals(adj.apply(source).pixels, masked.apply(source).pixels)
    }

    @Test
    fun `empty mask leaves image unchanged`() {
        val source = buf(0.5f, 0.5f, 0.5f, 1f)
        val masked = Brightness(0.2f).withMask(Mask.empty(1, 1))

        assertContentEquals(source.pixels, masked.apply(source).pixels)
    }

    @Test
    fun `half mask blends original and adjusted at 50 percent`() {
        val source = buf(0.4f, 0.4f, 0.4f, 1f)
        val adj = Brightness(0.4f)  // would push channels to 0.8
        val masked = adj.withMask(Mask(1, 1, floatArrayOf(0.5f)))

        val result = masked.apply(source)
        // 0.4 + (0.8 - 0.4) * 0.5 = 0.6
        assertEquals(0.6f, result.pixels[0], 1e-5f)
        assertEquals(0.6f, result.pixels[1], 1e-5f)
        assertEquals(0.6f, result.pixels[2], 1e-5f)
        assertEquals(1f,   result.pixels[3])  // alpha unchanged
    }

    @Test
    fun `mask applies per-pixel influence independently`() {
        // Two-pixel image: left pixel fully masked, right pixel not masked
        val source = buf2x1(0.2f, 0.2f, 0.2f, 1f,  0.2f, 0.2f, 0.2f, 1f)
        val adj = Exposure(1f)  // doubles luminance: 0.2 -> 0.4
        val mask = Mask(2, 1, floatArrayOf(1f, 0f))
        val result = adj.withMask(mask).apply(source)

        // Left pixel: fully adjusted (0.4)
        assertEquals(0.4f, result.pixels[0], 1e-5f)
        // Right pixel: unchanged (0.2)
        assertEquals(0.2f, result.pixels[4], 1e-5f)
    }

    @Test
    fun `alpha channel is never modified by mask`() {
        val source = buf(0.5f, 0.5f, 0.5f, 0.3f)
        val masked = Brightness(0.5f).withMask(Mask.full(1, 1))
        assertEquals(0.3f, masked.apply(source).pixels[3])
    }

    @Test
    fun `MaskedAdjustment delegates id and order to wrapped adjustment`() {
        val adj = Brightness(0.1f)
        val masked = adj.withMask(Mask.full(1, 1))
        assertEquals(adj.id, masked.id)
        assertEquals(adj.order, masked.order)
    }

    @Test
    fun `identity adjustment is still identity when masked`() {
        val masked = Exposure(0f).withMask(Mask.full(1, 1))
        assertEquals(true, masked.isIdentity())
    }

    @Test
    fun `mask size mismatch throws IllegalArgumentException`() {
        val source = buf(0.5f, 0.5f, 0.5f, 1f)  // 1x1
        val masked = Brightness(0.1f).withMask(Mask(2, 2, FloatArray(4) { 1f }))
        assertFailsWith<IllegalArgumentException> {
            masked.apply(source)
        }
    }

    @Test
    fun `pipeline accepts MaskedAdjustment and replaces by id`() {
        val source = buf(0.4f, 0.4f, 0.4f, 1f)
        val pipeline = Pipeline(source)
            .withAdjustment(Brightness(0.2f).withMask(Mask.full(1, 1)))
            .withAdjustment(Brightness(0.4f).withMask(Mask.empty(1, 1)))  // replaces — id matches

        // Empty mask means no change applied
        assertContentEquals(source.pixels, pipeline.render().pixels)
    }
}
