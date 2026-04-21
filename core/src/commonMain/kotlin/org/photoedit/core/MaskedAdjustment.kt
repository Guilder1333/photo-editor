package org.photoedit.core

/**
 * Decorator that applies an [Adjustment] only where a [Mask] permits.
 *
 * For each pixel at index `p`, the output is a linear blend between the original
 * and the fully-adjusted value, weighted by `mask.values[p]`:
 *
 * ```
 * out = original + (adjusted - original) * mask[p]
 * ```
 *
 * Alpha is always taken from the original input — masks do not affect transparency.
 *
 * [id] and [order] are delegated to the wrapped adjustment so the pipeline treats a
 * masked adjustment identically to its unmasked counterpart for deduplication and
 * canonical ordering.
 */
class MaskedAdjustment(
    val adjustment: Adjustment,
    val mask: Mask,
) : Adjustment {
    override val id: AdjustmentId get() = adjustment.id
    override val order: Int get() = adjustment.order
    override fun isIdentity(): Boolean = adjustment.isIdentity()

    override fun apply(input: ImageBuffer): ImageBuffer {
        require(mask.width == input.width && mask.height == input.height) {
            "Mask size (${mask.width}×${mask.height}) must match " +
                "image size (${input.width}×${input.height})"
        }
        val adjusted = adjustment.apply(input)
        val src = input.pixels
        val adj = adjusted.pixels
        val out = FloatArray(src.size)
        var i = 0
        var p = 0
        while (i < src.size) {
            val influence = mask.values[p++]
            out[i]     = src[i]     + (adj[i]     - src[i])     * influence  // R
            out[i + 1] = src[i + 1] + (adj[i + 1] - src[i + 1]) * influence  // G
            out[i + 2] = src[i + 2] + (adj[i + 2] - src[i + 2]) * influence  // B
            out[i + 3] = src[i + 3]                                            // A
            i += 4
        }
        return ImageBuffer(input.width, input.height, out)
    }
}

/** Wraps this adjustment with [mask] for selective application. */
fun Adjustment.withMask(mask: Mask): MaskedAdjustment = MaskedAdjustment(this, mask)
