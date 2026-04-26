package org.photoedit.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ImageBufferDownscaleTest {

    // Build a width×height image where each pixel's R channel = its index / pixelCount,
    // so we can verify averaging behaviour.
    private fun gradient(width: Int, height: Int): ImageBuffer {
        val n = width * height
        val pixels = FloatArray(n * 4)
        for (i in 0 until n) {
            pixels[i * 4]     = i.toFloat() / n   // R = position fraction
            pixels[i * 4 + 1] = 0.5f
            pixels[i * 4 + 2] = 0.25f
            pixels[i * 4 + 3] = 1f
        }
        return ImageBuffer(width, height, pixels)
    }

    private fun uniform(width: Int, height: Int, v: Float): ImageBuffer {
        val pixels = FloatArray(width * height * 4) { if (it % 4 < 3) v else 1f }
        return ImageBuffer(width, height, pixels)
    }

    // ── Identity: image already fits ─────────────────────────────────────────

    @Test
    fun `downscale returns this when image already fits within bounds`() {
        val img = uniform(100, 80, 0.5f)
        assertSame(img, img.downscale(100, 80))
        assertSame(img, img.downscale(200, 200))
        assertSame(img, img.downscale(100, 200))
        assertSame(img, img.downscale(200, 80))
    }

    // ── Output dimensions ─────────────────────────────────────────────────────

    @Test
    fun `downscale halves a 4x4 image to 2x2`() {
        val out = uniform(4, 4, 0.5f).downscale(2, 2)
        assertEquals(2, out.width)
        assertEquals(2, out.height)
    }

    @Test
    fun `downscale preserves aspect ratio when constrained by width`() {
        // 400×200 image, max 200×200 → scale by 0.5 → 200×100
        val out = uniform(400, 200, 0.5f).downscale(200, 200)
        assertEquals(200, out.width)
        assertEquals(100, out.height)
    }

    @Test
    fun `downscale preserves aspect ratio when constrained by height`() {
        // 200×400 image, max 200×200 → scale by 0.5 → 100×200
        val out = uniform(200, 400, 0.5f).downscale(200, 200)
        assertEquals(100, out.width)
        assertEquals(200, out.height)
    }

    @Test
    fun `downscale never produces a 0x0 image`() {
        val out = uniform(1000, 1000, 0.5f).downscale(1, 1)
        assertTrue(out.width  >= 1)
        assertTrue(out.height >= 1)
    }

    @Test
    fun `downscale pixel count matches width times height`() {
        val out = uniform(640, 480, 0.5f).downscale(320, 240)
        assertEquals(out.width * out.height * 4, out.pixels.size)
    }

    // ── Box-filter averaging ──────────────────────────────────────────────────

    @Test
    fun `downscale of uniform image preserves pixel value`() {
        val out = uniform(100, 100, 0.7f).downscale(50, 50)
        // uniform() sets R = G = B = 0.7, A = 1.0
        for (i in 0 until out.pixels.size step 4) {
            assertNear(0.7f, out.pixels[i],     epsilon = 0.002f, message = "R at $i")
            assertNear(0.7f, out.pixels[i + 1], epsilon = 0.002f, message = "G at $i")
            assertNear(0.7f, out.pixels[i + 2], epsilon = 0.002f, message = "B at $i")
            assertNear(1f,   out.pixels[i + 3], epsilon = 0.002f, message = "A at $i")
        }
    }

    @Test
    fun `downscale 2x2 to 1x1 averages all four pixels`() {
        // Four pixels: R = 0, 0.4, 0.6, 1.0 → average = 0.5
        val pixels = floatArrayOf(
            0f,   0f, 0f, 1f,
            0.4f, 0f, 0f, 1f,
            0.6f, 0f, 0f, 1f,
            1f,   0f, 0f, 1f,
        )
        val img = ImageBuffer(2, 2, pixels)
        val out = img.downscale(1, 1)
        assertEquals(1, out.width)
        assertEquals(1, out.height)
        assertNear(0.5f, out.pixels[0], epsilon = 0.002f, message = "R average")
    }

    @Test
    fun `downscale of 1x1 image is identity`() {
        val img = ImageBuffer(1, 1, floatArrayOf(0.3f, 0.6f, 0.9f, 1f))
        val out = img.downscale(1, 1)
        assertSame(img, out)
    }

    // ── Does not mutate input ─────────────────────────────────────────────────

    @Test
    fun `downscale does not mutate source pixels`() {
        val img = uniform(4, 4, 0.5f)
        val snap = img.pixels.copyOf()
        img.downscale(2, 2)
        for (i in snap.indices) assertNear(snap[i], img.pixels[i], message = "pixel[$i] mutated")
    }
}
