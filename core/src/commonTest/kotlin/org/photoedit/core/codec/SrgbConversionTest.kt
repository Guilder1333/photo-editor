package org.photoedit.core.codec

import kotlin.test.Test
import kotlin.test.assertEquals

class SrgbConversionTest {

    private fun assertNear(expected: Float, actual: Float, eps: Float = 0.001f) =
        assertEquals(expected, actual, eps)

    // --- srgbToLinear ---

    @Test
    fun `srgbToLinear 0 maps to 0`() = assertNear(0f, 0f.srgbToLinear())

    @Test
    fun `srgbToLinear 1 maps to 1`() = assertNear(1f, 1f.srgbToLinear())

    @Test
    fun `srgbToLinear midpoint is darker than 0_5`() {
        // sRGB is gamma-encoded, so linear 0.5 is lighter than sRGB 0.5
        val linear = 0.5f.srgbToLinear()
        assert(linear < 0.5f) { "expected linear < 0.5, got $linear" }
    }

    @Test
    fun `srgbToLinear known value 0_5`() = assertNear(0.2140f, 0.5f.srgbToLinear())

    @Test
    fun `srgbToLinear low value uses linear segment`() {
        // Values <= 0.04045 use the linear segment: linear = srgb / 12.92
        assertNear(0.04045f / 12.92f, 0.04045f.srgbToLinear())
    }

    // --- linearToSrgb ---

    @Test
    fun `linearToSrgb 0 maps to 0`() = assertNear(0f, 0f.linearToSrgb())

    @Test
    fun `linearToSrgb 1 maps to 1`() = assertNear(1f, 1f.linearToSrgb())

    @Test
    fun `linearToSrgb known value 0_2140`() = assertNear(0.5f, 0.2140f.linearToSrgb(), eps = 0.002f)

    @Test
    fun `linearToSrgb low value uses linear segment`() {
        // Values <= 0.0031308 use the linear segment: srgb = linear * 12.92
        assertNear(0.001f * 12.92f, 0.001f.linearToSrgb())
    }

    // --- round-trip ---

    @Test
    fun `round-trip sRGB to linear and back`() {
        listOf(0f, 0.1f, 0.25f, 0.5f, 0.75f, 1f).forEach { srgb ->
            assertNear(srgb, srgb.srgbToLinear().linearToSrgb(), eps = 0.001f)
        }
    }

    @Test
    fun `round-trip linear to sRGB and back`() {
        listOf(0f, 0.05f, 0.2f, 0.5f, 0.8f, 1f).forEach { linear ->
            assertNear(linear, linear.linearToSrgb().srgbToLinear(), eps = 0.001f)
        }
    }
}
