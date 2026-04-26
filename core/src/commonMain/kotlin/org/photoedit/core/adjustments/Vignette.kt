package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.Order
import kotlin.math.sqrt

/**
 * Radial vignette: darkens or brightens image edges relative to the center.
 *
 * Each pixel's distance from the image center is computed in normalized coordinates
 * where the corner is at distance 1.0. A smoothstep falloff controlled by [feather]
 * determines where the effect starts; [strength] controls the direction and intensity.
 *
 * Formula per pixel:
 * ```
 * dist      = sqrt(dx² + dy²)          // normalized; 0 at center, 1 at corner
 * edge      = 1 - feather
 * t         = clamp((dist − edge) / feather, 0, 1)
 * influence = t² × (3 − 2t)            // smoothstep
 * out       = clamp(in × (1 − strength × influence), 0, 1)
 * ```
 *
 * @param strength [-1, 1]. Positive = darken edges. Negative = brighten edges. 0 = no change.
 * @param feather  [0.01, 1]. Controls the transition width. Higher = softer falloff.
 */
class Vignette(val strength: Float, val feather: Float = 0.5f) : Adjustment {
    override val id = AdjustmentId("vignette")
    override val order = Order.VIGNETTE
    override fun isIdentity() = strength == 0f

    override fun apply(input: ImageBuffer): ImageBuffer {
        val w = input.width
        val h = input.height
        val p = input.pixels
        val out = FloatArray(p.size)

        val effectiveFeather = feather.coerceAtLeast(0.01f)
        val edge = 1f - effectiveFeather

        for (y in 0 until h) {
            for (x in 0 until w) {
                val dx = if (w <= 1) 0f else (2f * x / (w - 1)) - 1f   // [-1, 1]
                val dy = if (h <= 1) 0f else (2f * y / (h - 1)) - 1f
                val dist = sqrt(dx * dx + dy * dy)                    // 0 at center, ~1.41 at corners
                // Normalize so the corner distance maps to 1.0
                val normDist = dist / sqrt(2f)

                val t = ((normDist - edge) / effectiveFeather).coerceIn(0f, 1f)
                val influence = t * t * (3f - 2f * t)   // smoothstep

                val factor = 1f - strength * influence
                val base = (y * w + x) * 4
                out[base]     = (p[base]     * factor).coerceIn(0f, 1f)
                out[base + 1] = (p[base + 1] * factor).coerceIn(0f, 1f)
                out[base + 2] = (p[base + 2] * factor).coerceIn(0f, 1f)
                out[base + 3] = p[base + 3]
            }
        }
        return ImageBuffer(w, h, out)
    }
}
