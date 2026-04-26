package org.photoedit.core

/**
 * Platform-agnostic in-memory image representation.
 *
 * Pixels are stored as planar float32 RGBA in linear light (not sRGB-encoded).
 * Layout: R G B A  R G B A  … (row-major, left-to-right, top-to-bottom).
 * Alpha is preserved by all tone/color adjustments and is only meaningful at
 * display time.
 *
 * Platform adapters are responsible for sRGB ↔ linear conversion at load/save.
 * 8-bit conversion happens only at display time to avoid banding.
 */
class ImageBuffer(
    val width: Int,
    val height: Int,
    val pixels: FloatArray,  // size == width * height * 4
) {
    init {
        require(pixels.size == width * height * 4) {
            "pixels.size must equal width * height * 4 " +
                "(expected ${width * height * 4}, got ${pixels.size})"
        }
    }

    /** Returns a deep copy; safe to pass to parallel adjustments. */
    fun copy(): ImageBuffer = ImageBuffer(width, height, pixels.copyOf())

    /**
     * Returns a new [ImageBuffer] scaled down to fit within [maxWidth] × [maxHeight]
     * while preserving the aspect ratio. If the image already fits, returns `this`.
     *
     * Downsampling uses a box filter: each output pixel is the average of all source
     * pixels that map to it, which gives good quality for large reduction factors and
     * is fast enough for preview use.
     *
     * @param maxWidth  Maximum output width in pixels (clamped to at least 1).
     * @param maxHeight Maximum output height in pixels (clamped to at least 1).
     */
    fun downscale(maxWidth: Int, maxHeight: Int): ImageBuffer {
        val mw = maxWidth.coerceAtLeast(1)
        val mh = maxHeight.coerceAtLeast(1)
        if (width <= mw && height <= mh) return this

        val scale = minOf(mw.toFloat() / width, mh.toFloat() / height)
        val dstW = (width  * scale).toInt().coerceAtLeast(1)
        val dstH = (height * scale).toInt().coerceAtLeast(1)

        val xScale = width.toFloat()  / dstW
        val yScale = height.toFloat() / dstH
        val out = FloatArray(dstW * dstH * 4)

        for (dy in 0 until dstH) {
            val y0 = (dy       * yScale).toInt().coerceIn(0, height - 1)
            val y1 = ((dy + 1) * yScale).toInt().coerceAtMost(height)
            for (dx in 0 until dstW) {
                val x0 = (dx       * xScale).toInt().coerceIn(0, width - 1)
                val x1 = ((dx + 1) * xScale).toInt().coerceAtMost(width)
                var r = 0f; var g = 0f; var b = 0f; var a = 0f; var n = 0
                for (sy in y0 until y1) {
                    for (sx in x0 until x1) {
                        val s = (sy * width + sx) * 4
                        r += pixels[s]; g += pixels[s + 1]; b += pixels[s + 2]; a += pixels[s + 3]
                        n++
                    }
                }
                val base = (dy * dstW + dx) * 4
                if (n > 0) {
                    val inv = 1f / n
                    out[base] = r * inv; out[base + 1] = g * inv; out[base + 2] = b * inv; out[base + 3] = a * inv
                } else {
                    // Fallback: nearest-neighbour (only reachable if xScale < 1 or yScale < 1)
                    val s = (y0 * width + x0) * 4
                    out[base] = pixels[s]; out[base + 1] = pixels[s + 1]; out[base + 2] = pixels[s + 2]; out[base + 3] = pixels[s + 3]
                }
            }
        }
        return ImageBuffer(dstW, dstH, out)
    }
}
