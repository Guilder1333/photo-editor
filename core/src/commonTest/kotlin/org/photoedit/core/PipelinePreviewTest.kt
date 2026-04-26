package org.photoedit.core

import kotlinx.coroutines.test.runTest
import org.photoedit.core.adjustments.Exposure
import org.photoedit.core.adjustments.Brightness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PipelinePreviewTest {

    // 8×8 uniform source — large enough to be downscaled but simple to reason about.
    private fun src(v: Float = 0.5f): ImageBuffer {
        val pixels = FloatArray(8 * 8 * 4) { if (it % 4 < 3) v else 1f }
        return ImageBuffer(8, 8, pixels)
    }

    // ── renderPreview dimensions ───────────────────────────────────────────────

    @Test
    fun `renderPreview returns image within requested bounds`() {
        val out = Pipeline(src()).renderPreview(4, 4)
        assertTrue(out.width  <= 4)
        assertTrue(out.height <= 4)
    }

    @Test
    fun `renderPreview with bounds larger than source returns full-resolution image`() {
        val out = Pipeline(src()).renderPreview(100, 100)
        assertEquals(8, out.width)
        assertEquals(8, out.height)
    }

    @Test
    fun `renderPreview preserves aspect ratio`() {
        // 8×8 source, max 4×8 → scale 0.5 on width → 4×4
        val out = Pipeline(src()).renderPreview(4, 8)
        assertEquals(4, out.width)
        assertEquals(4, out.height)
    }

    // ── renderPreview applies adjustments correctly ───────────────────────────

    @Test
    fun `renderPreview with no adjustments equals downscaled source`() {
        val source = src(0.4f)
        val pipeline = Pipeline(source)
        val preview = pipeline.renderPreview(4, 4)
        val direct  = source.downscale(4, 4)
        for (i in preview.pixels.indices) {
            assertNear(direct.pixels[i], preview.pixels[i], epsilon = 0.002f, message = "pixel[$i]")
        }
    }

    @Test
    fun `renderPreview applies adjustment on downscaled buffer`() {
        // Exposure(1f) doubles pixel values; source is 0.5 → output ≈ 1.0
        val out = Pipeline(src(0.5f)).withAdjustment(Exposure(1f)).renderPreview(4, 4)
        assertNear(1.0f, out.pixels[0], epsilon = 0.002f, message = "R after +1 EV")
    }

    @Test
    fun `renderPreview and render agree on adjustment results`() {
        // Both should apply the same adjustment; preview pixel values should match
        // the full render (downscaled source with same adjustment applied).
        val source = src(0.25f)
        val pipeline = Pipeline(source).withAdjustment(Exposure(1f)) // 0.25 × 2 = 0.5
        val fullRender    = pipeline.render()
        val previewRender = pipeline.renderPreview(4, 4)

        // After Exposure(1f), every channel (except A) should be ≈ 0.5
        assertNear(0.5f, fullRender.pixels[0],    epsilon = 0.002f, message = "full render R")
        assertNear(0.5f, previewRender.pixels[0], epsilon = 0.002f, message = "preview render R")
    }

    // ── renderPreviewAsync ────────────────────────────────────────────────────

    @Test
    fun `renderPreviewAsync emits Complete with downscaled image`() = runTest {
        val events = mutableListOf<RenderProgress>()
        Pipeline(src()).renderPreviewAsync(4, 4).collect { events.add(it) }

        val complete = events.filterIsInstance<RenderProgress.Complete>().firstOrNull()
        assertNotNull(complete, "should emit Complete")
        assertTrue(complete.image.width  <= 4)
        assertTrue(complete.image.height <= 4)
    }

    @Test
    fun `renderPreviewAsync emits InProgress events for each adjustment`() = runTest {
        val pipeline = Pipeline(src())
            .withAdjustment(Exposure(1f))
            .withAdjustment(Brightness(0.1f))

        val events = mutableListOf<RenderProgress>()
        pipeline.renderPreviewAsync(4, 4).collect { events.add(it) }

        val inProgress = events.filterIsInstance<RenderProgress.InProgress>()
        assertEquals(2, inProgress.size, "should emit one InProgress per non-identity adjustment")
        assertEquals(100, inProgress.last().percent, "last InProgress should be 100%")
    }

    @Test
    fun `renderPreviewAsync with identity pipeline emits single Complete`() = runTest {
        val events = mutableListOf<RenderProgress>()
        Pipeline(src()).renderPreviewAsync(4, 4).collect { events.add(it) }

        assertEquals(1, events.size)
        assertTrue(events[0] is RenderProgress.Complete)
    }

    // ── PreviewSize ───────────────────────────────────────────────────────────

    @Test
    fun `PreviewSize stores maxWidth and maxHeight`() {
        val ps = PreviewSize(320, 240)
        assertEquals(320, ps.maxWidth)
        assertEquals(240, ps.maxHeight)
    }

    // ── Checkpoint interaction ────────────────────────────────────────────────

    @Test
    fun `renderPreview downscales from last checkpoint, not original source`() {
        val source     = src(0.1f)
        val checkpoint = src(0.8f)   // baked state — much brighter

        val pipeline = Pipeline(source)
            .addCheckpoint(checkpoint, producedBy = "test")

        val preview = pipeline.renderPreview(4, 4)
        // No adjustments after checkpoint, so preview should match downscaled checkpoint
        assertNear(0.8f, preview.pixels[0], epsilon = 0.01f, message = "R should come from checkpoint")
    }
}
