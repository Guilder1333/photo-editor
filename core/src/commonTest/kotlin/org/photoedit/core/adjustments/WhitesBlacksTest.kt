package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhitesBlacksTest {

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ════════════════════════════════════════════════════════════════
    // WHITES
    // ════════════════════════════════════════════════════════════════

    @Test fun `Whites isIdentity at 0`() = assertTrue(Whites(0f).isIdentity())
    @Test fun `Whites isIdentity false for non-zero`() = assertFalse(Whites(0.5f).isIdentity())

    @Test
    fun `Whites(0) leaves pixel unchanged`() {
        val input = px(0.9f, 0.8f, 0.95f)
        assertContentEquals(input.pixels, Whites(0f).apply(input).pixels)
    }

    @Test
    fun `Whites does not affect mid-grey (lum = 0_5, influence = 0)`() {
        // influence = max(0, (0.5 - 0.75) * 4) = 0
        val out = Whites(0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertNear(0.5f, out[0]); assertNear(0.5f, out[1]); assertNear(0.5f, out[2])
    }

    @Test
    fun `Whites does not affect dark pixel (lum below 0_75)`() {
        // lum = 0.2, influence = max(0, (0.2-0.75)*4) = 0
        val input = px(0.2f, 0.2f, 0.2f)
        assertContentEquals(input.pixels, Whites(0.8f).apply(input).pixels)
    }

    @Test
    fun `positive Whites lifts bright pixel`() {
        val out = Whites(0.5f).apply(px(0.9f, 0.9f, 0.9f)).pixels
        assertTrue(out[0] >= 0.9f, "bright pixel should be lifted or clipped")
    }

    @Test
    fun `negative Whites darkens bright pixel`() {
        val out = Whites(-0.5f).apply(px(1f, 1f, 1f)).pixels
        assertTrue(out[0] < 1f, "pure white should be pulled down")
    }

    @Test
    fun `output stays in 0 to 1 for extreme Whites`() {
        val out = Whites(1f).apply(px(1f, 1f, 1f)).pixels
        assertTrue(out[0] in 0f..1f && out[1] in 0f..1f && out[2] in 0f..1f)
    }

    @Test
    fun `Whites does not modify alpha`() {
        assertNear(0.4f, Whites(0.5f).apply(px(1f, 1f, 1f, 0.4f)).pixels[3])
    }

    @Test
    fun `Whites does not mutate input`() {
        val input = px(1f, 1f, 1f)
        val snap = input.pixels.copyOf()
        Whites(0.5f).apply(input)
        assertContentEquals(snap, input.pixels)
    }

    // Golden pixel tests:
    //
    // Whites(-0.5) on pure white (1,1,1):
    //   lum = 1.0, influence = max(0, (1.0-0.75)*4) = 1.0
    //   delta = -0.5 * 1.0 * 0.5 = -0.25
    //   out = 1.0 - 0.25 = 0.75

    @Test
    fun `golden - Whites(-0_5) pulls down pure white`() {
        val out = Whites(-0.5f).apply(px(1f, 1f, 1f)).pixels
        assertNear(0.75f, out[0], message = "R")
        assertNear(0.75f, out[1], message = "G")
        assertNear(0.75f, out[2], message = "B")
        assertNear(1.0f,  out[3], message = "A")
    }

    // Whites(0.5) on pixel at boundary (0.75, 0.75, 0.75):
    //   lum = 0.75, influence = max(0, (0.75-0.75)*4) = 0
    //   delta = 0 -> no change

    @Test
    fun `golden - Whites(0_5) does not affect lum = 0_75 boundary pixel`() {
        val input = px(0.75f, 0.75f, 0.75f)
        assertContentEquals(input.pixels, Whites(0.5f).apply(input).pixels)
    }

    // Whites(0.5) on pixel (0.875, 0.875, 0.875) lum=0.875:
    //   influence = (0.875-0.75)*4 = 0.5
    //   delta = 0.5 * 0.5 * 0.5 = 0.125
    //   out = 0.875 + 0.125 = 1.0 (clamped)

    @Test
    fun `golden - Whites(0_5) lifts high-luminance pixel`() {
        val out = Whites(0.5f).apply(px(0.875f, 0.875f, 0.875f)).pixels
        assertNear(1.0f, out[0], epsilon = 0.002f, message = "R")
    }

    // ════════════════════════════════════════════════════════════════
    // BLACKS
    // ════════════════════════════════════════════════════════════════

    @Test fun `Blacks isIdentity at 0`() = assertTrue(Blacks(0f).isIdentity())
    @Test fun `Blacks isIdentity false for non-zero`() = assertFalse(Blacks(0.5f).isIdentity())

    @Test
    fun `Blacks(0) leaves pixel unchanged`() {
        val input = px(0.1f, 0.05f, 0.15f)
        assertContentEquals(input.pixels, Blacks(0f).apply(input).pixels)
    }

    @Test
    fun `Blacks does not affect mid-grey (lum = 0_5, influence = 0)`() {
        // influence = max(0, 1 - 0.5*4) = 0
        val out = Blacks(0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertNear(0.5f, out[0]); assertNear(0.5f, out[1]); assertNear(0.5f, out[2])
    }

    @Test
    fun `Blacks does not affect bright pixel (lum above 0_25)`() {
        val input = px(0.8f, 0.8f, 0.8f)
        assertContentEquals(input.pixels, Blacks(0.8f).apply(input).pixels)
    }

    @Test
    fun `positive Blacks lifts dark pixel`() {
        val out = Blacks(0.5f).apply(px(0.05f, 0.05f, 0.05f)).pixels
        assertTrue(out[0] > 0.05f, "dark pixel should be lifted")
    }

    @Test
    fun `negative Blacks darkens dark pixel further`() {
        val out = Blacks(-0.5f).apply(px(0.1f, 0.1f, 0.1f)).pixels
        assertTrue(out[0] < 0.1f || out[0] == 0f, "dark pixel should be crushed")
    }

    @Test
    fun `output stays in 0 to 1 for extreme Blacks`() {
        val out = Blacks(1f).apply(px(0f, 0f, 0f)).pixels
        assertTrue(out[0] in 0f..1f && out[1] in 0f..1f && out[2] in 0f..1f)
    }

    @Test
    fun `Blacks does not modify alpha`() {
        assertNear(0.7f, Blacks(0.5f).apply(px(0f, 0f, 0f, 0.7f)).pixels[3])
    }

    @Test
    fun `Blacks does not mutate input`() {
        val input = px(0f, 0f, 0f)
        val snap = input.pixels.copyOf()
        Blacks(0.5f).apply(input)
        assertContentEquals(snap, input.pixels)
    }

    // Golden pixel tests:
    //
    // Blacks(0.5) on pure black (0,0,0):
    //   lum = 0, influence = max(0, 1 - 0*4) = 1.0
    //   delta = 0.5 * 1.0 * 0.5 = 0.25
    //   out = 0.25

    @Test
    fun `golden - Blacks(0_5) lifts pure black`() {
        val out = Blacks(0.5f).apply(px(0f, 0f, 0f)).pixels
        assertNear(0.25f, out[0], message = "R")
        assertNear(0.25f, out[1], message = "G")
        assertNear(0.25f, out[2], message = "B")
        assertNear(1.0f,  out[3], message = "A")
    }

    // Blacks(-1.0) on black: delta = -0.5 -> out = max(0, -0.5) = 0 (clamped)

    @Test
    fun `golden - Blacks(-1_0) on black stays at 0 (clamped)`() {
        val out = Blacks(-1f).apply(px(0f, 0f, 0f)).pixels
        assertNear(0f, out[0], message = "R clamped")
    }

    // Blacks(0.5) on pixel at boundary (0.25, 0.25, 0.25):
    //   lum = 0.25, influence = max(0, 1 - 0.25*4) = 0
    //   delta = 0 -> no change

    @Test
    fun `golden - Blacks(0_5) does not affect lum = 0_25 boundary pixel`() {
        val input = px(0.25f, 0.25f, 0.25f)
        assertContentEquals(input.pixels, Blacks(0.5f).apply(input).pixels)
    }

    // ── Symmetry ──────────────────────────────────────────────────────────────

    @Test
    fun `Whites and Blacks have non-overlapping influence zones`() {
        // A pixel at lum = 0.5 should be unaffected by both
        val grey = px(0.5f, 0.5f, 0.5f)
        assertContentEquals(grey.pixels, Whites(1f).apply(grey).pixels)
        assertContentEquals(grey.pixels, Blacks(1f).apply(grey).pixels)
    }
}
