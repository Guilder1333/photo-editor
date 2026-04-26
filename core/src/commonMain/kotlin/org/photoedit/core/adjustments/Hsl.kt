package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.Order
import kotlin.math.abs

/**
 * Per-hue HSL (hue/saturation/lightness) adjustments across 8 evenly-spaced hue ranges.
 *
 * Hue ranges (index 0–7) are centered at 0°, 45°, 90°, 135°, 180°, 225°, 270°, 315°,
 * corresponding roughly to: Red, Orange, Yellow, Green, Cyan, Blue, Purple, Magenta.
 *
 * Each pixel's hue determines which ranges influence it. Adjacent ranges blend smoothly
 * using a triangular weight function so there are no hard hue edges. Greyscale pixels
 * (saturation = 0) are unaffected by hue/saturation shifts.
 *
 * @param hueShifts        Per-range hue rotation in degrees [-180, 180]. 0 = no change.
 * @param saturationShifts Per-range saturation offset in [-1, 1]. 0 = no change.
 * @param lightnessShifts  Per-range lightness offset in [-1, 1]. 0 = no change.
 */
class Hsl(
    val hueShifts: FloatArray        = FloatArray(8),
    val saturationShifts: FloatArray = FloatArray(8),
    val lightnessShifts: FloatArray  = FloatArray(8),
) : Adjustment {

    override val id = AdjustmentId("hsl")
    override val order = Order.HSL

    override fun isIdentity() =
        hueShifts.all        { it == 0f } &&
        saturationShifts.all { it == 0f } &&
        lightnessShifts.all  { it == 0f }

    override fun apply(input: ImageBuffer): ImageBuffer {
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            val r = p[i]; val g = p[i + 1]; val b = p[i + 2]
            val (h, s, l) = rgbToHsl(r, g, b)

            var dh = 0f; var ds = 0f; var dl = 0f
            for (range in 0 until 8) {
                val w = hueWeight(h, range)
                if (w > 0f) {
                    // Hue and saturation shifts only make sense for coloured pixels;
                    // grey pixels (s=0) have no hue, so these shifts are suppressed.
                    if (s > 0f) {
                        dh += w * hueShifts[range]
                        ds += w * saturationShifts[range]
                    }
                    dl += w * lightnessShifts[range]
                }
            }

            val newH = ((h + dh / 360f) % 1f + 1f) % 1f
            val newS = (s + ds).coerceIn(0f, 1f)
            val newL = (l + dl).coerceIn(0f, 1f)

            val (nr, ng, nb) = hslToRgb(newH, newS, newL)
            out[i]     = nr.coerceIn(0f, 1f)
            out[i + 1] = ng.coerceIn(0f, 1f)
            out[i + 2] = nb.coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }

    companion object {
        // Hue range centers in [0, 1) — each center is separated by 1/8 (45°).
        private val HUE_CENTERS = FloatArray(8) { it / 8f }
        private const val HUE_WIDTH = 1f / 8f  // influence half-width (45°)

        /** Triangular weight of [rangeIndex] for a pixel whose hue is [hue] ∈ [0, 1). */
        internal fun hueWeight(hue: Float, rangeIndex: Int): Float {
            val center = HUE_CENTERS[rangeIndex]
            var dist = abs(hue - center)
            if (dist > 0.5f) dist = 1f - dist   // shortest-path wraparound
            return maxOf(0f, 1f - dist / HUE_WIDTH)
        }

        internal fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
            val cmax = maxOf(r, g, b)
            val cmin = minOf(r, g, b)
            val delta = cmax - cmin
            val l = (cmax + cmin) / 2f
            val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))
            val h = if (delta == 0f) 0f else when (cmax) {
                r    -> (((g - b) / delta) % 6f + 6f) % 6f / 6f
                g    -> ((b - r) / delta + 2f) / 6f
                else -> ((r - g) / delta + 4f) / 6f
            }
            return Triple(h, s, l)
        }

        internal fun hslToRgb(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
            val c = (1f - abs(2f * l - 1f)) * s
            val x = c * (1f - abs((h * 6f) % 2f - 1f))
            val m = l - c / 2f
            val (r1, g1, b1) = when ((h * 6f).toInt().coerceIn(0, 5)) {
                0    -> Triple(c, x, 0f)
                1    -> Triple(x, c, 0f)
                2    -> Triple(0f, c, x)
                3    -> Triple(0f, x, c)
                4    -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            return Triple(r1 + m, g1 + m, b1 + m)
        }
    }
}
