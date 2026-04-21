package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VibranceTest {

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `isIdentity true at 0`() = assertTrue(Vibrance(0f).isIdentity())
    @Test fun `isIdentity false for non-zero`() = assertFalse(Vibrance(0.5f).isIdentity())

    @Test
    fun `zero vibrance leaves pixel unchanged`() {
        val input = px(0.8f, 0.2f, 0.5f)
        assertContentEquals(input.pixels, Vibrance(0f).apply(input).pixels)
    }

    // ── Selective saturation ──────────────────────────────────────────────────

    @Test
    fun `grey pixel (sat=0) is unchanged by vibrance of any value`() {
        // For grey: R=G=B=lum, so (c - lum)=0 to boost is irrelevant
        for (v in listOf(-1f, -0.5f, 0f, 0.5f, 1f)) {
            val out = Vibrance(v).apply(px(0.5f, 0.5f, 0.5f)).pixels
            assertNear(0.5f, out[0], message = "R at vibrance=$v")
            assertNear(0.5f, out[1], message = "G at vibrance=$v")
            assertNear(0.5f, out[2], message = "B at vibrance=$v")
        }
    }

    @Test
    fun `fully-saturated pixel (sat=1) is unchanged by positive vibrance`() {
        // Pure red: sat = max(1,0,0) - min(1,0,0) = 1 to boost = 1 + value*(1-1) = 1
        val out = Vibrance(1f).apply(px(1f, 0f, 0f)).pixels
        // lum = LUM_R; out_R = lum + (1-lum)*1 = 1; out_G = lum + (0-lum)*1 = 0
        assertNear(1.0f, out[0], message = "R")
        assertNear(0.0f, out[1], message = "G")
        assertNear(0.0f, out[2], message = "B")
    }

    @Test
    fun `muted pixel receives more boost than saturated pixel`() {
        val muted     = px(0.6f, 0.4f, 0.5f)  // sat = 0.6-0.4 = 0.2, gets higher boost
        val saturated = px(0.9f, 0.1f, 0.5f)  // sat = 0.9-0.1 = 0.8, gets lower boost

        val outMuted     = Vibrance(0.5f).apply(muted).pixels
        val outSaturated = Vibrance(0.5f).apply(saturated).pixels

        // For muted: boost = 1 + 0.5*(1-0.2) = 1.4
        // For saturated: boost = 1 + 0.5*(1-0.8) = 1.1
        // Muted should show more deviation from luma than saturated
        val lumMuted     = outMuted[0] - muted.pixels[0]
        val lumSaturated = outSaturated[0] - saturated.pixels[0]
        assertTrue(
            kotlin.math.abs(lumMuted) >= kotlin.math.abs(lumSaturated),
            "Muted pixel should receive stronger boost than saturated pixel"
        )
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    fun `positive vibrance amplifies colour deviation from luma`() {
        val input = px(0.7f, 0.3f, 0.5f)
        val out = Vibrance(0.5f).apply(input).pixels
        // R is above luma to should move further from luma (higher)
        // G is below luma to should move further from luma (lower)
        assertTrue(out[0] > 0.7f || out[0] == 1f)
        assertTrue(out[1] < 0.3f || out[1] == 0f)
    }

    // ── Clamp ─────────────────────────────────────────────────────────────────

    @Test
    fun `output stays in 0 to 1 for extreme positive vibrance`() {
        val out = Vibrance(1f).apply(px(0.8f, 0.1f, 0.5f)).pixels
        assertTrue(out[0] in 0f..1f && out[1] in 0f..1f && out[2] in 0f..1f)
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `alpha channel is never modified`() {
        val out = Vibrance(0.5f).apply(px(0.8f, 0.2f, 0.5f, 0.75f)).pixels
        assertNear(0.75f, out[3])
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `input ImageBuffer is not mutated`() {
        val input = px(0.8f, 0.2f, 0.5f)
        val snapshot = input.pixels.copyOf()
        Vibrance(0.5f).apply(input)
        assertContentEquals(snapshot, input.pixels)
    }

    // ── Golden pixel test ─────────────────────────────────────────────────────
    //
    // Vibrance(0.5) on (0.8, 0.2, 0.5, 1.0):
    //   lum  = 0.2126×0.8 + 0.7152×0.2 + 0.0722×0.5
    //        = 0.17008 + 0.14304 + 0.03610 = 0.34922
    //   sat  = max(0.8,0.2,0.5) - min(0.8,0.2,0.5) = 0.8 - 0.2 = 0.6
    //   boost = 1 + 0.5 × (1 - 0.6) = 1.2
    //   out_R = 0.34922 + (0.8 - 0.34922) × 1.2 = 0.34922 + 0.54094 = 0.8902
    //   out_G = 0.34922 + (0.2 - 0.34922) × 1.2 = 0.34922 - 0.17906 = 0.1702
    //   out_B = 0.34922 + (0.5 - 0.34922) × 1.2 = 0.34922 + 0.18094 = 0.5302

    @Test
    fun `golden - Vibrance(0_5) on semi-saturated pixel`() {
        val out = Vibrance(0.5f).apply(px(0.8f, 0.2f, 0.5f)).pixels
        assertNear(0.8902f, out[0], epsilon = 0.002f, message = "R")
        assertNear(0.1702f, out[1], epsilon = 0.002f, message = "G")
        assertNear(0.5302f, out[2], epsilon = 0.002f, message = "B")
        assertNear(1.0f,    out[3], message = "A")
    }
}
