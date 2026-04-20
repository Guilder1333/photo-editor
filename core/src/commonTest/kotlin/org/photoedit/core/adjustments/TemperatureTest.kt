package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemperatureTest {

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `isIdentity true at 0`() = assertTrue(Temperature(0f).isIdentity())
    @Test fun `isIdentity false for non-zero`() = assertFalse(Temperature(0.5f).isIdentity())

    @Test
    fun `zero temperature leaves pixel unchanged`() {
        val input = px(0.4f, 0.6f, 0.8f)
        assertContentEquals(input.pixels, Temperature(0f).apply(input).pixels)
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    fun `positive value warms: R increases, B decreases`() {
        val out = Temperature(0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertTrue(out[0] > 0.5f, "R should increase (warmer)")
        assertTrue(out[2] < 0.5f, "B should decrease (warmer)")
    }

    @Test
    fun `negative value cools: R decreases, B increases`() {
        val out = Temperature(-0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertTrue(out[0] < 0.5f, "R should decrease (cooler)")
        assertTrue(out[2] > 0.5f, "B should increase (cooler)")
    }

    @Test
    fun `G channel is always unchanged`() {
        for (v in listOf(-1f, -0.5f, 0f, 0.5f, 1f)) {
            val out = Temperature(v).apply(px(0.3f, 0.6f, 0.7f)).pixels
            assertNear(0.6f, out[1], message = "G should be unchanged at value=$v")
        }
    }

    // ── Clamp ─────────────────────────────────────────────────────────────────

    @Test
    fun `R does not exceed 1 for max warm on bright pixel`() {
        val out = Temperature(1f).apply(px(0.9f, 0.5f, 0.1f)).pixels
        assertTrue(out[0] <= 1f, "R clamped")
    }

    @Test
    fun `B does not go below 0 for max warm on dark-blue pixel`() {
        val out = Temperature(1f).apply(px(0.5f, 0.5f, 0.1f)).pixels
        assertTrue(out[2] >= 0f, "B clamped")
    }

    @Test
    fun `R does not go below 0 for max cool`() {
        val out = Temperature(-1f).apply(px(0.1f, 0.5f, 0.5f)).pixels
        assertTrue(out[0] >= 0f)
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `alpha channel is never modified`() {
        val out = Temperature(0.5f).apply(px(0.5f, 0.5f, 0.5f, 0.6f)).pixels
        assertNear(0.6f, out[3])
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `input ImageBuffer is not mutated`() {
        val input = px(0.5f, 0.5f, 0.5f)
        val snapshot = input.pixels.copyOf()
        Temperature(0.5f).apply(input)
        assertContentEquals(snapshot, input.pixels)
    }

    // ── Golden pixel test ─────────────────────────────────────────────────────
    //
    // Temperature(0.5) on (0.5, 0.5, 0.5, 1.0) with SCALE = 0.2:
    //   shift = 0.5 × 0.2 = 0.1
    //   out_R = 0.5 + 0.1 = 0.6
    //   out_G = 0.5        (unchanged)
    //   out_B = 0.5 - 0.1 = 0.4

    @Test
    fun `golden - Temperature(0·5) warms neutral grey`() {
        val out = Temperature(0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertNear(0.6f, out[0], message = "R")
        assertNear(0.5f, out[1], message = "G unchanged")
        assertNear(0.4f, out[2], message = "B")
        assertNear(1.0f, out[3], message = "A")
    }

    // Temperature(-0.5) on (0.5, 0.5, 0.5): R→0.4, G→0.5, B→0.6

    @Test
    fun `golden - Temperature(-0·5) cools neutral grey`() {
        val out = Temperature(-0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertNear(0.4f, out[0], message = "R")
        assertNear(0.5f, out[1], message = "G unchanged")
        assertNear(0.6f, out[2], message = "B")
    }

    // Temperature(1.0) on (0.9, 0.5, 0.1): shift=0.2, R=1.1→1.0, B=-0.1→0.0

    @Test
    fun `golden - Temperature(1·0) clamps at extremes`() {
        val out = Temperature(1f).apply(px(0.9f, 0.5f, 0.1f)).pixels
        assertNear(1.0f, out[0], message = "R clamped to 1")
        assertNear(0.5f, out[1], message = "G unchanged")
        assertNear(0.0f, out[2], message = "B clamped to 0")
    }
}
