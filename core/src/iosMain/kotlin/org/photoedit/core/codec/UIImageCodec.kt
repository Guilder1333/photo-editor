package org.photoedit.core.codec

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.photoedit.core.ImageBuffer
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIImage

/**
 * Converts a [UIImage] to a linear-light [ImageBuffer].
 *
 * The image is drawn into an RGBA8888 CGBitmapContext with premultiplied alpha.
 * Channels are unpremultiplied then converted from sRGB to linear light.
 */
@OptIn(ExperimentalForeignApi::class)
fun UIImage.toImageBuffer(): ImageBuffer {
    val cgImage = this.CGImage ?: error("UIImage has no backing CGImage")
    val w = CGImageGetWidth(cgImage).toInt()
    val h = CGImageGetHeight(cgImage).toInt()
    val bytesPerRow = w * 4
    val rawBytes = ByteArray(h * bytesPerRow)

    rawBytes.usePinned { pinned ->
        val colorSpace = CGColorSpaceCreateDeviceRGB()!!
        val context = CGBitmapContextCreate(
            data = pinned.addressOf(0),
            width = w.toULong(),
            height = h.toULong(),
            bitsPerComponent = 8uL,
            bytesPerRow = bytesPerRow.toULong(),
            space = colorSpace,
            bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
        )!!
        CGContextDrawImage(context, CGRectMake(0.0, 0.0, w.toDouble(), h.toDouble()), cgImage)
        CGColorSpaceRelease(colorSpace)
        CGContextRelease(context)
    }

    val pixels = FloatArray(w * h * 4)
    for (p in 0 until w * h) {
        val b = p * 4
        val rPre = (rawBytes[b    ].toInt() and 0xFF) / 255f
        val gPre = (rawBytes[b + 1].toInt() and 0xFF) / 255f
        val bPre = (rawBytes[b + 2].toInt() and 0xFF) / 255f
        val a    = (rawBytes[b + 3].toInt() and 0xFF) / 255f
        val inv  = if (a > 0f) 1f / a else 0f
        pixels[b    ] = (rPre * inv).srgbToLinear()  // R
        pixels[b + 1] = (gPre * inv).srgbToLinear()  // G
        pixels[b + 2] = (bPre * inv).srgbToLinear()  // B
        pixels[b + 3] = a                             // A
    }
    return ImageBuffer(w, h, pixels)
}

/**
 * Renders this [ImageBuffer] to a [UIImage].
 *
 * Linear-light channels are converted to sRGB and premultiplied before being
 * written into a CGBitmapContext, from which a CGImage is extracted.
 */
@OptIn(ExperimentalForeignApi::class)
fun ImageBuffer.toUIImage(): UIImage {
    val bytesPerRow = width * 4
    val rawBytes = ByteArray(height * bytesPerRow)

    for (p in 0 until width * height) {
        val base = p * 4
        val r = pixels[base    ].linearToSrgb()
        val g = pixels[base + 1].linearToSrgb()
        val b = pixels[base + 2].linearToSrgb()
        val a = pixels[base + 3]
        // Premultiply for CGImage storage
        rawBytes[base    ] = (r * a * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
        rawBytes[base + 1] = (g * a * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
        rawBytes[base + 2] = (b * a * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
        rawBytes[base + 3] = (a       * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
    }

    var uiImage: UIImage? = null
    rawBytes.usePinned { pinned ->
        val colorSpace = CGColorSpaceCreateDeviceRGB()!!
        val context = CGBitmapContextCreate(
            data = pinned.addressOf(0),
            width = width.toULong(),
            height = height.toULong(),
            bitsPerComponent = 8uL,
            bytesPerRow = bytesPerRow.toULong(),
            space = colorSpace,
            bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
        )!!
        val cgImage = CGBitmapContextCreateImage(context)!!
        uiImage = UIImage(cgImage)
        CGColorSpaceRelease(colorSpace)
        CGContextRelease(context)
    }
    return uiImage!!
}
