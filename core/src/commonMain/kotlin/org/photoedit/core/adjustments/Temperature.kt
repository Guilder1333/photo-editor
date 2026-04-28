package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.AdjustmentType
import org.photoedit.core.ImageBuffer
import org.photoedit.core.Order

/**
 * Warm/cool colour temperature shift by adjusting R and B channels inversely.
 *
 * Positive values shift toward warm orange tones (more red, less blue).
 * Negative values shift toward cool blue tones (less red, more blue).
 * The G channel is left untouched.
 *
 * @param value Temperature shift in [-1, 1]. 0 = no change. Full range applies ±[SCALE] additive shift.
 */
class Temperature(val value: Float) : Adjustment {
    override val id = AdjustmentId("temperature")
    override val order = Order.TEMPERATURE
    override fun isIdentity() = value == 0f
    override fun toFields() = listOf("value" to value)

    override fun apply(input: ImageBuffer): ImageBuffer {
        val shift = value * SCALE
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            out[i]     = (p[i]     + shift).coerceIn(0f, 1f)  // R: warm = more red
            out[i + 1] = p[i + 1]                              // G: unchanged
            out[i + 2] = (p[i + 2] - shift).coerceIn(0f, 1f)  // B: warm = less blue
            out[i + 3] = p[i + 3]                              // A: unchanged
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }

    companion object : AdjustmentType {
        override val typeKey = "temperature"
        override fun fromFields(fields: Map<String, String?>) = Temperature(fields["value"]!!.toFloat())

        /** Maximum per-channel shift: value=±1 applies ±SCALE to R/B. */
        const val SCALE = 0.2f
    }
}
