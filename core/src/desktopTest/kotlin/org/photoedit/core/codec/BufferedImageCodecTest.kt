package org.photoedit.core.codec

import org.photoedit.core.ImageBuffer
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BufferedImageCodecTest {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 0.005f) =
        assertEquals(expected, actual, eps)

    @Test
    fun `toImageBuffer preserves image dimensions`() {
        val img = BufferedImage(3, 5, BufferedImage.TYPE_INT_ARGB)
        val buf = img.toImageBuffer()
        assertEquals(3, buf.width)
        assertEquals(5, buf.height)
        assertEquals(3 * 5 * 4, buf.pixels.size)
    }

    @Test
    fun `toImageBuffer converts white pixel to linear 1`() {
        val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        img.setRGB(0, 0, 0xFFFFFFFF.toInt())
        val buf = img.toImageBuffer()
        assertNear(1f, buf.pixels[0])  // R
        assertNear(1f, buf.pixels[1])  // G
        assertNear(1f, buf.pixels[2])  // B
        assertNear(1f, buf.pixels[3])  // A
    }

    @Test
    fun `toImageBuffer converts black pixel to linear 0`() {
        val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        img.setRGB(0, 0, 0xFF000000.toInt())
        val buf = img.toImageBuffer()
        assertNear(0f, buf.pixels[0])
        assertNear(0f, buf.pixels[1])
        assertNear(0f, buf.pixels[2])
        assertNear(1f, buf.pixels[3])  // A
    }

    @Test
    fun `toImageBuffer applies sRGB to linear conversion`() {
        val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        // sRGB 128 ≈ 0.502 normalised → linear ≈ 0.216
        img.setRGB(0, 0, 0xFF808080.toInt())
        val linear = img.toImageBuffer().pixels[0]
        assertTrue(linear < 0.5f, "expected linear < 0.5 for sRGB 0.5, got $linear")
        assertNear(0.216f, linear, eps = 0.005f)
    }

    @Test
    fun `toBufferedImage preserves dimensions`() {
        val buf = ImageBuffer(4, 2, FloatArray(4 * 2 * 4) { 0.5f })
        val img = buf.toBufferedImage()
        assertEquals(4, img.width)
        assertEquals(2, img.height)
        assertEquals(BufferedImage.TYPE_INT_ARGB, img.type)
    }

    @Test
    fun `toBufferedImage converts linear 1 to white`() {
        val buf = ImageBuffer(1, 1, floatArrayOf(1f, 1f, 1f, 1f))
        val argb = buf.toBufferedImage().getRGB(0, 0)
        assertEquals(0xFF, argb shr 24 and 0xFF)  // A
        assertEquals(0xFF, argb shr 16 and 0xFF)  // R
        assertEquals(0xFF, argb shr  8 and 0xFF)  // G
        assertEquals(0xFF, argb        and 0xFF)  // B
    }

    @Test
    fun `toBufferedImage converts linear 0 to black`() {
        val buf = ImageBuffer(1, 1, floatArrayOf(0f, 0f, 0f, 1f))
        val argb = buf.toBufferedImage().getRGB(0, 0)
        assertEquals(0xFF, argb shr 24 and 0xFF)  // A
        assertEquals(0x00, argb shr 16 and 0xFF)  // R
        assertEquals(0x00, argb shr  8 and 0xFF)  // G
        assertEquals(0x00, argb        and 0xFF)  // B
    }

    @Test
    fun `round-trip BufferedImage through ImageBuffer and back`() {
        val original = BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)
        original.setRGB(0, 0, 0xFFFF0000.toInt())  // red
        original.setRGB(1, 0, 0xFF00FF00.toInt())  // green
        original.setRGB(0, 1, 0xFF0000FF.toInt())  // blue
        original.setRGB(1, 1, 0xFFFFFFFF.toInt())  // white

        val roundTripped = original.toImageBuffer().toBufferedImage()

        for (y in 0 until 2) {
            for (x in 0 until 2) {
                val orig  = original.getRGB(x, y)
                val rt    = roundTripped.getRGB(x, y)
                val diffR = Math.abs((orig shr 16 and 0xFF) - (rt shr 16 and 0xFF))
                val diffG = Math.abs((orig shr  8 and 0xFF) - (rt shr  8 and 0xFF))
                val diffB = Math.abs((orig        and 0xFF) - (rt        and 0xFF))
                assertTrue(diffR <= 1, "R diff at ($x,$y): $diffR")
                assertTrue(diffG <= 1, "G diff at ($x,$y): $diffG")
                assertTrue(diffB <= 1, "B diff at ($x,$y): $diffB")
            }
        }
    }
}
