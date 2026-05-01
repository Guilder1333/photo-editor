package org.photoedit.core

import org.photoedit.core.codec.linearToSrgb

/**
 * Per-channel 256-bin histogram in sRGB display space, plus linear-light mean statistics.
 *
 * Bins are populated from sRGB-encoded values so they reflect what the eye sees.
 * Mean values are kept in linear light for accurate arithmetic (exposure, WB calculations).
 */
class Histogram(
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray,
    val luma: IntArray,
    val pixelCount: Int,
    val meanLinearR: Float,
    val meanLinearG: Float,
    val meanLinearB: Float,
    val meanLinearLuma: Float,
) {
    companion object {
        const val BINS = 256
    }

    /**
     * Returns the smallest bin index b such that at least [fraction] of pixels
     * in [channel] fall within bins 0..b (inclusive).
     */
    fun percentileBin(channel: IntArray, fraction: Float): Int {
        if (pixelCount == 0) return 0
        // Require at least 1 accumulated pixel so that empty leading bins are skipped.
        val threshold = (fraction * pixelCount.toFloat()).coerceAtLeast(1f)
        var cumulative = 0
        for (bin in 0 until BINS) {
            cumulative += channel[bin]
            if (cumulative >= threshold) return bin
        }
        return BINS - 1
    }

    /** Returns the [fraction] percentile of [channel] normalised to [0, 1]. */
    fun percentile(channel: IntArray, fraction: Float): Float =
        percentileBin(channel, fraction) / 255f
}

/** Computes a [Histogram] from this [ImageBuffer] in a single pass. */
fun ImageBuffer.computeHistogram(): Histogram {
    val red   = IntArray(Histogram.BINS)
    val green = IntArray(Histogram.BINS)
    val blue  = IntArray(Histogram.BINS)
    val luma  = IntArray(Histogram.BINS)

    var sumR    = 0.0
    var sumG    = 0.0
    var sumB    = 0.0
    var sumLuma = 0.0

    val p = pixels
    var i = 0
    while (i < p.size) {
        val r = p[i]; val g = p[i + 1]; val b = p[i + 2]
        val sr = r.linearToSrgb()
        val sg = g.linearToSrgb()
        val sb = b.linearToSrgb()
        val sl = LUM_R * sr + LUM_G * sg + LUM_B * sb

        red  [srgbBin(sr)]++
        green[srgbBin(sg)]++
        blue [srgbBin(sb)]++
        luma [srgbBin(sl)]++

        sumR    += r
        sumG    += g
        sumB    += b
        sumLuma += LUM_R * r + LUM_G * g + LUM_B * b
        i += 4
    }

    val n = (width * height).coerceAtLeast(1).toDouble()
    return Histogram(
        red, green, blue, luma, width * height,
        (sumR    / n).toFloat(),
        (sumG    / n).toFloat(),
        (sumB    / n).toFloat(),
        (sumLuma / n).toFloat(),
    )
}

private fun srgbBin(v: Float): Int = (v * 255f + 0.5f).toInt().coerceIn(0, 255)
