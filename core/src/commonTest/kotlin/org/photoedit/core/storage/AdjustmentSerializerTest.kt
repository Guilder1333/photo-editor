package org.photoedit.core.storage

import org.photoedit.core.adjustments.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Round-trip tests for [AdjustmentSerializer]: every supported adjustment type
 * is encoded to JSON and decoded back; the result must be structurally equal to
 * the original.
 */
class AdjustmentSerializerTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun roundTrip(vararg adjs: org.photoedit.core.Adjustment) =
        AdjustmentSerializer.fromJson(AdjustmentSerializer.toJson(adjs.toList()))

    // ── Empty list ────────────────────────────────────────────────────────────

    @Test fun `empty list round-trips to empty list`() {
        val result = AdjustmentSerializer.fromJson(AdjustmentSerializer.toJson(emptyList()))
        assertTrue(result.isEmpty())
    }

    // ── Single-float adjustments ──────────────────────────────────────────────

    @Test fun `Exposure round-trips`() {
        val out = roundTrip(Exposure(1.5f)).single() as Exposure
        assertEquals(1.5f, out.ev)
    }

    @Test fun `Brightness round-trips`() {
        val out = roundTrip(Brightness(-0.3f)).single() as Brightness
        assertEquals(-0.3f, out.value)
    }

    @Test fun `Contrast round-trips`() {
        val out = roundTrip(Contrast(0.7f)).single() as Contrast
        assertEquals(0.7f, out.value)
    }

    @Test fun `Highlights round-trips`() {
        val out = roundTrip(Highlights(-0.5f)).single() as Highlights
        assertEquals(-0.5f, out.value)
    }

    @Test fun `Shadows round-trips`() {
        val out = roundTrip(Shadows(0.4f)).single() as Shadows
        assertEquals(0.4f, out.value)
    }

    @Test fun `Whites round-trips`() {
        val out = roundTrip(Whites(0.2f)).single() as Whites
        assertEquals(0.2f, out.value)
    }

    @Test fun `Blacks round-trips`() {
        val out = roundTrip(Blacks(-0.1f)).single() as Blacks
        assertEquals(-0.1f, out.value)
    }

    @Test fun `Temperature round-trips`() {
        val out = roundTrip(Temperature(0.6f)).single() as Temperature
        assertEquals(0.6f, out.value)
    }

    @Test fun `Tint round-trips`() {
        val out = roundTrip(Tint(-0.2f)).single() as Tint
        assertEquals(-0.2f, out.value)
    }

    @Test fun `Saturation round-trips`() {
        val out = roundTrip(Saturation(1.3f)).single() as Saturation
        assertEquals(1.3f, out.value)
    }

    @Test fun `Vibrance round-trips`() {
        val out = roundTrip(Vibrance(0.5f)).single() as Vibrance
        assertEquals(0.5f, out.value)
    }

    @Test fun `Sharpness round-trips`() {
        val out = roundTrip(Sharpness(0.8f)).single() as Sharpness
        assertEquals(0.8f, out.value)
    }

    @Test fun `Clarity round-trips`() {
        val out = roundTrip(Clarity(0.4f)).single() as Clarity
        assertEquals(0.4f, out.value)
    }

    @Test fun `NoiseReduction round-trips`() {
        val out = roundTrip(NoiseReduction(0.25f)).single() as NoiseReduction
        assertEquals(0.25f, out.strength)
    }

    // ── Multi-float adjustments ───────────────────────────────────────────────

    @Test fun `Vignette round-trips`() {
        val out = roundTrip(Vignette(0.6f, 0.3f)).single() as Vignette
        assertEquals(0.6f, out.strength)
        assertEquals(0.3f, out.feather)
    }

    @Test fun `Vignette default feather round-trips`() {
        val out = roundTrip(Vignette(0.5f)).single() as Vignette
        assertEquals(0.5f, out.strength)
        assertEquals(0.5f, out.feather)
    }

    @Test fun `Crop round-trips`() {
        val out = roundTrip(Crop(0.1f, 0.05f, 0.15f, 0.02f)).single() as Crop
        assertEquals(0.1f,  out.left)
        assertEquals(0.05f, out.top)
        assertEquals(0.15f, out.right)
        assertEquals(0.02f, out.bottom)
    }

    // ── Curves ────────────────────────────────────────────────────────────────

    @Test fun `Curves identity round-trips`() {
        val out = roundTrip(Curves()).single() as Curves
        assertEquals(listOf(0f to 0f, 1f to 1f), out.rgb)
        assertNull(out.red)
        assertNull(out.green)
        assertNull(out.blue)
    }

    @Test fun `Curves with per-channel curves round-trips`() {
        val redCurve   = listOf(0f to 0f, 0.5f to 0.6f, 1f to 1f)
        val greenCurve = listOf(0f to 0.1f, 1f to 0.9f)
        val input = Curves(
            rgb   = listOf(0f to 0f, 0.5f to 0.7f, 1f to 1f),
            red   = redCurve,
            green = greenCurve,
            blue  = null,
        )
        val out = roundTrip(input).single() as Curves
        assertEquals(listOf(0f to 0f, 0.5f to 0.7f, 1f to 1f), out.rgb)
        assertEquals(redCurve,   out.red)
        assertEquals(greenCurve, out.green)
        assertNull(out.blue)
    }

    // ── Hsl ───────────────────────────────────────────────────────────────────

    @Test fun `Hsl identity round-trips`() {
        val out = roundTrip(Hsl()).single() as Hsl
        assertTrue(out.hueShifts.all { it == 0f })
        assertTrue(out.saturationShifts.all { it == 0f })
        assertTrue(out.lightnessShifts.all { it == 0f })
    }

    @Test fun `Hsl with values round-trips`() {
        val hue = FloatArray(8) { it * 10f }         // 0, 10, 20, ..., 70
        val sat = FloatArray(8) { it * 0.1f - 0.5f } // mixed positive/negative
        val lit = FloatArray(8) { -it * 0.05f }
        val out = roundTrip(Hsl(hue, sat, lit)).single() as Hsl
        for (i in 0 until 8) {
            assertEquals(hue[i], out.hueShifts[i],        "hueShifts[$i]")
            assertEquals(sat[i], out.saturationShifts[i], "satShifts[$i]")
            assertEquals(lit[i], out.lightnessShifts[i],  "litShifts[$i]")
        }
    }

    // ── Multiple adjustments in one list ──────────────────────────────────────

    @Test fun `multiple adjustments round-trip preserving order`() {
        val result = roundTrip(Exposure(1f), Brightness(0.2f), Contrast(-0.1f))
        assertEquals(3, result.size)
        assertTrue(result[0] is Exposure)
        assertTrue(result[1] is Brightness)
        assertTrue(result[2] is Contrast)
    }

    // ── JSON format sanity ────────────────────────────────────────────────────

    @Test fun `toJson produces valid JSON array`() {
        val json = AdjustmentSerializer.toJson(listOf(Exposure(1f), Brightness(0.5f)))
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("\"type\":\"exposure\""))
        assertTrue(json.contains("\"type\":\"brightness\""))
    }
}
