package org.photoedit.core

import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * Asserts that [actual] is within [epsilon] of [expected].
 *
 * Default epsilon of 0.001 is well above float32 rounding noise for the simple
 * arithmetic used in adjustment implementations (~1e-7), so failures here indicate
 * real formula errors.
 */
fun assertNear(
    expected: Float,
    actual: Float,
    epsilon: Float = 0.001f,
    message: String? = null,
) {
    val diff = abs(actual - expected)
    assertTrue(
        diff <= epsilon,
        buildString {
            if (message != null) append("$message — ")
            append("expected $expected ±$epsilon but was $actual (diff=$diff)")
        },
    )
}
