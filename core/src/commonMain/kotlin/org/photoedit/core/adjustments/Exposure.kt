package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.Order

/**
 * Exposure adjustment in linear light.
 *
 * Applies a 2^EV multiplier to the RGB channels; alpha is preserved.
 * Because the pipeline stores pixels in linear light, this is a simple
 * multiplication — no gamma correction needed here.
 *
 * @param ev Exposure value in stops. 0 = no change, +1 = one stop brighter,
 *           -1 = one stop darker.
 */
class Exposure(private val ev: Float) : Adjustment {
    override val id = AdjustmentId("exposure")
    override val order = Order.EXPOSURE
    override fun isIdentity() = ev == 0f

    override fun apply(input: ImageBuffer): ImageBuffer {
        val factor = pow2(ev)
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            out[i]     = p[i]     * factor  // R
            out[i + 1] = p[i + 1] * factor  // G
            out[i + 2] = p[i + 2] * factor  // B
            out[i + 3] = p[i + 3]           // A — untouched
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }

    // exp(x * ln2) == 2^x, computed without kotlin.math.pow for multiplatform compat.
    private fun pow2(x: Float): Float = kotlin.math.exp(x * 0.6931472f)
}
