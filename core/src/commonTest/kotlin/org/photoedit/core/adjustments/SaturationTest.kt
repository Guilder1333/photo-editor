package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.LUM_B
import org.photoedit.core.LUM_G
import org.photoedit.core.LUM_R
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaturationTest {

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `isIdentity true at 1 (original)`() = assertTrue(Saturation(1f).isIdentity())
    @Test fun `isIdentity false at 0 (greyscale)`() = assertFalse(Saturation(0f).isIdentity())
    @Test fun `isIdentity false above 1 (boosted)`() = assertFalse(Saturation(1.5f).isIdentity())

    @Test
    fun `Saturation(1) leaves pixel unchanged`() {
        val input = px(0.6f, 0.3f, 0.4f)
        assertContentEquals(input.pixels, Saturation(1f).apply(input).pixels)
    }

    // ── Greyscale ─────────────────────────────────────────────────────────────

    @Test
    fun `Saturation(0) produces greyscale using BT709 luma`() {
        // Pure red: lum = 0.2126
        val out = Saturation(0f).apply(px(1f, 0f, 0f)).pixels
        val expectedLum = LUM_R  // 0.2126
        assertNear(expectedLum, out[0], message = "R")
        assertNear(expectedLum, out[1], message = "G")
        assertNear(expectedLum, out[2], message = "B")
    }

    @Test
    fun `grey pixel is invariant regardless of saturation value`() {
        // For grey R=G=B=lum, so (c - lum)=0 for all channels → no change
        for (v in listOf(0f, 0.5f, 1f, 2f)) {
            val out = Saturation(v).apply(px(0.5f, 0.5f, 0.5f)).pixels
            assertNear(0.5f, out[0], message = "R at sat=$v")
            assertNear(0.5f, out[1], message = "G at sat=$v")
            assertNear(0.5f, out[2], message = "B at sat=$v")
        }
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    fun `Saturation less than 1 moves channels toward luma`() {
        val input = px(0.8f, 0.2f, 0.5f)
        val lum = LUM_R * 0.8f + LUM_G * 0.2f + LUM_B * 0.5f
        val out = Saturation(0.5f).apply(input).pixels
        // Channels should be between lum and original
        assertTrue(out[0] in lum..0.8f, "R should move toward luma")
        assertTrue(out[1] in 0.2f..lum, "G should move toward luma")
    }

    @Test
    fun `Saturation greater than 1 amplifies deviation from luma`() {
        val input = px(0.7f, 0.4f, 0.5f)
        val lum = LUM_R * 0.7f + LUM_G * 0.4f + LUM_B * 0.5f
        val out = Saturation(1.5f).apply(input).pixels
        // R > original (deviation amplified), G further from luma
        assertTrue(out[0] > 0.7f || out[0] == 1f, "R deviated further from luma")
    }

    // ── Clamp ─────────────────────────────────────────────────────────────────

    @Test
    fun `output stays in 0 to 1 for extreme boost`() {
        val out = Saturation(3f).apply(px(1f, 0f, 0f)).pixels
        assertTrue(out[0] in 0f..1f && out[1] in 0f..1f && out[2] in 0f..1f)
    }

    @Test
    fun `negative value treated as 0 (greyscale floor)`() {
        // Negative multiplier is clamped to 0 → same as Saturation(0)
        val outNeg  = Saturation(-0.5f).apply(px(0.8f, 0.3f, 0.1f)).pixels
        val outZero = Saturation( 0.0f).apply(px(0.8f, 0.3f, 0.1f)).pixels
        assertContentEquals(outZero, outNeg)
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `alpha channel is never modified`() {
        val out = Saturation(0f).apply(px(0.8f, 0.3f, 0.5f, 0.7f)).pixels
        assertNear(0.7f, out[3])
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `input ImageBuffer is not mutated`() {
        val input = px(0.6f, 0.3f, 0.4f)
        val snapshot = input.pixels.copyOf()
        Saturation(0.5f).apply(input)
        assertContentEquals(snapshot, input.pixels)
    }

    // ── Golden pixel test ─────────────────────────────────────────────────────
    //
    // Saturation(0.5) on (0.6, 0.3, 0.4, 1.0):
    //   lum = 0.2126×0.6 + 0.7152×0.3 + 0.0722×0.4 = 0.12756 + 0.21456 + 0.02888 = 0.371
    //   out_R = 0.371 + (0.6 - 0.371) × 0.5 = 0.371 + 0.1145 = 0.4855
    //   out_G = 0.371 + (0.3 - 0.371) × 0.5 = 0.371 - 0.0355 = 0.3355
    //   out_B = 0.371 + (0.4 - 0.371) × 0.5 = 0.371 + 0.0145 = 0.3855

    @Test
    fun `golden - Saturation(0·5) partial desaturation`() {
        val out = Saturation(0.5f).apply(px(0.6f, 0.3f, 0.4f)).pixels
        assertNear(0.4855f, out[0], message = "R")
        assertNear(0.3355f, out[1], message = "G")
        assertNear(0.3855f, out[2], message = "B")
        assertNear(1.0f,    out[3], message = "A")
    }

    // Saturation(0) on pure red → greyscale luma = LUM_R = 0.2126

    @Test
    fun `golden - Saturation(0) pure red to greyscale`() {
        val out = Saturation(0f).apply(px(1f, 0f, 0f)).pixels
        assertNear(LUM_R, out[0], message = "R")
        assertNear(LUM_R, out[1], message = "G")
        assertNear(LUM_R, out[2], message = "B")
    }
}
