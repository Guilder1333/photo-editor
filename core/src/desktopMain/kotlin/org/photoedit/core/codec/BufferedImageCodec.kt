package org.photoedit.core.codec

import org.photoedit.core.ImageBuffer
import java.awt.image.BufferedImage

/**
 * Converts a [BufferedImage] to a linear-light [ImageBuffer].
 *
 * Pixels are read via [BufferedImage.getRGB] which always returns non-premultiplied
 * ARGB regardless of the source image type. sRGB channel values are converted to
 * linear light at load time.
 */
fun BufferedImage.toImageBuffer(): ImageBuffer {
    val pixels = FloatArray(width * height * 4)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val argb = getRGB(x, y)
            val base = (y * width + x) * 4
            pixels[base    ] = (argb shr 16 and 0xFF).toFloat().div(255f).srgbToLinear()  // R
            pixels[base + 1] = (argb shr  8 and 0xFF).toFloat().div(255f).srgbToLinear()  // G
            pixels[base + 2] = (argb        and 0xFF).toFloat().div(255f).srgbToLinear()  // B
            pixels[base + 3] = (argb shr 24 and 0xFF).toFloat().div(255f)                 // A
        }
    }
    return ImageBuffer(width, height, pixels)
}

/**
 * Renders this [ImageBuffer] to a [BufferedImage.TYPE_INT_ARGB] image.
 *
 * Linear-light channel values are converted to sRGB at save time.
 */
fun ImageBuffer.toBufferedImage(): BufferedImage {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val base = (y * width + x) * 4
            val r = (pixels[base    ].linearToSrgb() * 255f + 0.5f).toInt().coerceIn(0, 255)
            val g = (pixels[base + 1].linearToSrgb() * 255f + 0.5f).toInt().coerceIn(0, 255)
            val b = (pixels[base + 2].linearToSrgb() * 255f + 0.5f).toInt().coerceIn(0, 255)
            val a = (pixels[base + 3]                * 255f + 0.5f).toInt().coerceIn(0, 255)
            image.setRGB(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
        }
    }
    return image
}
