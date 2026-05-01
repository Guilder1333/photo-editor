package org.photoedit.core

import kotlin.test.Test
import kotlin.test.assertEquals

class HistogramTest {

    // ── computeHistogram ──────────────────────────────────────────────────────

    @Test
    fun allWhiteImagePopulatesOnlyBin255() {
        val buf = ImageBuffer(2, 2, FloatArray(16) { if (it % 4 == 3) 1f else 1f })
        val h = buf.computeHistogram()

        assertEquals(4, h.red  [255])
        assertEquals(4, h.green[255])
        assertEquals(4, h.blue [255])
        assertEquals(4, h.luma [255])
        for (bin in 0 until 255) {
            assertEquals(0, h.red  [bin], "red bin $bin should be 0")
            assertEquals(0, h.green[bin], "green bin $bin should be 0")
            assertEquals(0, h.blue [bin], "blue bin $bin should be 0")
            assertEquals(0, h.luma [bin], "luma bin $bin should be 0")
        }
    }

    @Test
    fun allBlackImagePopulatesOnlyBin0() {
        val buf = ImageBuffer(2, 2, FloatArray(16) { if (it % 4 == 3) 1f else 0f })
        val h = buf.computeHistogram()

        assertEquals(4, h.red  [0])
        assertEquals(4, h.green[0])
        assertEquals(4, h.blue [0])
        assertEquals(4, h.luma [0])
        for (bin in 1 until 256) {
            assertEquals(0, h.red  [bin], "red bin $bin should be 0")
            assertEquals(0, h.luma [bin], "luma bin $bin should be 0")
        }
    }

    @Test
    fun meansAreCorrectForMixedPixels() {
        // One pure-red pixel (R=1, G=0, B=0) and one pure-green pixel (R=0, G=1, B=0)
        val pixels = floatArrayOf(
            1f, 0f, 0f, 1f,
            0f, 1f, 0f, 1f,
        )
        val h = ImageBuffer(2, 1, pixels).computeHistogram()

        assertNear(0.5f, h.meanLinearR)
        assertNear(0.5f, h.meanLinearG)
        assertNear(0f,   h.meanLinearB)
    }

    @Test
    fun pixelCountMatchesWidthTimesHeight() {
        val h = ImageBuffer(3, 5, FloatArray(60)).computeHistogram()
        assertEquals(15, h.pixelCount)
    }

    // ── percentile ────────────────────────────────────────────────────────────

    @Test
    fun percentile0ReturnsFirstNonEmptyBin() {
        val channel = IntArray(256).also { it[42] = 1 }
        val h = makeHistogram(channel)
        assertEquals(42, h.percentileBin(channel, 0f))
    }

    @Test
    fun percentile1ReturnsLastNonEmptyBin() {
        val channel = IntArray(256).also { it[42] = 1 }
        val h = makeHistogram(channel)
        assertEquals(42, h.percentileBin(channel, 1f))
    }

    @Test
    fun percentileSplitsCorrectlyForTwoEquidistantBins() {
        // 100 pixels: 50 at bin 0, 50 at bin 200
        val channel = IntArray(256).also { it[0] = 50; it[200] = 50 }
        val h = makeHistogram(channel, pixelCount = 100)

        // p50: threshold = 50.0 → bin 0 cumulative = 50 >= 50 → returns bin 0
        assertEquals(0,   h.percentileBin(channel, 0.50f))
        // p51: threshold = 51.0 → bin 0 cumulative = 50 < 51 → bin 200 cumulative = 100 >= 51 → 200
        assertEquals(200, h.percentileBin(channel, 0.51f))
        // p99: threshold = 99.0 → same as p51 → 200
        assertEquals(200, h.percentileBin(channel, 0.99f))
    }

    @Test
    fun percentileNormalisedResultIsInUnitRange() {
        val channel = IntArray(256).also { it[128] = 10 }
        val h = makeHistogram(channel, pixelCount = 10)
        val v = h.percentile(channel, 0.5f)
        assert(v in 0f..1f) { "percentile result $v out of [0,1]" }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun makeHistogram(channel: IntArray, pixelCount: Int = channel.sum()) = Histogram(
        red = channel, green = channel, blue = channel, luma = channel,
        pixelCount = pixelCount,
        meanLinearR = 0f, meanLinearG = 0f, meanLinearB = 0f, meanLinearLuma = 0f,
    )
}
