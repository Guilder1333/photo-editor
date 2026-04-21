package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.Order

/**
 * Additive brightness shift applied uniformly to all RGB channels in linear light.
 *
 * @param value Shift amount in [-1, 1]. 0 = no change. Positive = brighter, negative = darker.
 *   Output is clamped to [0, 1].
 */
class Brightness(val value: Float) : Adjustment {
    override val id = AdjustmentId("brightness")
    override val order = Order.BRIGHTNESS
    override fun isIdentity() = value == 0f

    override fun apply(input: ImageBuffer): ImageBuffer {
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            out[i]     = (p[i]     + value).coerceIn(0f, 1f)
            out[i + 1] = (p[i + 1] + value).coerceIn(0f, 1f)
            out[i + 2] = (p[i + 2] + value).coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]   // alpha unchanged
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }
}
