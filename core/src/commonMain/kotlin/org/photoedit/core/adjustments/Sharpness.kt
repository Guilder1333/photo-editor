package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.Order

/**
 * Unsharp-mask sharpening: `out = clamp(original + value * (original - blur), 0, 1)`
 *
 * The blur is a 3×3 box blur applied to RGB channels with edge-clamping (border pixels
 * repeat at boundaries). Alpha is always copied unchanged.
 *
 * On a 1×1 image (or any uniform image) the blur equals the original, so sharpening has
 * no effect regardless of [value].
 *
 * @param value Sharpening amount in [0, 1]. 0 = no change. 1 = full unsharp-mask strength.
 */
class Sharpness(val value: Float) : Adjustment {
    override val id = AdjustmentId("sharpness")
    override val order = Order.SHARPEN
    override fun isIdentity() = value == 0f

    override fun apply(input: ImageBuffer): ImageBuffer {
        val w = input.width
        val h = input.height
        val p = input.pixels

        // Step 1: 3×3 box blur with edge-clamped borders, RGB channels only.
        val blur = FloatArray(p.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val base = (y * w + x) * 4
                for (c in 0..2) {
                    var sum = 0f
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val nx = (x + dx).coerceIn(0, w - 1)
                            val ny = (y + dy).coerceIn(0, h - 1)
                            sum += p[(ny * w + nx) * 4 + c]
                        }
                    }
                    blur[base + c] = sum / 9f
                }
                blur[base + 3] = p[base + 3]  // alpha pass-through
            }
        }

        // Step 2: Unsharp mask — add the high-frequency residual scaled by value.
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            out[i]     = (p[i]     + value * (p[i]     - blur[i]    )).coerceIn(0f, 1f)
            out[i + 1] = (p[i + 1] + value * (p[i + 1] - blur[i + 1])).coerceIn(0f, 1f)
            out[i + 2] = (p[i + 2] + value * (p[i + 2] - blur[i + 2])).coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]   // alpha unchanged
            i += 4
        }
        return ImageBuffer(w, h, out)
    }
}
