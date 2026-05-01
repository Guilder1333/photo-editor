package org.photoedit.core

import org.photoedit.core.adjustments.Blacks
import org.photoedit.core.adjustments.Exposure
import org.photoedit.core.adjustments.Temperature
import org.photoedit.core.adjustments.Tint
import org.photoedit.core.adjustments.Whites
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoAdjustTest {

    // ── exposure ──────────────────────────────────────────────────────────────

    @Test
    fun exposureIsZeroWhenMeanLumaIs18Percent() {
        val h = histogram(meanLuma = 0.18f)
        assertNear(0f, AutoAdjust.exposure(h).ev, epsilon = 0.01f)
    }

    @Test
    fun exposureIsOnePlusStopWhenImageIsHalfAsBright() {
        val h = histogram(meanLuma = 0.09f)   // half of 0.18 → +1 EV
        assertNear(1f, AutoAdjust.exposure(h).ev, epsilon = 0.01f)
    }

    @Test
    fun exposureIsClampedToMinus3ForVeryBrightImages() {
        val h = histogram(meanLuma = 0.99f)
        assertTrue(AutoAdjust.exposure(h).ev >= -3f)
    }

    @Test
    fun exposureIsClampedToPlus3ForVeryDarkImages() {
        val h = histogram(meanLuma = 1e-7f)
        assertEquals(3f, AutoAdjust.exposure(h).ev)
    }

    // ── whiteBalance ─────────────────────────────────────────────────────────

    @Test
    fun whiteBalanceIsNeutralForGrayImage() {
        val h = histogram(meanR = 0.18f, meanG = 0.18f, meanB = 0.18f)
        val (temp, tint) = AutoAdjust.whiteBalance(h)
        assertNear(0f, temp.value, epsilon = 0.01f)
        assertNear(0f, tint.value, epsilon = 0.01f)
    }

    @Test
    fun warmTemperatureToCounterBlueCast() {
        // B > R → scene is too blue → warm up (positive temperature)
        val h = histogram(meanR = 0.10f, meanG = 0.18f, meanB = 0.30f)
        val (temp, _) = AutoAdjust.whiteBalance(h)
        assertTrue(temp.value > 0f, "expected warm temp but got ${temp.value}")
    }

    @Test
    fun coolTemperatureToCounterOrangeCast() {
        // R > B → scene is too warm → cool down (negative temperature)
        val h = histogram(meanR = 0.30f, meanG = 0.18f, meanB = 0.10f)
        val (temp, _) = AutoAdjust.whiteBalance(h)
        assertTrue(temp.value < 0f, "expected cool temp but got ${temp.value}")
    }

    @Test
    fun magentaTintToCounterGreenCast() {
        // G > gray → green cast → magenta tint (positive)
        val h = histogram(meanR = 0.15f, meanG = 0.30f, meanB = 0.15f)
        val (_, tint) = AutoAdjust.whiteBalance(h)
        assertTrue(tint.value > 0f, "expected magenta tint but got ${tint.value}")
    }

    @Test
    fun whiteBalanceValuesAreClampedToUnitRange() {
        val h = histogram(meanR = 0.01f, meanG = 0.5f, meanB = 1.0f)
        val (temp, tint) = AutoAdjust.whiteBalance(h)
        assertTrue(temp.value in -1f..1f, "temp ${temp.value} out of [-1,1]")
        assertTrue(tint.value in -1f..1f, "tint ${tint.value} out of [-1,1]")
    }

    // ── toneRange ─────────────────────────────────────────────────────────────

    @Test
    fun whitesIsNearZeroWhenHighlightsAreAlreadyNearTarget() {
        // p99 sRGB ≈ 0.98 → no adjustment needed
        val h = histogramWithPercentiles(p01 = 0.02f, p99 = 0.98f)
        val (whites, _) = AutoAdjust.toneRange(h)
        assertTrue(abs(whites.value) < 0.05f, "whites value ${whites.value} expected near 0")
    }

    @Test
    fun whitesIsPositiveForDarkHighlights() {
        val h = histogramWithPercentiles(p01 = 0.02f, p99 = 0.60f)
        val (whites, _) = AutoAdjust.toneRange(h)
        assertTrue(whites.value > 0f, "expected positive whites but got ${whites.value}")
    }

    @Test
    fun blacksIsNearZeroWhenShadowsAreAlreadyNearTarget() {
        val h = histogramWithPercentiles(p01 = 0.02f, p99 = 0.98f)
        val (_, blacks) = AutoAdjust.toneRange(h)
        assertTrue(abs(blacks.value) < 0.05f, "blacks value ${blacks.value} expected near 0")
    }

    @Test
    fun blacksIsNegativeWhenShadowsAreTooLight() {
        // p01 = 0.20 (shadows far too bright) → crush blacks (negative value)
        val h = histogramWithPercentiles(p01 = 0.20f, p99 = 0.98f)
        val (_, blacks) = AutoAdjust.toneRange(h)
        assertTrue(blacks.value < 0f, "expected negative blacks but got ${blacks.value}")
    }

    @Test
    fun toneRangeValuesAreClampedToUnitRange() {
        // p99 = 0 forces whites to clamp at 1; p01 = 0 with clamped values stays in range
        val h = histogramWithPercentiles(p01 = 0f, p99 = 0f)
        val (whites, blacks) = AutoAdjust.toneRange(h)
        assertTrue(whites.value in -1f..1f)
        assertTrue(blacks.value in -1f..1f)
    }

    // ── all ───────────────────────────────────────────────────────────────────

    @Test
    fun allReturnsFiveAdjustmentsInOrder() {
        val h = histogram(meanLuma = 0.18f)
        val list = AutoAdjust.all(h)
        assertEquals(5, list.size)
        assertTrue(list[0] is Exposure)
        assertTrue(list[1] is Temperature)
        assertTrue(list[2] is Tint)
        assertTrue(list[3] is Whites)
        assertTrue(list[4] is Blacks)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun histogram(
        meanR: Float = 0.18f,
        meanG: Float = 0.18f,
        meanB: Float = 0.18f,
        meanLuma: Float = LUM_R * meanR + LUM_G * meanG + LUM_B * meanB,
    ) = Histogram(
        red = IntArray(256), green = IntArray(256), blue = IntArray(256), luma = IntArray(256),
        pixelCount = 0,
        meanLinearR = meanR, meanLinearG = meanG, meanLinearB = meanB, meanLinearLuma = meanLuma,
    )

    /**
     * Builds a 100-pixel luma histogram whose percentiles match the requested sRGB values.
     *
     * Layout (100 pixels total):
     *  - 1 pixel at p01Bin  → p01 threshold = 1.0 is met at exactly this bin
     *  - 97 pixels at midBin (between p01 and p99, exclusive)
     *  - 2 pixels at p99Bin → cumulative reaches 100 >= 99.0 at p99Bin
     *
     * When p01Bin == p99Bin the 97 mid pixels are placed at the same bin.
     */
    private fun histogramWithPercentiles(p01: Float, p99: Float): Histogram {
        val luma    = IntArray(256)
        val p01Bin  = (p01 * 255f + 0.5f).toInt().coerceIn(0, 255)
        val p99Bin  = (p99 * 255f + 0.5f).toInt().coerceIn(0, 255)
        val midBin  = if (p99Bin > p01Bin + 1) (p01Bin + p99Bin) / 2 else p01Bin

        luma[p01Bin] += 1
        luma[midBin] += 97   // if midBin == p01Bin: luma[p01Bin] = 98, still >= threshold 1.0
        luma[p99Bin] += 2    // total = 100; cumulative at p99Bin = 100 >= 99.0

        return Histogram(
            red = IntArray(256), green = IntArray(256), blue = IntArray(256), luma = luma,
            pixelCount = 100,
            meanLinearR = 0.18f, meanLinearG = 0.18f, meanLinearB = 0.18f, meanLinearLuma = 0.18f,
        )
    }
}
