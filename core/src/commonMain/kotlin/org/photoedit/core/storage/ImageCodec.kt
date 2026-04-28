package org.photoedit.core.storage

import org.photoedit.core.ImageBuffer

/**
 * Platform-specific image encoding and decoding.
 *
 * Implementations convert between [ImageBuffer] (linear-light float32 RGBA) and
 * compressed byte streams (JPEG or PNG). sRGB ↔ linear conversion must happen
 * inside the codec, matching the contract of the platform-specific codecs in
 * `commonMain/codec/`.
 *
 * Inject the platform implementation into [PhotoStorageService] at the application
 * boundary.
 */
interface ImageCodec {
    /**
     * Encodes [image] as a JPEG byte stream.
     *
     * @param quality Compression quality in [0, 100]. 85 is a good default.
     */
    fun encodeJpeg(image: ImageBuffer, quality: Int = 85): ByteArray

    /**
     * Encodes [image] as a lossless PNG byte stream.
     */
    fun encodePng(image: ImageBuffer): ByteArray

    /**
     * Decodes a JPEG or PNG byte stream to a linear-light [ImageBuffer].
     */
    fun decode(bytes: ByteArray): ImageBuffer
}
