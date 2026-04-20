package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for both [Highlights] and [Shadows] — they are inverses of each other and
 * share the same luminance-driven influence model, so combined coverage is cleaner.
 */
class HighlightsShadowsTest {

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ════════════════════════════════════════════════════════════════
    // HIGHLIGHTS
    // ════════════════════════════════════════════════════════════════

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `Highlights isIdentity at 0`() = assertTrue(Highlights(0f).isIdentity())
    @Test fun `Highlights isIdentity false for non-zero`() = assertFalse(Highlights(0.5f).isIdentity())

    @Test
    fun `Highlights(0) leaves pixel unchanged`() {
        val input = px(0.9f, 0.7f, 0.8f)
        assertContentEquals(input.pixels, Highlights(0f).apply(input).pixels)
    }

    // ── Boundary at lum = 0.5 ─────────────────────────────────────────────────

    @Test
    fun `Highlights does not affect mid-grey (lum = 0_5, influence = 0)`() {
        // influence = max(0, 0.5×2 - 1) = 0
        val out = Highlights(0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertNear(0.5f, out[0], message = "R at boundary")
        assertNear(0.5f, out[1], message = "G at boundary")
        assertNear(0.5f, out[2], message = "B at boundary")
    }

    @Test
    fun `Highlights does not affect dark pixel (lum below 0_5)`() {
        // lum = 0.2 to influence = max(0, 0.2×2-1) = 0
        val input = px(0.2f, 0.2f, 0.2f)
        assertContentEquals(input.pixels, Highlights(0.8f).apply(input).pixels)
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    fun `positive Highlights darkens bright pixel`() {
        val out = Highlights(0.5f).apply(px(0.9f, 0.9f, 0.9f)).pixels
        assertTrue(out[0] < 0.9f, "bright pixel should be darkened")
    }

    @Test
    fun `negative Highlights brightens bright pixel further`() {
        val out = Highlights(-0.5f).apply(px(0.9f, 0.9f, 0.9f)).pixels
        assertTrue(out[0] > 0.9f || out[0] == 1f, "bright pixel should be lifted")
    }

    // ── Clamp ─────────────────────────────────────────────────────────────────

    @Test
    fun `output stays in 0 to 1 for extreme Highlights`() {
        val out = Highlights(1f).apply(px(1f, 1f, 1f)).pixels
        assertTrue(out[0] in 0f..1f && out[1] in 0f..1f && out[2] in 0f..1f)
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `Highlights does not modify alpha`() {
        val out = Highlights(0.5f).apply(px(1f, 1f, 1f, 0.3f)).pixels
        assertNear(0.3f, out[3])
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `Highlights does not mutate input`() {
        val input = px(1f, 1f, 1f)
        val snapshot = input.pixels.copyOf()
        Highlights(0.5f).apply(input)
        assertContentEquals(snapshot, input.pixels)
    }

    // ── Golden pixel tests ────────────────────────────────────────────────────
    //
    // Highlights(0.5) on pure white (1, 1, 1, 1):
    //   lum = 1.0 to influence = max(0, 1×2−1) = 1.0
    //   reduction = 0.5 × 1.0 × 0.5 = 0.25
    //   out = 1.0 − 0.25 = 0.75

    @Test
    fun `golden - Highlights(0_5) pulls down pure white`() {
        val out = Highlights(0.5f).apply(px(1f, 1f, 1f)).pixels
        assertNear(0.75f, out[0], message = "R")
        assertNear(0.75f, out[1], message = "G")
        assertNear(0.75f, out[2], message = "B")
        assertNear(1.0f,  out[3], message = "A")
    }

    // Highlights(1.0) on white: reduction = 1×1×0.5 = 0.5 to out = 0.5

    @Test
    fun `golden - Highlights(1_0) on white to 0_5`() {
        val out = Highlights(1f).apply(px(1f, 1f, 1f)).pixels
        assertNear(0.5f, out[0], message = "R at max recovery")
    }

    // Highlights(0.5) on slightly-bright pixel (0.8, 0.8, 0.8):
    //   lum = 0.8 to influence = max(0, 0.8×2−1) = 0.6
    //   reduction = 0.5 × 0.6 × 0.5 = 0.15
    //   out = 0.8 − 0.15 = 0.65

    @Test
    fun `golden - Highlights(0_5) on bright-grey pixel`() {
        val out = Highlights(0.5f).apply(px(0.8f, 0.8f, 0.8f)).pixels
        assertNear(0.65f, out[0], epsilon = 0.002f, message = "R")
    }

    // ════════════════════════════════════════════════════════════════
    // SHADOWS
    // ════════════════════════════════════════════════════════════════

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `Shadows isIdentity at 0`() = assertTrue(Shadows(0f).isIdentity())
    @Test fun `Shadows isIdentity false for non-zero`() = assertFalse(Shadows(0.5f).isIdentity())

    @Test
    fun `Shadows(0) leaves pixel unchanged`() {
        val input = px(0.1f, 0.2f, 0.05f)
        assertContentEquals(input.pixels, Shadows(0f).apply(input).pixels)
    }

    // ── Boundary at lum = 0.5 ─────────────────────────────────────────────────

    @Test
    fun `Shadows does not affect mid-grey (lum = 0_5, influence = 0)`() {
        // influence = max(0, 1 − 0.5×2) = 0
        val out = Shadows(0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertNear(0.5f, out[0], message = "R at boundary")
        assertNear(0.5f, out[1], message = "G at boundary")
        assertNear(0.5f, out[2], message = "B at boundary")
    }

    @Test
    fun `Shadows does not affect bright pixel (lum above 0_5)`() {
        // lum = 0.8 to influence = max(0, 1−0.8×2) = 0
        val input = px(0.8f, 0.8f, 0.8f)
        assertContentEquals(input.pixels, Shadows(0.8f).apply(input).pixels)
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    fun `positive Shadows lifts dark pixel`() {
        val out = Shadows(0.5f).apply(px(0.1f, 0.1f, 0.1f)).pixels
        assertTrue(out[0] > 0.1f, "dark pixel should be lifted")
    }

    @Test
    fun `negative Shadows darkens dark pixel further`() {
        val out = Shadows(-0.5f).apply(px(0.1f, 0.1f, 0.1f)).pixels
        assertTrue(out[0] < 0.1f || out[0] == 0f, "dark pixel should be pushed darker")
    }

    // ── Clamp ─────────────────────────────────────────────────────────────────

    @Test
    fun `output stays in 0 to 1 for extreme Shadows`() {
        val out = Shadows(1f).apply(px(0f, 0f, 0f)).pixels
        assertTrue(out[0] in 0f..1f && out[1] in 0f..1f && out[2] in 0f..1f)
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `Shadows does not modify alpha`() {
        val out = Shadows(0.5f).apply(px(0f, 0f, 0f, 0.9f)).pixels
        assertNear(0.9f, out[3])
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `Shadows does not mutate input`() {
        val input = px(0f, 0f, 0f)
        val snapshot = input.pixels.copyOf()
        Shadows(0.5f).apply(input)
        assertContentEquals(snapshot, input.pixels)
    }

    // ── Golden pixel tests ────────────────────────────────────────────────────
    //
    // Shadows(0.5) on pure black (0, 0, 0, 1):
    //   lum = 0 to influence = max(0, 1 − 0×2) = 1.0
    //   lift = 0.5 × 1.0 × 0.5 = 0.25
    //   out = 0 + 0.25 = 0.25

    @Test
    fun `golden - Shadows(0_5) lifts pure black`() {
        val out = Shadows(0.5f).apply(px(0f, 0f, 0f)).pixels
        assertNear(0.25f, out[0], message = "R")
        assertNear(0.25f, out[1], message = "G")
        assertNear(0.25f, out[2], message = "B")
        assertNear(1.0f,  out[3], message = "A")
    }

    // Shadows(1.0) on black: lift = 1×1×0.5 = 0.5 to out = 0.5

    @Test
    fun `golden - Shadows(1_0) on black to 0_5`() {
        val out = Shadows(1f).apply(px(0f, 0f, 0f)).pixels
        assertNear(0.5f, out[0], message = "R at max lift")
    }

    // Shadows(0.5) on slightly-dark pixel (0.2, 0.2, 0.2):
    //   lum = 0.2 to influence = max(0, 1 − 0.2×2) = 0.6
    //   lift = 0.5 × 0.6 × 0.5 = 0.15
    //   out = 0.2 + 0.15 = 0.35

    @Test
    fun `golden - Shadows(0_5) on dark-grey pixel`() {
        val out = Shadows(0.5f).apply(px(0.2f, 0.2f, 0.2f)).pixels
        assertNear(0.35f, out[0], epsilon = 0.002f, message = "R")
    }

    // ── Symmetry check ────────────────────────────────────────────────────────

    @Test
    fun `Highlights and Shadows are symmetric at boundary pixel`() {
        // Both should have zero effect on exactly mid-grey
        val grey = px(0.5f, 0.5f, 0.5f)
        val afterHighlights = Highlights(0.5f).apply(grey)
        val afterShadows    = Shadows(0.5f).apply(grey)
        assertContentEquals(grey.pixels, afterHighlights.pixels)
        assertContentEquals(grey.pixels, afterShadows.pixels)
    }
}
