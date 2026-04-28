package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.AdjustmentType
import org.photoedit.core.ImageBuffer
import org.photoedit.core.Order

/**
 * Green/magenta tint shift applied to the G channel only.
 *
 * Positive values push toward magenta (reduce green).
 * Negative values push toward green (increase green).
 * R and B channels are left untouched.
 *
 * @param value Tint shift in [-1, 1]. 0 = no change. Full range applies ±[SCALE] additive shift.
 */
class Tint(val value: Float) : Adjustment {
    override val id = AdjustmentId("tint")
    override val order = Order.TINT
    override fun isIdentity() = value == 0f
    override fun toFields() = listOf("value" to value)

    override fun apply(input: ImageBuffer): ImageBuffer {
        val shift = value * SCALE
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            out[i]     = p[i]                              // R: unchanged
            out[i + 1] = (p[i + 1] - shift).coerceIn(0f, 1f)  // G: positive=magenta(less green)
            out[i + 2] = p[i + 2]                          // B: unchanged
            out[i + 3] = p[i + 3]                          // A: unchanged
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }

    companion object : AdjustmentType {
        override val typeKey = "tint"
        override fun fromFields(fields: Map<String, String?>) = Tint(fields["value"]!!.toFloat())

        /** Maximum per-channel shift: value=±1 applies ±SCALE to G. */
        const val SCALE = 0.2f
    }
}
