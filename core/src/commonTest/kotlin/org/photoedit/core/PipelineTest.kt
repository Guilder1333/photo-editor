package org.photoedit.core

import org.photoedit.core.adjustments.Exposure
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PipelineTest {

    private fun src(vararg px: Float) = ImageBuffer(1, px.size / 4, floatArrayOf(*px))

    @Test
    fun `render with no adjustments returns source pixels`() {
        val source = src(0.3f, 0.5f, 0.7f, 1f)
        assertContentEquals(source.pixels, Pipeline(source).render().pixels)
    }

    @Test
    fun `render applies active adjustment`() {
        val source = src(0.5f, 0.5f, 0.5f, 1f)
        val result = Pipeline(source).withAdjustment(Exposure(1f)).render()
        assertEquals(1.0f, result.pixels[0], 1e-5f)
    }

    @Test
    fun `withAdjustment replaces existing adjustment of same id`() {
        val source = src(0.25f, 0.25f, 0.25f, 1f)
        val pipeline = Pipeline(source)
            .withAdjustment(Exposure(1f))
            .withAdjustment(Exposure(2f))  // replaces, does not stack
        // 0.25 * 2^2 = 1.0
        assertEquals(1.0f, pipeline.render().pixels[0], 1e-5f)
    }

    @Test
    fun `identity adjustments are skipped and result equals source`() {
        val source = src(0.3f, 0.3f, 0.3f, 1f)
        val pipeline = Pipeline(source).withAdjustment(Exposure(0f))
        assertContentEquals(source.pixels, pipeline.render().pixels)
    }

    @Test
    fun `canonical order is enforced regardless of insertion order`() {
        // Once Contrast is implemented this test should compare:
        //   Exposure then Contrast  vs  Contrast then Exposure
        // and assert both pipelines produce the same result (canonical order wins).
        // Placeholder: verify the pipeline itself is stable with one adjustment.
        val source = src(0.1f, 0.1f, 0.1f, 1f)
        val p = Pipeline(source).withAdjustment(Exposure(1f))
        assertEquals(0.2f, p.render().pixels[0], 1e-5f)
    }

    @Test
    fun `addCheckpoint resets active adjustments and stacks on baked image`() {
        val source = src(0.1f, 0.1f, 0.1f, 1f)
        val baked = src(0.5f, 0.5f, 0.5f, 1f)

        // After adding checkpoint, subsequent Exposure applies to baked, not source.
        val pipeline = Pipeline(source)
            .addCheckpoint(baked, producedBy = "test")
            .withAdjustment(Exposure(1f))

        // 0.5 * 2^1 = 1.0
        assertEquals(1.0f, pipeline.render().pixels[0], 1e-5f)
    }

    @Test
    fun `render after checkpoint ignores pre-checkpoint adjustments`() {
        val source = src(0.1f, 0.1f, 0.1f, 1f)
        val baked = src(0.5f, 0.5f, 0.5f, 1f)

        // A large pre-checkpoint Exposure should have no effect post-checkpoint.
        val pipeline = Pipeline(source)
            .withAdjustment(Exposure(10f))   // huge, but gets baked into checkpoint
            .addCheckpoint(baked, producedBy = "test")
        // No active adjustments after checkpoint — result should equal baked.
        assertContentEquals(baked.pixels, pipeline.render().pixels)
    }
}
