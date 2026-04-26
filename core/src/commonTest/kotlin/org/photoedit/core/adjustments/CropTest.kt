package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CropTest {

    // 4×4 image with distinct pixels: value = (y*4+x)/16 for easy verification
    private fun img4x4(): ImageBuffer {
        val pixels = FloatArray(4 * 4 * 4)
        for (y in 0 until 4) for (x in 0 until 4) {
            val base = (y * 4 + x) * 4
            val v = (y * 4 + x) / 16f
            pixels[base]     = v
            pixels[base + 1] = v
            pixels[base + 2] = v
            pixels[base + 3] = 1f
        }
        return ImageBuffer(4, 4, pixels)
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `Crop isIdentity when all fractions are 0`() = assertTrue(Crop().isIdentity())
    @Test fun `Crop is not identity when any fraction is non-zero`() = assertFalse(Crop(left = 0.1f).isIdentity())

    @Test
    fun `Crop(0,0,0,0) leaves image unchanged`() {
        val input = img4x4()
        val out = Crop().apply(input)
        assertEquals(4, out.width)
        assertEquals(4, out.height)
        assertContentEquals(input.pixels, out.pixels)
    }

    // ── Dimensions ────────────────────────────────────────────────────────────

    @Test
    fun `Crop reduces width correctly`() {
        // left=0.25 removes 1 column, right=0.25 removes 1 column → width = 2
        val out = Crop(left = 0.25f, right = 0.25f).apply(img4x4())
        assertEquals(2, out.width, "width should be 2")
        assertEquals(4, out.height, "height should be unchanged")
    }

    @Test
    fun `Crop reduces height correctly`() {
        // top=0.25 removes 1 row, bottom=0.25 removes 1 row → height = 2
        val out = Crop(top = 0.25f, bottom = 0.25f).apply(img4x4())
        assertEquals(4, out.width, "width should be unchanged")
        assertEquals(2, out.height, "height should be 2")
    }

    @Test
    fun `Crop with all sides reduces both dimensions`() {
        val out = Crop(left = 0.25f, top = 0.25f, right = 0.25f, bottom = 0.25f).apply(img4x4())
        assertEquals(2, out.width)
        assertEquals(2, out.height)
    }

    // ── Pixel content ─────────────────────────────────────────────────────────

    @Test
    fun `Crop preserves correct source pixels`() {
        // Remove left column: first column of output should be what was column 1
        val src = img4x4()
        val out = Crop(left = 0.25f).apply(src)  // removes 1 col from left
        assertEquals(3, out.width)

        // First pixel of output row 0 should be src (0, 1) = 1/16
        val expected = src.pixels[(0 * 4 + 1) * 4]  // y=0, x=1
        val actual   = out.pixels[0]
        assertEquals(expected, actual, "first pixel of cropped row 0 should match src(0,1)")
    }

    @Test
    fun `Crop with top removes top rows`() {
        val src = img4x4()
        val out = Crop(top = 0.25f).apply(src)   // removes 1 row from top
        assertEquals(3, out.height)

        // First pixel of output (row 0) should be src row 1, col 0 = 4/16
        val expected = src.pixels[(1 * 4 + 0) * 4]
        val actual   = out.pixels[0]
        assertEquals(expected, actual, "first pixel of output should come from src row 1")
    }

    // ── Minimum size guarantee ────────────────────────────────────────────────

    @Test
    fun `Crop never produces empty image even with extreme fractions`() {
        val out = Crop(left = 0.9f, right = 0.9f, top = 0.9f, bottom = 0.9f).apply(img4x4())
        assertTrue(out.width  >= 1, "width must be at least 1")
        assertTrue(out.height >= 1, "height must be at least 1")
    }

    @Test
    fun `Crop output pixel count equals width times height times 4`() {
        val out = Crop(left = 0.25f, top = 0.25f, right = 0.25f, bottom = 0.25f).apply(img4x4())
        assertEquals(out.width * out.height * 4, out.pixels.size)
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `Crop does not mutate input`() {
        val input = img4x4()
        val snap = input.pixels.copyOf()
        Crop(left = 0.25f, top = 0.25f).apply(input)
        assertContentEquals(snap, input.pixels)
    }
}
