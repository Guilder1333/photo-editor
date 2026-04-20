package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrightnessTest {

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `isIdentity true at neutral value`() = assertTrue(Brightness(0f).isIdentity())
    @Test fun `isIdentity false for non-zero`() = assertFalse(Brightness(0.1f).isIdentity())

    @Test
    fun `zero brightness leaves pixel unchanged`() {
        val input = px(0.4f, 0.6f, 0.8f)
        assertContentEquals(input.pixels, Brightness(0f).apply(input).pixels)
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    fun `positive value brightens all channels`() {
        val out = Brightness(0.2f).apply(px(0.3f, 0.5f, 0.7f)).pixels
        assertTrue(out[0] > 0.3f && out[1] > 0.5f && out[2] > 0.7f)
    }

    @Test
    fun `negative value darkens all channels`() {
        val out = Brightness(-0.2f).apply(px(0.5f, 0.6f, 0.7f)).pixels
        assertTrue(out[0] < 0.5f && out[1] < 0.6f && out[2] < 0.7f)
    }

    // ── Clamp ─────────────────────────────────────────────────────────────────

    @Test
    fun `output never exceeds 1 for extreme positive value`() {
        val out = Brightness(0.9f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertTrue(out[0] <= 1f && out[1] <= 1f && out[2] <= 1f)
    }

    @Test
    fun `output never goes below 0 for extreme negative value`() {
        val out = Brightness(-0.9f).apply(px(0.3f, 0.3f, 0.3f)).pixels
        assertTrue(out[0] >= 0f && out[1] >= 0f && out[2] >= 0f)
    }

    @Test
    fun `bright pixel clamped to 1`() {
        val out = Brightness(0.8f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertNear(1.0f, out[0])  // 0.5 + 0.8 = 1.3 to clamped
    }

    @Test
    fun `dark pixel clamped to 0`() {
        val out = Brightness(-0.8f).apply(px(0.3f, 0.3f, 0.3f)).pixels
        assertNear(0.0f, out[0])  // 0.3 - 0.8 = -0.5 to clamped
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `alpha channel is never modified`() {
        val out = Brightness(0.5f).apply(px(0.5f, 0.5f, 0.5f, 0.4f)).pixels
        assertNear(0.4f, out[3])
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `input ImageBuffer is not mutated`() {
        val input = px(0.3f, 0.5f, 0.7f)
        val snapshot = input.pixels.copyOf()
        Brightness(0.3f).apply(input)
        assertContentEquals(snapshot, input.pixels)
    }

    // ── Golden pixel test ─────────────────────────────────────────────────────
    //
    // Brightness(0.2) on (0.3, 0.5, 0.1, 1.0):
    //   out_R = 0.3 + 0.2 = 0.5
    //   out_G = 0.5 + 0.2 = 0.7
    //   out_B = 0.1 + 0.2 = 0.3
    //   out_A = 1.0 (unchanged)

    @Test
    fun `golden - Brightness(0_2) on known pixel`() {
        val out = Brightness(0.2f).apply(px(0.3f, 0.5f, 0.1f)).pixels
        assertNear(0.5f, out[0], message = "R")
        assertNear(0.7f, out[1], message = "G")
        assertNear(0.3f, out[2], message = "B")
        assertNear(1.0f, out[3], message = "A")
    }
}
