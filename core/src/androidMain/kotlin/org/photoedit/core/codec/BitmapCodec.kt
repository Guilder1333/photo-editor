package org.photoedit.core.codec

import android.graphics.Bitmap
import org.photoedit.core.ImageBuffer

/**
 * Converts an Android [Bitmap] to a linear-light [ImageBuffer].
 *
 * The bitmap is first converted to [Bitmap.Config.ARGB_8888] if necessary.
 * sRGB-encoded channel values are converted to linear light at load time.
 * Alpha is preserved as-is (not premultiplied inside [ImageBuffer]).
 */
fun Bitmap.toImageBuffer(): ImageBuffer {
    val src = if (config == Bitmap.Config.ARGB_8888) this
              else copy(Bitmap.Config.ARGB_8888, false)
    val w = src.width
    val h = src.height
    val argb = IntArray(w * h)
    src.getPixels(argb, 0, w, 0, 0, w, h)
    if (src !== this) src.recycle()

    val pixels = FloatArray(w * h * 4)
    for (i in argb.indices) {
        val px = argb[i]
        val base = i * 4
        pixels[base]     = (px shr 16 and 0xFF).toFloat().div(255f).srgbToLinear()  // R
        pixels[base + 1] = (px shr  8 and 0xFF).toFloat().div(255f).srgbToLinear()  // G
        pixels[base + 2] = (px        and 0xFF).toFloat().div(255f).srgbToLinear()  // B
        pixels[base + 3] = (px shr 24 and 0xFF).toFloat().div(255f)                 // A
    }
    return ImageBuffer(w, h, pixels)
}

/**
 * Renders this [ImageBuffer] to an [Bitmap.Config.ARGB_8888] Android [Bitmap].
 *
 * Linear-light channel values are converted to sRGB at save time.
 */
fun ImageBuffer.toBitmap(): Bitmap {
    val argb = IntArray(width * height)
    for (i in argb.indices) {
        val base = i * 4
        val r = (pixels[base    ].linearToSrgb() * 255f + 0.5f).toInt().coerceIn(0, 255)
        val g = (pixels[base + 1].linearToSrgb() * 255f + 0.5f).toInt().coerceIn(0, 255)
        val b = (pixels[base + 2].linearToSrgb() * 255f + 0.5f).toInt().coerceIn(0, 255)
        val a = (pixels[base + 3]                * 255f + 0.5f).toInt().coerceIn(0, 255)
        argb[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(argb, 0, width, 0, 0, width, height)
    return bitmap
}
