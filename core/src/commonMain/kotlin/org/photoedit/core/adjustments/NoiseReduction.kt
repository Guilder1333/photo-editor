package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.Order

/**
 * Luminance noise reduction via a 5×5 Gaussian-weighted blur blended with the original.
 *
 * The blur is computed using a separable approximation of a Gaussian (σ ≈ 1.0) via
 * the kernel [1, 4, 6, 4, 1] / 16 applied horizontally then vertically. [strength]
 * controls the opacity of the blur — 0 leaves the image untouched, 1 replaces it
 * fully with the smoothed version. Alpha is always preserved.
 *
 * @param strength [0, 1]. 0 = no change. 1 = fully blurred.
 */
class NoiseReduction(val strength: Float) : Adjustment {
    override val id = AdjustmentId("noise_reduction")
    override val order = Order.NOISE_REDUCTION
    override fun isIdentity() = strength == 0f

    override fun apply(input: ImageBuffer): ImageBuffer {
        val w = input.width
        val h = input.height
        val p = input.pixels

        val kernel = floatArrayOf(1f, 4f, 6f, 4f, 1f)
        val kernelSum = 16f

        // Horizontal pass into temp buffer.
        val horiz = FloatArray(p.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val base = (y * w + x) * 4
                for (c in 0..2) {
                    var sum = 0f
                    for (k in -2..2) {
                        val nx = (x + k).coerceIn(0, w - 1)
                        sum += p[(y * w + nx) * 4 + c] * kernel[k + 2]
                    }
                    horiz[base + c] = sum / kernelSum
                }
                horiz[base + 3] = p[base + 3]
            }
        }

        // Vertical pass into blur buffer.
        val blur = FloatArray(p.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val base = (y * w + x) * 4
                for (c in 0..2) {
                    var sum = 0f
                    for (k in -2..2) {
                        val ny = (y + k).coerceIn(0, h - 1)
                        sum += horiz[(ny * w + x) * 4 + c] * kernel[k + 2]
                    }
                    blur[base + c] = sum / kernelSum
                }
                blur[base + 3] = horiz[base + 3]
            }
        }

        // Blend original with blurred result.
        val clamped = strength.coerceIn(0f, 1f)
        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            out[i]     = (p[i]     + clamped * (blur[i]     - p[i]    )).coerceIn(0f, 1f)
            out[i + 1] = (p[i + 1] + clamped * (blur[i + 1] - p[i + 1])).coerceIn(0f, 1f)
            out[i + 2] = (p[i + 2] + clamped * (blur[i + 2] - p[i + 2])).coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]
            i += 4
        }
        return ImageBuffer(w, h, out)
    }
}
