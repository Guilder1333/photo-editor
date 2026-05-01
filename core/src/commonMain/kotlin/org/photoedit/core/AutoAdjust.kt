package org.photoedit.core

import org.photoedit.core.adjustments.Blacks
import org.photoedit.core.adjustments.Exposure
import org.photoedit.core.adjustments.Temperature
import org.photoedit.core.adjustments.Tint
import org.photoedit.core.adjustments.Whites
import kotlin.math.ln

/**
 * Derives a set of corrective [Adjustment]s from image statistics.
 *
 * All calculations work in linear light (using [Histogram.meanLinear*]) so that
 * the suggestions are physically meaningful and independent of gamma encoding.
 */
object AutoAdjust {

    /**
     * Exposure correction that targets 18 % grey as the average luminance.
     *
     * ev = log2(0.18 / meanLuma), clamped to [-3, 3].
     */
    fun exposure(histogram: Histogram): Exposure {
        val meanLuma = histogram.meanLinearLuma.coerceAtLeast(1e-6f)
        val ev = (ln(0.18f / meanLuma) / LN2).coerceIn(-3f, 3f)
        return Exposure(ev)
    }

    /**
     * White-balance correction using the Grey World assumption:
     * the scene average colour should be neutral grey.
     *
     * Temperature: positive = warm (counteract blue cast), negative = cool.
     * Tint:        positive = magenta (counteract green cast), negative = green.
     */
    fun whiteBalance(histogram: Histogram): Pair<Temperature, Tint> {
        val r    = histogram.meanLinearR
        val g    = histogram.meanLinearG
        val b    = histogram.meanLinearB
        val gray = (r + g + b) / 3f

        // Temperature.apply: outR += value*SCALE, outB -= value*SCALE
        val temp = ((b - r) / (2f * Temperature.SCALE)).coerceIn(-1f, 1f)
        // Tint.apply: outG -= value*SCALE
        val tint = ((g - gray) / Tint.SCALE).coerceIn(-1f, 1f)

        return Temperature(temp) to Tint(tint)
    }

    /**
     * Tone range corrections that map the 99th-percentile luma to ~0.98 (whites)
     * and the 1st-percentile luma to ~0.02 (blacks).
     */
    fun toneRange(histogram: Histogram): Pair<Whites, Blacks> {
        val p99 = histogram.percentile(histogram.luma, 0.99f)
        val p01 = histogram.percentile(histogram.luma, 0.01f)

        val whitesValue = ((0.98f - p99) * 2f).coerceIn(-1f, 1f)
        val blacksValue = ((0.02f - p01) * 2f).coerceIn(-1f, 1f)

        return Whites(whitesValue) to Blacks(blacksValue)
    }

    /** Returns all five suggested adjustments in pipeline order. */
    fun all(histogram: Histogram): List<Adjustment> = buildList {
        add(exposure(histogram))
        val (temp, tint) = whiteBalance(histogram)
        add(temp)
        add(tint)
        val (whites, blacks) = toneRange(histogram)
        add(whites)
        add(blacks)
    }

    private val LN2 = ln(2f)
}
