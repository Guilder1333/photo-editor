package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TintTest {

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `isIdentity true at 0`() = assertTrue(Tint(0f).isIdentity())
    @Test fun `isIdentity false for non-zero`() = assertFalse(Tint(0.3f).isIdentity())

    @Test
    fun `zero tint leaves pixel unchanged`() {
        val input = px(0.4f, 0.6f, 0.8f)
        assertContentEquals(input.pixels, Tint(0f).apply(input).pixels)
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    fun `positive tint pushes toward magenta (G decreases)`() {
        val out = Tint(0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertTrue(out[1] < 0.5f, "G should decrease (magenta)")
    }

    @Test
    fun `negative tint pushes toward green (G increases)`() {
        val out = Tint(-0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertTrue(out[1] > 0.5f, "G should increase (green)")
    }

    @Test
    fun `R and B channels are always unchanged`() {
        for (v in listOf(-1f, -0.5f, 0f, 0.5f, 1f)) {
            val out = Tint(v).apply(px(0.3f, 0.6f, 0.7f)).pixels
            assertNear(0.3f, out[0], message = "R unchanged at tint=$v")
            assertNear(0.7f, out[2], message = "B unchanged at tint=$v")
        }
    }

    // ── Clamp ─────────────────────────────────────────────────────────────────

    @Test
    fun `G does not go below 0 for strong magenta tint`() {
        val out = Tint(1f).apply(px(0.5f, 0.05f, 0.5f)).pixels
        assertTrue(out[1] >= 0f, "G clamped to 0")  // 0.05 - 0.2 = -0.15 to 0
    }

    @Test
    fun `G does not exceed 1 for strong green tint`() {
        val out = Tint(-1f).apply(px(0.5f, 0.9f, 0.5f)).pixels
        assertTrue(out[1] <= 1f, "G clamped to 1")  // 0.9 + 0.2 = 1.1 to 1.0
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `alpha channel is never modified`() {
        val out = Tint(0.5f).apply(px(0.5f, 0.5f, 0.5f, 0.8f)).pixels
        assertNear(0.8f, out[3])
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `input ImageBuffer is not mutated`() {
        val input = px(0.5f, 0.5f, 0.5f)
        val snapshot = input.pixels.copyOf()
        Tint(0.5f).apply(input)
        assertContentEquals(snapshot, input.pixels)
    }

    // ── Golden pixel test ─────────────────────────────────────────────────────
    //
    // Tint(0.5) on (0.5, 0.5, 0.5, 1.0) with SCALE = 0.2:
    //   shift = 0.5 × 0.2 = 0.1
    //   out_R = 0.5 (unchanged)
    //   out_G = 0.5 - 0.1 = 0.4  (magenta: less green)
    //   out_B = 0.5 (unchanged)

    @Test
    fun `golden - Tint(0_5) magenta shift on grey`() {
        val out = Tint(0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertNear(0.5f, out[0], message = "R unchanged")
        assertNear(0.4f, out[1], message = "G (magenta)")
        assertNear(0.5f, out[2], message = "B unchanged")
        assertNear(1.0f, out[3], message = "A")
    }

    // Tint(-0.5) on (0.5, 0.5, 0.5): G = 0.5 + 0.1 = 0.6

    @Test
    fun `golden - Tint(-0_5) green shift on grey`() {
        val out = Tint(-0.5f).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertNear(0.5f, out[0], message = "R unchanged")
        assertNear(0.6f, out[1], message = "G (green)")
        assertNear(0.5f, out[2], message = "B unchanged")
    }

    // Tint(1.0) on (0.4, 0.05, 0.6): G = 0.05 - 0.2 = -0.15 to 0.0

    @Test
    fun `golden - Tint(1_0) clamps G to 0`() {
        val out = Tint(1f).apply(px(0.4f, 0.05f, 0.6f)).pixels
        assertNear(0.4f, out[0], message = "R unchanged")
        assertNear(0.0f, out[1], message = "G clamped to 0")
        assertNear(0.6f, out[2], message = "B unchanged")
    }
}
