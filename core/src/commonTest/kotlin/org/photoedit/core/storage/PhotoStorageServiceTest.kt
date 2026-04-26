package org.photoedit.core.storage

import org.photoedit.core.ImageBuffer
import org.photoedit.core.Pipeline
import org.photoedit.core.PreviewSize
import org.photoedit.core.adjustments.Exposure
import org.photoedit.core.adjustments.Brightness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotoStorageServiceTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    /** In-memory PhotoStore — no filesystem involved. */
    private class FakeStore : PhotoStore {
        val data = mutableMapOf<Pair<String, DataSlot>, ByteArray>()

        override fun write(photoId: String, slot: DataSlot, data: ByteArray) {
            this.data[photoId to slot] = data
        }
        override fun read(photoId: String, slot: DataSlot) = data[photoId to slot]
        override fun delete(photoId: String) { data.keys.removeAll { it.first == photoId } }
        override fun listPhotoIds() = data.keys.map { it.first }.distinct().sorted()
    }

    /**
     * Lossless fake codec: encodes as raw float32 RGBA bytes so round-trips are exact.
     * The simple format is: 4-byte width (big-endian int), 4-byte height, then pixel floats.
     */
    private class FakeCodec : ImageCodec {
        override fun encodeJpeg(image: ImageBuffer, quality: Int) = encode(image)
        override fun encodePng(image: ImageBuffer)                 = encode(image)

        private fun encode(image: ImageBuffer): ByteArray {
            val out = ByteArray(8 + image.pixels.size * 4)
            writeInt(out, 0, image.width)
            writeInt(out, 4, image.height)
            var off = 8
            for (f in image.pixels) {
                val bits = f.toBits()
                out[off++] = (bits shr 24).toByte(); out[off++] = (bits shr 16).toByte()
                out[off++] = (bits shr  8).toByte(); out[off++] = bits.toByte()
            }
            return out
        }

        override fun decode(bytes: ByteArray): ImageBuffer {
            val w = readInt(bytes, 0); val h = readInt(bytes, 4)
            val pixels = FloatArray(w * h * 4)
            var off = 8
            for (i in pixels.indices) {
                val bits = (bytes[off].toInt() and 0xFF shl 24) or
                           (bytes[off+1].toInt() and 0xFF shl 16) or
                           (bytes[off+2].toInt() and 0xFF shl  8) or
                           (bytes[off+3].toInt() and 0xFF)
                pixels[i] = Float.fromBits(bits); off += 4
            }
            return ImageBuffer(w, h, pixels)
        }

        private fun writeInt(buf: ByteArray, off: Int, v: Int) {
            buf[off] = (v shr 24).toByte(); buf[off+1] = (v shr 16).toByte()
            buf[off+2] = (v shr 8).toByte(); buf[off+3] = v.toByte()
        }
        private fun readInt(buf: ByteArray, off: Int) =
            (buf[off].toInt() and 0xFF shl 24) or (buf[off+1].toInt() and 0xFF shl 16) or
            (buf[off+2].toInt() and 0xFF shl  8) or (buf[off+3].toInt() and 0xFF)
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private val store   = FakeStore()
    private val codec   = FakeCodec()
    private val service = PhotoStorageService(
        store         = store,
        codec         = codec,
        previewSize   = PreviewSize(4, 4),    // tiny so tests run fast
        thumbnailSize = PreviewSize(2, 2),
    )

    private fun src(v: Float = 0.5f): ImageBuffer {
        val pixels = FloatArray(8 * 8 * 4) { if (it % 4 < 3) v else 1f }
        return ImageBuffer(8, 8, pixels)
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    fun `save writes all four slots`() {
        service.save("p1", Pipeline(src()))
        DataSlot.entries.forEach { slot ->
            assertNotNull(store.read("p1", slot), "$slot should be stored")
        }
    }

    @Test
    fun `save overwrites previous data`() {
        val p = Pipeline(src(0.3f))
        service.save("p1", p)
        service.save("p1", p)  // second call should not throw
        assertNotNull(store.read("p1", DataSlot.ORIGINAL))
    }

    // ── loadPipeline ──────────────────────────────────────────────────────────

    @Test
    fun `loadPipeline returns null for unknown photoId`() {
        assertNull(service.loadPipeline("unknown"))
    }

    @Test
    fun `loadPipeline restores source image`() {
        val source = src(0.4f)
        service.save("p1", Pipeline(source))
        val loaded = service.loadPipeline("p1")!!
        // Source pixels should survive encode → decode round-trip via FakeCodec
        for (i in source.pixels.indices) {
            assertEquals(source.pixels[i], loaded.source.pixels[i], "pixel[$i]")
        }
    }

    @Test
    fun `loadPipeline restores adjustments`() {
        val pipeline = Pipeline(src())
            .withAdjustment(Exposure(1f))
            .withAdjustment(Brightness(0.2f))
        service.save("p1", pipeline)

        val loaded = service.loadPipeline("p1")!!
        assertEquals(2, loaded.adjustments.size)
        assertTrue(loaded.adjustments.any { it is Exposure })
        assertTrue(loaded.adjustments.any { it is Brightness })
    }

    @Test
    fun `loadPipeline with no edits stored returns identity pipeline`() {
        // Write only original, no edits slot
        store.write("p1", DataSlot.ORIGINAL, codec.encodePng(src()))
        val loaded = service.loadPipeline("p1")!!
        assertEquals(0, loaded.adjustments.size)
    }

    @Test
    fun `render result is equivalent after save and load`() {
        val source = src(0.25f)
        val pipeline = Pipeline(source).withAdjustment(Exposure(1f))  // 0.25 × 2 = 0.5
        service.save("p1", pipeline)

        val loaded = service.loadPipeline("p1")!!
        val originalResult = pipeline.render().pixels[0]
        val restoredResult = loaded.render().pixels[0]
        assertEquals(originalResult, restoredResult, absoluteTolerance = 0.002f)
    }

    // ── loadThumbnail / loadPreview ───────────────────────────────────────────

    @Test
    fun `loadThumbnail returns null for unknown photo`() {
        assertNull(service.loadThumbnail("unknown"))
    }

    @Test
    fun `loadThumbnail returns image within thumbnail bounds`() {
        service.save("p1", Pipeline(src()))
        val thumb = service.loadThumbnail("p1")!!
        assertTrue(thumb.width  <= 2)
        assertTrue(thumb.height <= 2)
    }

    @Test
    fun `loadPreview returns image within preview bounds`() {
        service.save("p1", Pipeline(src()))
        val preview = service.loadPreview("p1")!!
        assertTrue(preview.width  <= 4)
        assertTrue(preview.height <= 4)
    }

    // ── delete / listPhotoIds ─────────────────────────────────────────────────

    @Test
    fun `delete removes all slots for the photo`() {
        service.save("p1", Pipeline(src()))
        service.delete("p1")
        assertNull(service.loadPipeline("p1"))
        assertNull(service.loadThumbnail("p1"))
    }

    @Test
    fun `listPhotoIds returns saved IDs`() {
        service.save("p1", Pipeline(src()))
        service.save("p2", Pipeline(src()))
        val ids = service.listPhotoIds()
        assertTrue("p1" in ids)
        assertTrue("p2" in ids)
    }

    @Test
    fun `listPhotoIds is empty when nothing saved`() {
        assertTrue(service.listPhotoIds().isEmpty())
    }
}
