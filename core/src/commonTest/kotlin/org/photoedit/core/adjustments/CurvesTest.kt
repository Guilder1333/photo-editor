package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CurvesTest {

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `default Curves is identity`() = assertTrue(Curves().isIdentity())

    @Test
    fun `Curves with explicit identity list is identity`() =
        assertTrue(Curves(rgb = listOf(0f to 0f, 1f to 1f)).isIdentity())

    @Test
    fun `Curves with non-identity rgb is not identity`() =
        assertFalse(Curves(rgb = listOf(0f to 0f, 0.5f to 0.7f, 1f to 1f)).isIdentity())

    @Test
    fun `identity Curves leaves pixel unchanged`() {
        val input = px(0.3f, 0.5f, 0.7f)
        assertContentEquals(input.pixels, Curves().apply(input).pixels)
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @Test
    fun `pure black stays black with any curve that passes through (0,0)`() {
        val curve = Curves(rgb = listOf(0f to 0f, 0.5f to 0.8f, 1f to 1f))
        val out = curve.apply(px(0f, 0f, 0f)).pixels
        assertNear(0f, out[0]); assertNear(0f, out[1]); assertNear(0f, out[2])
    }

    @Test
    fun `pure white stays white with any curve that passes through (1,1)`() {
        val curve = Curves(rgb = listOf(0f to 0f, 0.5f to 0.3f, 1f to 1f))
        val out = curve.apply(px(1f, 1f, 1f)).pixels
        assertNear(1f, out[0]); assertNear(1f, out[1]); assertNear(1f, out[2])
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    fun `S-curve brightens midtones`() {
        // Classic S-curve: boosts contrast by lifting upper-mid and pulling lower-mid
        val sCurve = listOf(0f to 0f, 0.25f to 0.15f, 0.75f to 0.85f, 1f to 1f)
        val out = Curves(rgb = sCurve).apply(px(0.75f, 0.75f, 0.75f)).pixels
        assertTrue(out[0] > 0.75f, "upper mid should be lifted by S-curve")
    }

    @Test
    fun `inverted curve darkens image`() {
        val darken = listOf(0f to 0f, 1f to 0.5f)
        val out = Curves(rgb = darken).apply(px(0.8f, 0.8f, 0.8f)).pixels
        assertTrue(out[0] < 0.8f, "curve mapping 1→0.5 should darken bright pixels")
    }

    // ── Per-channel curves ────────────────────────────────────────────────────

    @Test
    fun `red curve shifts only red channel`() {
        val redBoost = listOf(0f to 0f, 1f to 0.5f)
        val curve = Curves(red = redBoost)
        val out = curve.apply(px(1f, 1f, 1f)).pixels
        assertNear(0.5f, out[0], epsilon = 0.01f, message = "R should be shifted by red curve")
        assertNear(1f,   out[1], message = "G should be unchanged")
        assertNear(1f,   out[2], message = "B should be unchanged")
    }

    @Test
    fun `rgb and per-channel curves compose`() {
        // Global curve: maps 1 -> 0.5 (halves all channels)
        // Red curve: maps 0.5 -> 0.5 (identity on 0.5)
        // Result for R: 1 -> 0.5 (rgb) -> 0.5 (red identity at 0.5)
        val half = listOf(0f to 0f, 1f to 0.5f)
        val curve = Curves(rgb = half, red = listOf(0f to 0f, 1f to 1f))
        val out = curve.apply(px(1f, 1f, 1f)).pixels
        assertNear(0.5f, out[0], epsilon = 0.01f, message = "R after rgb+identity red")
        assertNear(0.5f, out[1], epsilon = 0.01f, message = "G after rgb")
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `Curves does not modify alpha`() {
        val curve = Curves(rgb = listOf(0f to 0f, 0.5f to 0.8f, 1f to 1f))
        assertNear(0.6f, curve.apply(px(0.5f, 0.5f, 0.5f, 0.6f)).pixels[3])
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `Curves does not mutate input`() {
        val input = px(0.5f, 0.5f, 0.5f)
        val snap = input.pixels.copyOf()
        Curves(rgb = listOf(0f to 0f, 0.5f to 0.8f, 1f to 1f)).apply(input)
        assertContentEquals(snap, input.pixels)
    }

    // ── Output bounds ─────────────────────────────────────────────────────────

    @Test
    fun `output stays in 0 to 1 regardless of curve shape`() {
        val wild = listOf(0f to 0.5f, 0.5f to 1f, 1f to 0f)
        val out = Curves(rgb = wild).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertTrue(out[0] in 0f..1f && out[1] in 0f..1f && out[2] in 0f..1f)
    }

    // ── Golden: three-point midtone lift ──────────────────────────────────────
    //
    // Curve: (0,0), (0.5, 0.75), (1,1) — lifts mid-grey to 0.75.
    // At input = 0.5 the spline passes exactly through the control point.

    @Test
    fun `golden - three-point curve passes through control point at mid-grey`() {
        val lift = listOf(0f to 0f, 0.5f to 0.75f, 1f to 1f)
        val out = Curves(rgb = lift).apply(px(0.5f, 0.5f, 0.5f)).pixels
        assertNear(0.75f, out[0], epsilon = 0.005f, message = "R at control point")
    }

    // ── LUT internal tests ────────────────────────────────────────────────────

    @Test
    fun `identity curve evaluates to x at all points`() {
        val lut = Curves.buildLut(listOf(0f to 0f, 1f to 1f))
        assertNear(0f,   lut.eval(0f))
        assertNear(0.25f, lut.eval(0.25f), epsilon = 0.005f)
        assertNear(0.5f,  lut.eval(0.5f),  epsilon = 0.005f)
        assertNear(0.75f, lut.eval(0.75f), epsilon = 0.005f)
        assertNear(1f,   lut.eval(1f))
    }
}
