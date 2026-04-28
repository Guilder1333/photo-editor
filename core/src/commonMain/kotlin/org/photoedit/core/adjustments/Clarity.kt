package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.AdjustmentType
import org.photoedit.core.ImageBuffer
import org.photoedit.core.LUM_B
import org.photoedit.core.LUM_G
import org.photoedit.core.LUM_R
import org.photoedit.core.Order
import kotlin.math.abs

/**
 * Mid-tone local contrast enhancement (clarity).
 *
 * Extracts high-frequency detail using a large-radius (5×5) box blur, then blends
 * that detail back with a strength proportional to the pixel's mid-tone luminance.
 * Shadows and highlights are left largely unaffected; contrast is boosted most where
 * luminance is near 0.5.
 *
 * Formula per pixel:
 * ```
 * localContrast = original − blur5x5
 * midMask       = max(0, 1 − |lum − 0.5| × 2)   // 1 at lum=0.5, 0 at lum=0 and 1
 * out           = clamp(original + value × midMask × localContrast, 0, 1)
 * ```
 *
 * @param value [0, 1]. 0 = no change. 1 = full clarity boost. Negative values reduce mid-tone contrast.
 */
class Clarity(val value: Float) : Adjustment {
    override val id = AdjustmentId("clarity")
    override val order = Order.CLARITY
    override fun isIdentity() = value == 0f
    override fun toFields() = listOf("value" to value)

    override fun apply(input: ImageBuffer): ImageBuffer {
        val w = input.width
        val h = input.height
        val p = input.pixels

        // 5×5 box blur (RGB only) with edge-clamping.
        val blur = FloatArray(p.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val base = (y * w + x) * 4
                for (c in 0..2) {
                    var sum = 0f
                    for (dy in -2..2) {
                        for (dx in -2..2) {
                            val nx = (x + dx).coerceIn(0, w - 1)
                            val ny = (y + dy).coerceIn(0, h - 1)
                            sum += p[(ny * w + nx) * 4 + c]
                        }
                    }
                    blur[base + c] = sum / 25f
                }
                blur[base + 3] = p[base + 3]
            }
        }

        val out = FloatArray(p.size)
        var i = 0
        while (i < p.size) {
            val r = p[i]; val g = p[i + 1]; val b = p[i + 2]
            val lum = LUM_R * r + LUM_G * g + LUM_B * b
            val midMask = maxOf(0f, 1f - abs(lum - 0.5f) * 2f)
            val scale = value * midMask
            out[i]     = (r + scale * (r - blur[i]    )).coerceIn(0f, 1f)
            out[i + 1] = (g + scale * (g - blur[i + 1])).coerceIn(0f, 1f)
            out[i + 2] = (b + scale * (b - blur[i + 2])).coerceIn(0f, 1f)
            out[i + 3] = p[i + 3]
            i += 4
        }
        return ImageBuffer(w, h, out)
    }

    companion object : AdjustmentType {
        override val typeKey = "clarity"
        override fun fromFields(fields: Map<String, String?>) = Clarity(fields["value"]!!.toFloat())
    }
}
