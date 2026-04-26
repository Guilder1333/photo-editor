package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoiseReductionTest {

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `NoiseReduction isIdentity at strength 0`() = assertTrue(NoiseReduction(0f).isIdentity())
    @Test fun `NoiseReduction isIdentity false for non-zero`() = assertFalse(NoiseReduction(0.5f).isIdentity())

    @Test
    fun `NoiseReduction(0) leaves pixel unchanged`() {
        val input = px(0.7f, 0.2f, 0.9f)
        assertContentEquals(input.pixels, NoiseReduction(0f).apply(input).pixels)
    }

    // ── Uniform image is unaffected by any strength ───────────────────────────

    @Test
    fun `uniform image is unchanged by full noise reduction`() {
        val size = 5
        val pixels = FloatArray(size * size * 4) { if (it % 4 < 3) 0.6f else 1f }
        val input = ImageBuffer(size, size, pixels)
        val out = NoiseReduction(1f).apply(input)
        for (i in 0 until out.pixels.size step 4) {
            assertNear(0.6f, out.pixels[i],     epsilon = 0.002f, message = "R at $i")
            assertNear(0.6f, out.pixels[i + 1], epsilon = 0.002f, message = "G at $i")
            assertNear(0.6f, out.pixels[i + 2], epsilon = 0.002f, message = "B at $i")
        }
    }

    // ── Blurring smooths sharp transitions ────────────────────────────────────

    @Test
    fun `full strength blurs a noisy image (reduces per-pixel variance)`() {
        val w = 7; val h = 7
        val pixels = FloatArray(w * h * 4)
        for (i in 0 until w * h) {
            val v = if (i % 2 == 0) 0f else 1f
            pixels[i * 4]     = v
            pixels[i * 4 + 1] = v
            pixels[i * 4 + 2] = v
            pixels[i * 4 + 3] = 1f
        }
        val input = ImageBuffer(w, h, pixels)
        val out = NoiseReduction(1f).apply(input)

        // After blurring a checkerboard the center pixels should be close to 0.5
        val centerIdx = (3 * w + 3) * 4
        assertNear(0.5f, out.pixels[centerIdx], epsilon = 0.1f, message = "center R after blur")
    }

    // ── Strength controls blend factor ────────────────────────────────────────

    @Test
    fun `higher strength produces more smoothing than lower strength`() {
        val w = 5; val h = 5
        val pixels = FloatArray(w * h * 4)
        for (i in 0 until w * h) {
            pixels[i * 4]     = if (i % 2 == 0) 0f else 1f
            pixels[i * 4 + 1] = if (i % 2 == 0) 0f else 1f
            pixels[i * 4 + 2] = if (i % 2 == 0) 0f else 1f
            pixels[i * 4 + 3] = 1f
        }
        val input = ImageBuffer(w, h, pixels)
        val out25 = NoiseReduction(0.25f).apply(input)
        val out75 = NoiseReduction(0.75f).apply(input)

        // A center pixel that was originally 1.0 should be pulled toward 0.5 more by 0.75
        val centerIdx = (2 * w + 2) * 4
        val diff25 = kotlin.math.abs(out25.pixels[centerIdx] - pixels[centerIdx])
        val diff75 = kotlin.math.abs(out75.pixels[centerIdx] - pixels[centerIdx])
        assertTrue(diff75 >= diff25, "higher strength should produce more change")
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `NoiseReduction does not modify alpha`() {
        val input = px(0.5f, 0.5f, 0.5f, 0.2f)
        assertNear(0.2f, NoiseReduction(1f).apply(input).pixels[3])
    }

    // ── Output bounds ─────────────────────────────────────────────────────────

    @Test
    fun `output stays in 0 to 1`() {
        val w = 3; val h = 3
        val pixels = FloatArray(w * h * 4) { if (it % 4 < 3) if ((it / 4) % 2 == 0) 0f else 1f else 1f }
        val input = ImageBuffer(w, h, pixels)
        val out = NoiseReduction(1f).apply(input)
        for (i in 0 until out.pixels.size step 4) {
            assertTrue(out.pixels[i]     in 0f..1f)
            assertTrue(out.pixels[i + 1] in 0f..1f)
            assertTrue(out.pixels[i + 2] in 0f..1f)
        }
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `NoiseReduction does not mutate input`() {
        val input = px(0.5f, 0.5f, 0.5f)
        val snap = input.pixels.copyOf()
        NoiseReduction(0.5f).apply(input)
        assertContentEquals(snap, input.pixels)
    }
}
