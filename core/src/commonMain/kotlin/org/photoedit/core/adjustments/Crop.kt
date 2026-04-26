package org.photoedit.core.adjustments

import org.photoedit.core.Adjustment
import org.photoedit.core.AdjustmentId
import org.photoedit.core.ImageBuffer
import org.photoedit.core.Order

/**
 * Non-destructive crop: removes a fractional border from each edge.
 *
 * Each parameter is a fraction of the original dimension to remove from that edge,
 * in [0, 1). The output image contains only the pixels inside the crop rectangle.
 * If the combined left+right or top+bottom fractions would leave fewer than 1 pixel
 * in either dimension the crop is clamped so that at least 1 pixel remains.
 *
 * Because Crop changes image dimensions it has the lowest pipeline order ([Order.CROP])
 * so all subsequent adjustments operate on the already-cropped pixel region.
 *
 * @param left   Fraction [0, 1) to remove from the left edge.
 * @param top    Fraction [0, 1) to remove from the top edge.
 * @param right  Fraction [0, 1) to remove from the right edge.
 * @param bottom Fraction [0, 1) to remove from the bottom edge.
 */
class Crop(
    val left: Float   = 0f,
    val top: Float    = 0f,
    val right: Float  = 0f,
    val bottom: Float = 0f,
) : Adjustment {
    override val id = AdjustmentId("crop")
    override val order = Order.CROP
    override fun isIdentity() = left == 0f && top == 0f && right == 0f && bottom == 0f

    override fun apply(input: ImageBuffer): ImageBuffer {
        val srcW = input.width
        val srcH = input.height
        val p = input.pixels

        val x0 = (left.coerceIn(0f, 1f)   * srcW).toInt()
        val y0 = (top.coerceIn(0f, 1f)    * srcH).toInt()
        val x1 = srcW - (right.coerceIn(0f, 1f)  * srcW).toInt()
        val y1 = srcH - (bottom.coerceIn(0f, 1f) * srcH).toInt()

        // Guarantee at least a 1×1 output.
        val dstX0 = x0.coerceIn(0, srcW - 1)
        val dstY0 = y0.coerceIn(0, srcH - 1)
        val dstX1 = x1.coerceIn(dstX0 + 1, srcW)
        val dstY1 = y1.coerceIn(dstY0 + 1, srcH)

        val dstW = dstX1 - dstX0
        val dstH = dstY1 - dstY0
        val out = FloatArray(dstW * dstH * 4)

        for (y in 0 until dstH) {
            val srcRow = ((y + dstY0) * srcW + dstX0) * 4
            val dstRow = y * dstW * 4
            p.copyInto(out, dstRow, srcRow, srcRow + dstW * 4)
        }
        return ImageBuffer(dstW, dstH, out)
    }
}
