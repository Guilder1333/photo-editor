package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.LUM_B
import org.photoedit.core.LUM_G
import org.photoedit.core.LUM_R
import org.photoedit.core.Order

/**
 * Selective saturation boost that protects already-saturated pixels.
 *
 * Unlike [Saturation] which applies a uniform multiplier, Vibrance scales the
 * boost inversely with the pixel's current saturation (max - min channel range),
 * so muted tones receive a stronger lift while vivid colours are left largely alone.
 *
 * Formula per pixel:
 * ```
 * sat   = max(R, G, B) - min(R, G, B)
 * boost = 1 + value * (1 - sat)
 * out   = clamp(lum + (in - lum) * boost, 0, 1)
 * ```
 *
 * @param value Vibrance amount in [-1, 1]. 0 = no change.
 *   Grey pixels (sat = 0) receive the full boost; fully-saturated pixels are unaffected.
 */
class Vibrance(val value: Float) : Adjustment {
    override val id = AdjustmentId("vibrance")
    override val order = Order.VIBRANCE
    override fun isIdentity() = value == 0f

    override fun apply(input: ImageBuffer): ImageBuffer {
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            val r = p[i]; val g = p[i + 1]; val b = p[i + 2]
            val lum = LUM_R * r + LUM_G * g + LUM_B * b
            val sat = maxOf(r, g, b) - minOf(r, g, b)
            val boost = 1f + value * (1f - sat)
            out[i]     = (lum + (r - lum) * boost).coerceIn(0f, 1f)
            out[i + 1] = (lum + (g - lum) * boost).coerceIn(0f, 1f)
            out[i + 2] = (lum + (b - lum) * boost).coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]   // alpha unchanged
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }
}
