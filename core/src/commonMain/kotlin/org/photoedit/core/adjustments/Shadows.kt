package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.LUM_B
import org.photoedit.core.LUM_G
import org.photoedit.core.LUM_R
import org.photoedit.core.Order

/**
 * Lifts dark shadow areas using a luminance-driven influence mask.
 *
 * Only pixels with luminance below 0.5 are affected; the influence ramps linearly
 * from 1 at lum = 0 to 0 at lum = 0.5. The max per-channel lift at full
 * influence is `value × 0.5`, keeping the adjustment gentle even at value = 1.
 *
 * Formula:
 * ```
 * influence = max(0, 1 − lum × 2)
 * out       = clamp(in + value × influence × 0.5, 0, 1)
 * ```
 *
 * @param value Shadow recovery in [-1, 1].
 *   Positive = lift dark areas brighter.
 *   Negative = push dark areas even darker.
 *   0 = no change.
 */
class Shadows(val value: Float) : Adjustment {
    override val id = AdjustmentId("shadows")
    override val order = Order.SHADOWS
    override fun isIdentity() = value == 0f

    override fun apply(input: ImageBuffer): ImageBuffer {
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            val r = p[i]; val g = p[i + 1]; val b = p[i + 2]
            val lum = LUM_R * r + LUM_G * g + LUM_B * b
            val influence = maxOf(0f, 1f - lum * 2f)
            val lift = value * influence * 0.5f
            out[i]     = (r + lift).coerceIn(0f, 1f)
            out[i + 1] = (g + lift).coerceIn(0f, 1f)
            out[i + 2] = (b + lift).coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]   // alpha unchanged
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }
}
