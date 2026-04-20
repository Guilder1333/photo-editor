package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContrastTest {

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `isIdentity true at 0`() = assertTrue(Contrast(0f).isIdentity())
    @Test fun `isIdentity false for non-zero`() = assertFalse(Contrast(0.5f).isIdentity())

    @Test
    fun `zero contrast leaves pixel unchanged`() {
        val input = px(0.4f, 0.6f, 0.3f)
        assertContentEquals(input.pixels, Contrast(0f).apply(input).pixels)
    }

    // ── Pivot ─────────────────────────────────────────────────────────────────

    @Test
    fun `mid-grey (0·5) is invariant at any contrast value`() {
        // (0.5 - PIVOT) * factor + PIVOT = 0 * factor + 0.5 = 0.5 always
        for (v in listOf(-1f, -0.5f, 0f, 0.5f, 1f)) {
            val out = Contrast(v).apply(px(0.5f, 0.5f, 0.5f)).pixels
            assertNear(0.5f, out[0], message = "R at contrast=$v")
            assertNear(0.5f, out[1], message = "G at contrast=$v")
            assertNear(0.5f, out[2], message = "B at contrast=$v")
        }
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    fun `positive contrast pushes bright values brighter and dark values darker`() {
        val bright = Contrast(0.5f).apply(px(0.8f, 0.8f, 0.8f)).pixels
        val dark   = Contrast(0.5f).apply(px(0.2f, 0.2f, 0.2f)).pixels
        assertTrue(bright[0] > 0.8f, "bright should get brighter")
        assertTrue(dark[0]   < 0.2f, "dark should get darker")
    }

    @Test
    fun `negative contrast collapses all values toward mid-grey`() {
        val outBright = Contrast(-0.5f).apply(px(0.9f, 0.9f, 0.9f)).pixels
        val outDark   = Contrast(-0.5f).apply(px(0.1f, 0.1f, 0.1f)).pixels
        assertTrue(outBright[0] < 0.9f, "bright should move toward 0.5")
        assertTrue(outDark[0]   > 0.1f, "dark should move toward 0.5")
    }

    @Test
    fun `Contrast(-1) collapses everything to mid-grey (factor = 0)`() {
        val out = Contrast(-1f).apply(px(0.9f, 0.1f, 0.7f)).pixels
        assertNear(0.5f, out[0], message = "R")
        assertNear(0.5f, out[1], message = "G")
        assertNear(0.5f, out[2], message = "B")
    }

    // ── Clamp ─────────────────────────────────────────────────────────────────

    @Test
    fun `output never exceeds 1 at max contrast`() {
        val out = Contrast(1f).apply(px(0.9f, 0.9f, 0.9f)).pixels
        assertTrue(out[0] <= 1f)
    }

    @Test
    fun `output never goes below 0 at max contrast`() {
        val out = Contrast(1f).apply(px(0.1f, 0.1f, 0.1f)).pixels
        assertTrue(out[0] >= 0f)
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `alpha channel is never modified`() {
        val out = Contrast(0.5f).apply(px(0.8f, 0.8f, 0.8f, 0.3f)).pixels
        assertNear(0.3f, out[3])
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `input ImageBuffer is not mutated`() {
        val input = px(0.8f, 0.2f, 0.5f)
        val snapshot = input.pixels.copyOf()
        Contrast(0.5f).apply(input)
        assertContentEquals(snapshot, input.pixels)
    }

    // ── Golden pixel test ─────────────────────────────────────────────────────
    //
    // Contrast(0.5) on (0.8, 0.2, 0.5, 1.0) — factor = 1.5, pivot = 0.5:
    //   out_R = (0.8 - 0.5) * 1.5 + 0.5 = 0.45 + 0.5 = 0.95
    //   out_G = (0.2 - 0.5) * 1.5 + 0.5 = -0.45 + 0.5 = 0.05
    //   out_B = (0.5 - 0.5) * 1.5 + 0.5 = 0 + 0.5 = 0.50 (pivot, unchanged)

    @Test
    fun `golden - Contrast(0·5) on known pixel`() {
        val out = Contrast(0.5f).apply(px(0.8f, 0.2f, 0.5f)).pixels
        assertNear(0.95f, out[0], message = "R")
        assertNear(0.05f, out[1], message = "G")
        assertNear(0.50f, out[2], message = "B")
        assertNear(1.00f, out[3], message = "A")
    }

    // Contrast(1.0) on (0.9, 0.1, 0.5) — factor = 2.0:
    //   out_R = (0.9 - 0.5) * 2 + 0.5 = 0.8 + 0.5 = 1.3 → clamped to 1.0
    //   out_G = (0.1 - 0.5) * 2 + 0.5 = -0.8 + 0.5 = -0.3 → clamped to 0.0
    //   out_B = pivot → 0.5

    @Test
    fun `golden - Contrast(1·0) clamps extremes`() {
        val out = Contrast(1f).apply(px(0.9f, 0.1f, 0.5f)).pixels
        assertNear(1.0f, out[0], message = "R clamped to 1")
        assertNear(0.0f, out[1], message = "G clamped to 0")
        assertNear(0.5f, out[2], message = "B at pivot")
    }
}
