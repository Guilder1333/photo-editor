package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.AdjustmentType
import org.photoedit.core.ImageBuffer
import org.photoedit.core.LUM_B
import org.photoedit.core.LUM_G
import org.photoedit.core.LUM_R
import org.photoedit.core.Order

/**
 * Fine-grained control over the very darkest tones (luminance < 0.25).
 *
 * Influence ramps linearly from 1 at lum = 0 to 0 at lum = 0.25, so only the
 * bottom quarter of the tonal range is affected. This lets Blacks and Shadows
 * work together without double-affecting the same tones.
 *
 * Formula:
 * ```
 * influence = max(0, 1 − lum × 4)
 * out       = clamp(in + value × influence × 0.5, 0, 1)
 * ```
 *
 * @param value [-1, 1]. Positive = lift/expand blacks. Negative = crush/clip blacks. 0 = no change.
 */
class Blacks(val value: Float) : Adjustment {
    override val id = AdjustmentId("blacks")
    override val order = Order.BLACKS
    override fun isIdentity() = value == 0f
    override fun toFields() = listOf("value" to value)

    override fun apply(input: ImageBuffer): ImageBuffer {
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            val r = p[i]; val g = p[i + 1]; val b = p[i + 2]
            val lum = LUM_R * r + LUM_G * g + LUM_B * b
            val influence = maxOf(0f, 1f - lum * 4f)
            val delta = value * influence * 0.5f
            out[i]     = (r + delta).coerceIn(0f, 1f)
            out[i + 1] = (g + delta).coerceIn(0f, 1f)
            out[i + 2] = (b + delta).coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }

    companion object : AdjustmentType {
        override val typeKey = "blacks"
        override fun fromFields(fields: Map<String, String?>) = Blacks(fields["value"]!!.toFloat())
    }
}
