package org.photoedit.core.adjustments

import org.photoedit.core.ImageBuffer
import org.photoedit.core.assertNear
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VignetteTest {

    // 3×3 uniform image so we can inspect center vs corner pixels
    private fun uniform3x3(v: Float): ImageBuffer {
        val pixels = FloatArray(3 * 3 * 4) { if (it % 4 == 3) 1f else v }
        return ImageBuffer(3, 3, pixels)
    }

    private fun px(r: Float, g: Float, b: Float, a: Float = 1f) =
        ImageBuffer(1, 1, floatArrayOf(r, g, b, a))

    // ── Identity ──────────────────────────────────────────────────────────────

    @Test fun `Vignette isIdentity at strength 0`() = assertTrue(Vignette(0f).isIdentity())
    @Test fun `Vignette isIdentity false for non-zero strength`() = assertFalse(Vignette(0.5f).isIdentity())

    @Test
    fun `Vignette(0) leaves image unchanged`() {
        val input = uniform3x3(0.8f)
        assertContentEquals(input.pixels, Vignette(0f).apply(input).pixels)
    }

    // ── Direction ─────────────────────────────────────────────────────────────

    @Test
    fun `positive strength darkens corners more than center`() {
        val out = Vignette(1f, feather = 1f).apply(uniform3x3(0.8f)).pixels
        val center = out[(1 * 3 + 1) * 4]        // middle pixel
        val corner = out[(0 * 3 + 0) * 4]        // top-left corner
        assertTrue(corner <= center, "corner should be darker than or equal to center")
    }

    @Test
    fun `negative strength brightens corners relative to center`() {
        val input = uniform3x3(0.5f)
        val out = Vignette(-1f, feather = 1f).apply(input).pixels
        val center = out[(1 * 3 + 1) * 4]
        val corner = out[(0 * 3 + 0) * 4]
        assertTrue(corner >= center, "corner should be lighter than or equal to center with negative strength")
    }

    // ── Center pixel ──────────────────────────────────────────────────────────

    @Test
    fun `center pixel of odd-sized image is unaffected by vignette`() {
        // For a 3×3 image, center pixel is at (1,1): dx=0, dy=0, dist=0, influence=0
        val input = uniform3x3(0.6f)
        val out = Vignette(1f, feather = 0.5f).apply(input).pixels
        val centerIdx = (1 * 3 + 1) * 4
        assertNear(0.6f, out[centerIdx], message = "center R")
    }

    // ── Alpha ─────────────────────────────────────────────────────────────────

    @Test
    fun `Vignette does not modify alpha`() {
        val input = px(0.5f, 0.5f, 0.5f, 0.8f)
        assertNear(0.8f, Vignette(1f).apply(input).pixels[3])
    }

    // ── Output bounds ─────────────────────────────────────────────────────────

    @Test
    fun `output stays in 0 to 1 for extreme strength`() {
        val out = Vignette(1f, feather = 0.1f).apply(uniform3x3(1f)).pixels
        out.forEachIndexed { i, v -> if (i % 4 != 3) assertTrue(v in 0f..1f, "channel $i out of range") }
    }

    // ── Immutability ──────────────────────────────────────────────────────────

    @Test
    fun `Vignette does not mutate input`() {
        val input = uniform3x3(0.7f)
        val snap = input.pixels.copyOf()
        Vignette(0.5f).apply(input)
        assertContentEquals(snap, input.pixels)
    }

    // ── Single-pixel image ────────────────────────────────────────────────────

    @Test
    fun `Vignette on 1x1 image leaves pixel unchanged (dist=0)`() {
        val input = px(0.5f, 0.5f, 0.5f)
        assertContentEquals(input.pixels, Vignette(1f).apply(input).pixels)
    }
}
