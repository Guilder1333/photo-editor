package org.photoedit.core

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.photoedit.core.adjustments.Brightness
import org.photoedit.core.adjustments.Exposure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertContentEquals

class PipelineAsyncTest {

    private fun src(vararg px: Float) = ImageBuffer(1, px.size / 4, floatArrayOf(*px))

    @Test
    fun `renderAsync with no adjustments emits only Complete`() = runTest {
        val source = src(0.3f, 0.5f, 0.7f, 1f)
        val events = Pipeline(source).renderAsync().toList()

        assertEquals(1, events.size)
        val complete = assertIs<RenderProgress.Complete>(events[0])
        assertContentEquals(source.pixels, complete.image.pixels)
    }

    @Test
    fun `renderAsync emits InProgress then Complete for single adjustment`() = runTest {
        val source = src(0.5f, 0.5f, 0.5f, 1f)
        val events = Pipeline(source).withAdjustment(Exposure(1f)).renderAsync().toList()

        assertEquals(2, events.size)
        val progress = assertIs<RenderProgress.InProgress>(events[0])
        assertEquals(100, progress.percent)
        assertEquals(1, progress.step)
        assertEquals(1, progress.total)

        assertIs<RenderProgress.Complete>(events[1])
    }

    @Test
    fun `renderAsync emits one InProgress per active adjustment`() = runTest {
        val source = src(0.2f, 0.2f, 0.2f, 1f)
        val events = Pipeline(source)
            .withAdjustment(Exposure(1f))
            .withAdjustment(Brightness(0.1f))
            .renderAsync()
            .toList()

        // 2 InProgress + 1 Complete
        assertEquals(3, events.size)
        val p1 = assertIs<RenderProgress.InProgress>(events[0])
        assertEquals(50, p1.percent)
        assertEquals(1, p1.step)
        assertEquals(2, p1.total)

        val p2 = assertIs<RenderProgress.InProgress>(events[1])
        assertEquals(100, p2.percent)
        assertEquals(2, p2.step)
        assertEquals(2, p2.total)

        assertIs<RenderProgress.Complete>(events[2])
    }

    @Test
    fun `renderAsync Complete image matches synchronous render`() = runTest {
        val source = src(0.25f, 0.25f, 0.25f, 1f)
        val pipeline = Pipeline(source)
            .withAdjustment(Exposure(1f))
            .withAdjustment(Brightness(0.1f))

        val syncResult = pipeline.render()
        val events = pipeline.renderAsync().toList()
        val asyncResult = assertIs<RenderProgress.Complete>(events.last()).image

        assertContentEquals(syncResult.pixels, asyncResult.pixels)
    }

    @Test
    fun `renderAsync skips identity adjustments`() = runTest {
        val source = src(0.5f, 0.5f, 0.5f, 1f)
        // Exposure(0f) is identity — should not appear as a step
        val events = Pipeline(source).withAdjustment(Exposure(0f)).renderAsync().toList()

        assertEquals(1, events.size)
        assertIs<RenderProgress.Complete>(events[0])
    }

    @Test
    fun `renderAsync with checkpoint starts from baked image`() = runTest {
        val source = src(0.1f, 0.1f, 0.1f, 1f)
        val baked = src(0.5f, 0.5f, 0.5f, 1f)

        val events = Pipeline(source)
            .addCheckpoint(baked, producedBy = "test")
            .withAdjustment(Exposure(1f))
            .renderAsync()
            .toList()

        val result = assertIs<RenderProgress.Complete>(events.last()).image
        // 0.5 * 2^1 = 1.0
        assertEquals(1.0f, result.pixels[0], 1e-5f)
    }
}
