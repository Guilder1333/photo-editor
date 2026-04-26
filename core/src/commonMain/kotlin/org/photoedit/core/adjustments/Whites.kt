package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.LUM_B
import org.photoedit.core.LUM_G
import org.photoedit.core.LUM_R
import org.photoedit.core.Order

/**
 * Fine-grained control over the very brightest tones (luminance > 0.75).
 *
 * Influence ramps linearly from 0 at lum = 0.75 to 1 at lum = 1.0, so only the
 * top quarter of the tonal range is affected. This lets Whites and Highlights work
 * together without double-affecting the same tones.
 *
 * Formula:
 * ```
 * influence = max(0, (lum − 0.75) × 4)
 * out       = clamp(in + value × influence × 0.5, 0, 1)
 * ```
 *
 * @param value [-1, 1]. Positive = lift/expand whites. Negative = compress/recover whites. 0 = no change.
 */
class Whites(val value: Float) : Adjustment {
    override val id = AdjustmentId("whites")
    override val order = Order.WHITES
    override fun isIdentity() = value == 0f

    override fun apply(input: ImageBuffer): ImageBuffer {
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            val r = p[i]; val g = p[i + 1]; val b = p[i + 2]
            val lum = LUM_R * r + LUM_G * g + LUM_B * b
            val influence = maxOf(0f, (lum - 0.75f) * 4f)
            val delta = value * influence * 0.5f
            out[i]     = (r + delta).coerceIn(0f, 1f)
            out[i + 1] = (g + delta).coerceIn(0f, 1f)
            out[i + 2] = (b + delta).coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }
}
