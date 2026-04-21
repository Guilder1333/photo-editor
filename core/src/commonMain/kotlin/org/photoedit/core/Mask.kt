package org.photoedit.core

/**
 * Grayscale influence mask for selective adjustments.
 *
 * Each value in [values] corresponds to one pixel (row-major, matching [ImageBuffer]
 * layout). A value of `1.0` means the adjustment is applied at full strength; `0.0`
 * leaves the pixel unchanged. Intermediate values linearly blend the original and
 * adjusted pixels.
 */
class Mask(
    val width: Int,
    val height: Int,
    val values: FloatArray,
) {
    init {
        require(values.size == width * height) {
            "values.size must equal width * height " +
                "(expected ${width * height}, got ${values.size})"
        }
    }

    companion object {
        /** A mask that applies an adjustment at full strength everywhere. */
        fun full(width: Int, height: Int) =
            Mask(width, height, FloatArray(width * height) { 1f })

        /** A mask that suppresses an adjustment everywhere (no-op mask). */
        fun empty(width: Int, height: Int) =
            Mask(width, height, FloatArray(width * height) { 0f })
    }
}
