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
}
