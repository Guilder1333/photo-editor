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
 * Global saturation using a luminance-weighted blend between the original pixel and
 * its greyscale equivalent (BT.709 luma).
 *
 * Formula: `out = clamp(lum + (in - lum) * value, 0, 1)`
 *
 * @param value Saturation multiplier.
 *   1 = no change (original).
 *   0 = fully desaturated (greyscale).
 *   Values > 1 boost saturation. Negative values are clamped to 0 (greyscale floor).
 */
class Saturation(val value: Float) : Adjustment {
    override val id = AdjustmentId("saturation")
    override val order = Order.SATURATION
    override fun isIdentity() = value == 1f
    override fun toFields() = listOf("value" to value)

    override fun apply(input: ImageBuffer): ImageBuffer {
        val multiplier = value.coerceAtLeast(0f)
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            val lum = LUM_R * p[i] + LUM_G * p[i + 1] + LUM_B * p[i + 2]
            out[i]     = (lum + (p[i]     - lum) * multiplier).coerceIn(0f, 1f)
            out[i + 1] = (lum + (p[i + 1] - lum) * multiplier).coerceIn(0f, 1f)
            out[i + 2] = (lum + (p[i + 2] - lum) * multiplier).coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]   // alpha unchanged
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }

    companion object : AdjustmentType {
        override val typeKey = "saturation"
        override fun fromFields(fields: Map<String, String?>) = Saturation(fields["value"]!!.toFloat())
    }
}
