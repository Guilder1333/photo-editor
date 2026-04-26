package org.photoedit.core.storage

import org.photoedit.core.ImageBuffer
import org.photoedit.core.codec.toBufferedImage
import org.photoedit.core.codec.toImageBuffer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * [ImageCodec] for desktop (JVM), backed by `javax.imageio`.
 *
 * Encoding converts the linear-light [ImageBuffer] to sRGB via [toBufferedImage]
 * before compression. Decoding converts the loaded sRGB image back to linear light
 * via [toImageBuffer]. Both conversions use the IEC 61966-2-1 transfer function
 * already implemented in the existing platform codecs.
 */
class DesktopImageCodec : ImageCodec {

    override fun encodeJpeg(image: ImageBuffer, quality: Int): ByteArray {
        val buffered = image.toBufferedImage()
        val writers = ImageIO.getImageWritersByFormatName("jpeg")
        check(writers.hasNext()) { "No JPEG ImageWriter found" }
        val writer = writers.next()
        val params = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = quality.coerceIn(0, 100) / 100f
        }
        val out = ByteArrayOutputStream()
        writer.output = ImageIO.createImageOutputStream(out)
        writer.write(null, IIOImage(buffered, null, null), params)
        writer.dispose()
        return out.toByteArray()
    }

    override fun encodePng(image: ImageBuffer): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(image.toBufferedImage(), "png", out)
        return out.toByteArray()
    }

    override fun decode(bytes: ByteArray): ImageBuffer =
        ImageIO.read(ByteArrayInputStream(bytes))
            ?.toImageBuffer()
            ?: error("Could not decode image bytes (unsupported format or corrupt data)")
}
