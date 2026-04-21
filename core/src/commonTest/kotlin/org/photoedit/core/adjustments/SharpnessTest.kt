package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharpnessTest {

    private fun px1(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    /** 3×3 image: all pixels = [bg] except the center pixel (1,1) which = [center]. */
    private fun img3x3(bg: Float, center: Float, a: Float = 1f): ImageBuffer {
        val pixels = FloatArray(3 * 3 * 4) { i ->
            val isAlpha = i % 4 == 3
            if (isAlpha) a else bg
        }
        // pixel at (x=1, y=1) is index (1*3 + 1)*4 = 16
        pixels[16] = center
        pixels[17] = center
        pixels[18] = center
        return ImageBuffer(3, 3, pixels)
    }

    /** Extract the RGB value of pixel (x, y) from a 3×3 ImageBuffer (all channels equal). */
    private fun centerValue(buf: ImageBuffer) = buf.pixels[(1 * 3 + 1) * 4]
    private fun cornerValue(buf: ImageBuffer) = buf.pixels[0]

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `isIdentity true at 0`() = assertTrue(Sharpness(0f).isIdentity())
    @Test fun `isIdentity false for non-zero`() = assertFalse(Sharpness(0.5f).isIdentity())

    @Test
    fun `zero sharpness leaves pixel unchanged (1×1 image)`() {
        val input = px1(0.6f, 0.4f, 0.8f)
        assertContentEquals(input.pixels, Sharpness(0f).apply(input).pixels)
    }

    // ── 1×1 image is always invariant (no neighbours) ─────────────────────────

    @Test
    fun `sharpness on 1×1 image is identity regardless of value`() {
        // blur of a 1×1 image = the single pixel itself → unsharp = 0
        val input = px1(0.7f, 0.3f, 0.5f)
        for (v in listOf(0.25f, 0.5f, 1f)) {
            assertContentEquals(
                input.pixels,
                Sharpness(v).apply(input).pixels,
                "Expected identity on 1×1 at sharpness=$v",
            )
        }
    }

    // ── Uniform image is invariant (no edges to sharpen) ──────────────────────

    @Test
    fun `sharpness on a spatially uniform image is identity`() {
        // All pixels the same → blur = original → unsharp residual = 0
        val pixels = FloatArray(3 * 3 * 4) { if (it % 4 == 3) 1f else 0.5f }
        val input = ImageBuffer(3, 3, pixels)
        val out = Sharpness(0.5f).apply(input)
        assertContentEquals(pixels, out.pixels)
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    fun `positive sharpness enhances edges bright centre gets brighter, surround gets darker`() {
        val input = img3x3(bg = 0.5f, center = 0.8f)
        val out = Sharpness(0.5f).apply(input)
        assertTrue(centerValue(out) > 0.8f, "centre should be enhanced (brighter)")
        assertTrue(cornerValue(out) < 0.5f, "surrounding pixels should be slightly darker")
    }

    // ── Clamp ─────────────────────────────────────────────────────────────────

    @Test
    fun `output never exceeds 1 even at max sharpness`() {
        val input = img3x3(bg = 0.1f, center = 0.99f)
        val out = Sharpness(1f).apply(input)
        assertTrue(out.pixels.all { it <= 1f }, "no pixel should exceed 1")
    }

    @Test
    fun `output never goes below 0 even at max sharpness`() {
        val input = img3x3(bg = 0.9f, center = 0.01f)
        val out = Sharpness(1f).apply(input)
        assertTrue(out.pixels.all { it >= 0f }, "no pixel should go below 0")
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `alpha channel is never modified`() {
        val input = img3x3(bg = 0.5f, center = 0.8f, a = 0.4f)
        val out = Sharpness(0.5f).apply(input)
        // Check all alpha channels
        for (i in 3 until out.pixels.size step 4) {
            assertNear(0.4f, out.pixels[i], message = "alpha at pixel ${i / 4}")
        }
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `input ImageBuffer is not mutated`() {
        val input = img3x3(bg = 0.5f, center = 0.8f)
        val snapshot = input.pixels.copyOf()
        Sharpness(0.5f).apply(input)
        assertContentEquals(snapshot, input.pixels)
    }

    // ── Golden pixel test ─────────────────────────────────────────────────────
    //
    // 3×3 image: all pixels = 0.5 except center (1,1) = 0.8.
    //
    // For every pixel in this image the 3×3 box-blur neighbourhood contains exactly
    // one pixel at 0.8 (the centre) and the remaining 8 contributing samples at 0.5
    // (direct or via edge clamping). So:
    //
    //   blur = (8 × 0.5 + 0.8) / 9 = 4.8 / 9 ≈ 0.53333
    //
    // Centre pixel after Sharpness(0.5):
    //   out = 0.8 + 0.5 × (0.8 − 0.53333) = 0.8 + 0.5 × 0.26667 = 0.8 + 0.13333 ≈ 0.9333
    //
    // Any non-centre pixel after Sharpness(0.5):
    //   out = 0.5 + 0.5 × (0.5 − 0.53333) = 0.5 + 0.5 × (−0.03333) = 0.5 − 0.01667 ≈ 0.4833

    private val BLUR_3X3  = 4.8f / 9f          // ≈ 0.5333
    private val CENTER_IN = 0.8f
    private val BG_IN     = 0.5f
    private val SHARPNESS = 0.5f

    private val EXPECTED_CENTER = CENTER_IN + SHARPNESS * (CENTER_IN - BLUR_3X3) // ≈ 0.9333
    private val EXPECTED_BG     = BG_IN     + SHARPNESS * (BG_IN     - BLUR_3X3) // ≈ 0.4833

    @Test
    fun `golden - centre pixel is enhanced`() {
        val out = Sharpness(SHARPNESS).apply(img3x3(BG_IN, CENTER_IN))
        assertNear(EXPECTED_CENTER, centerValue(out), epsilon = 0.002f, message = "centre R")
    }

    @Test
    fun `golden - surrounding pixels are slightly darkened`() {
        val out = Sharpness(SHARPNESS).apply(img3x3(BG_IN, CENTER_IN))
        assertNear(EXPECTED_BG, cornerValue(out), epsilon = 0.002f, message = "corner R")
    }

    @Test
    fun `golden - alpha of centre pixel is unchanged`() {
        val out = Sharpness(SHARPNESS).apply(img3x3(BG_IN, CENTER_IN))
        assertNear(1.0f, out.pixels[(1 * 3 + 1) * 4 + 3], message = "centre alpha")
    }
}
