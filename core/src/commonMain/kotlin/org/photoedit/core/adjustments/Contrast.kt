package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.Order

/**
 * S-curve contrast adjustment pivoting around mid-grey (0.5 in linear light).
 *
 * Formula: `out = clamp((in - PIVOT) * (1 + value) + PIVOT, 0, 1)`
 *
 * @param value Contrast amount in [-1, 1].
 *   0 = no change.
 *   1 = maximum increase (factor 2×).
 *   -1 = flat grey (factor 0, everything collapses to [PIVOT]).
 */
class Contrast(val value: Float) : Adjustment {
    override val id = AdjustmentId("contrast")
    override val order = Order.CONTRAST
    override fun isIdentity() = value == 0f

    override fun apply(input: ImageBuffer): ImageBuffer {
        val factor = 1f + value
        val p = input.pixels
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            out[i]     = ((p[i]     - PIVOT) * factor + PIVOT).coerceIn(0f, 1f)
            out[i + 1] = ((p[i + 1] - PIVOT) * factor + PIVOT).coerceIn(0f, 1f)
            out[i + 2] = ((p[i + 2] - PIVOT) * factor + PIVOT).coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]   // alpha unchanged
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }

    companion object {
        /** Pivot point: pixels at exactly this value are unaffected by contrast changes. */
        const val PIVOT = 0.5f
    }
}
