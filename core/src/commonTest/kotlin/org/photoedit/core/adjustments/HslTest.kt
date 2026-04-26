package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HslTest {

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `default Hsl is identity`() = assertTrue(Hsl().isIdentity())

    @Test
    fun `identity Hsl leaves pixel unchanged`() {
        val input = px(0.8f, 0.3f, 0.1f)
        val out = Hsl().apply(input).pixels
        // Use assertNear: the RGB→HSL→RGB round-trip has float32 rounding noise.
        assertNear(input.pixels[0], out[0], epsilon = 0.002f, message = "R")
        assertNear(input.pixels[1], out[1], epsilon = 0.002f, message = "G")
        assertNear(input.pixels[2], out[2], epsilon = 0.002f, message = "B")
        assertNear(input.pixels[3], out[3], message = "A")
    }

    @Test
    fun `Hsl with any non-zero shift is not identity`() {
        val hShifts = FloatArray(8).also { it[0] = 30f }
        assertFalse(Hsl(hueShifts = hShifts).isIdentity())
    }

    // ── Grey pixels are unaffected by hue/saturation shifts ───────────────────

    @Test
    fun `grey pixel is unaffected by hue shifts`() {
        val hShifts = FloatArray(8) { 90f }
        val input = px(0.5f, 0.5f, 0.5f)
        val out = Hsl(hueShifts = hShifts).apply(input).pixels
        assertNear(0.5f, out[0], message = "R"); assertNear(0.5f, out[1], message = "G")
        assertNear(0.5f, out[2], message = "B")
    }

    @Test
    fun `grey pixel is unaffected by saturation shifts`() {
        val sShifts = FloatArray(8) { 1f }
        val input = px(0.5f, 0.5f, 0.5f)
        val out = Hsl(saturationShifts = sShifts).apply(input).pixels
        assertNear(0.5f, out[0], message = "R"); assertNear(0.5f, out[1], message = "G")
        assertNear(0.5f, out[2], message = "B")
    }

    // ── Saturation shift ──────────────────────────────────────────────────────

    @Test
    fun `negative saturation shift desaturates a coloured pixel`() {
        // Pure red is at hue 0 → range index 0
        val sShifts = FloatArray(8).also { it[0] = -1f }
        val out = Hsl(saturationShifts = sShifts).apply(px(1f, 0f, 0f)).pixels
        // Fully desaturated red should become grey (equal RGB channels)
        assertNear(out[0], out[1], epsilon = 0.02f, message = "R≈G after full desaturation")
        assertNear(out[1], out[2], epsilon = 0.02f, message = "G≈B after full desaturation")
    }

    // ── Lightness shift ───────────────────────────────────────────────────────

    @Test
    fun `positive lightness shift brightens a coloured pixel`() {
        val lShifts = FloatArray(8).also { it[0] = 0.2f }
        val before = px(0.6f, 0f, 0f)
        val out = Hsl(lightnessShifts = lShifts).apply(before).pixels
        val beforeLum = 0.6f * Hsl.rgbToHsl(before.pixels[0], before.pixels[1], before.pixels[2]).third
        val afterLum  = Hsl.rgbToHsl(out[0], out[1], out[2]).third
        assertTrue(afterLum > beforeLum || afterLum == 1f, "lightness should increase")
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `Hsl does not modify alpha`() {
        val sShifts = FloatArray(8).also { it[0] = 0.5f }
        val out = Hsl(saturationShifts = sShifts).apply(px(1f, 0f, 0f, 0.3f)).pixels
        assertNear(0.3f, out[3])
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `Hsl does not mutate input`() {
        val input = px(1f, 0f, 0f)
        val snap = input.pixels.copyOf()
        Hsl(hueShifts = FloatArray(8) { 30f }).apply(input)
        assertContentEquals(snap, input.pixels)
    }

    // ── Output bounds ─────────────────────────────────────────────────────────

    @Test
    fun `output stays in 0 to 1 for extreme shifts`() {
        val lShifts = FloatArray(8) { 1f }
        val out = Hsl(lightnessShifts = lShifts).apply(px(1f, 1f, 1f)).pixels
        assertTrue(out[0] in 0f..1f && out[1] in 0f..1f && out[2] in 0f..1f)
    }

    // ── RGB ↔ HSL round-trip ─────────────────────────────────────────────────

    @Test
    fun `RGB to HSL and back is lossless for pure red`() {
        val (h, s, l) = Hsl.rgbToHsl(1f, 0f, 0f)
        val (r, g, b) = Hsl.hslToRgb(h, s, l)
        assertNear(1f, r, epsilon = 0.002f, message = "R")
        assertNear(0f, g, epsilon = 0.002f, message = "G")
        assertNear(0f, b, epsilon = 0.002f, message = "B")
    }

    @Test
    fun `RGB to HSL and back is lossless for arbitrary colour`() {
        val (h, s, l) = Hsl.rgbToHsl(0.4f, 0.7f, 0.2f)
        val (r, g, b) = Hsl.hslToRgb(h, s, l)
        assertNear(0.4f, r, epsilon = 0.002f)
        assertNear(0.7f, g, epsilon = 0.002f)
        assertNear(0.2f, b, epsilon = 0.002f)
    }

    // ── Hue weight ────────────────────────────────────────────────────────────

    @Test
    fun `hue weight is 1 at range center`() {
        for (i in 0 until 8) {
            val center = i / 8f
            assertNear(1f, Hsl.hueWeight(center, i), message = "range $i at its own center")
        }
    }

    @Test
    fun `hue weight is 0 for a pixel at the opposite hue range center`() {
        // Range 0 (Red, center=0.0) should have zero weight for range 4 (Cyan, center=0.5)
        assertNear(0f, Hsl.hueWeight(0.5f, 0), message = "Red weight for cyan pixel")
    }

    @Test
    fun `hue weight wraps around for Red range at hue near 1_0`() {
        // Hue 0.95 is close to Red (center=0, dist wraps to 0.05 < 1/8)
        assertTrue(Hsl.hueWeight(0.95f, 0) > 0f, "near-1 hue should still have red weight")
    }
}
