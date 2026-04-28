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
 * Pulls down bright highlight areas using a luminance-driven influence mask.
 *
 * Only pixels with luminance above 0.5 are affected; the influence ramps linearly
 * from 0 at lum = 0.5 to 1 at lum = 1.0. The max per-channel reduction at full
 * influence is `value × 0.5`, keeping the adjustment gentle even at value = 1.
 *
 * Formula:
 * ```
 * influence = max(0, lum × 2 − 1)
 * out       = clamp(in − value × influence × 0.5, 0, 1)
 * ```
 *
 * @param value Highlights recovery in [-1, 1].
 *   Positive = pull bright areas darker.
 *   Negative = push bright areas even brighter.
 *   0 = no change.
 */
class Highlights(val value: Float) : Adjustment {
    override val id = AdjustmentId("highlights")
    override val order = Order.HIGHLIGHTS
    override fun isIdentity() = value == 0f
    override fun toFields() = listOf("value" to value)

    override fun apply(input: ImageBuffer): ImageBuffer {
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            val r = p[i]; val g = p[i + 1]; val b = p[i + 2]
            val lum = LUM_R * r + LUM_G * g + LUM_B * b
            val influence = maxOf(0f, lum * 2f - 1f)
            val reduction = value * influence * 0.5f
            out[i]     = (r - reduction).coerceIn(0f, 1f)
            out[i + 1] = (g - reduction).coerceIn(0f, 1f)
            out[i + 2] = (b - reduction).coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]   // alpha unchanged
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }

    companion object : AdjustmentType {
        override val typeKey = "highlights"
        override fun fromFields(fields: Map<String, String?>) = Highlights(fields["value"]!!.toFloat())
    }
}
