package org.photoedit.core.codec

import kotlin.math.pow

/**
 * IEC 61966-2-1 sRGB transfer functions for converting between sRGB-encoded
 * 8-bit values (normalised to [0,1]) and linear-light values.
 *
 * Platform codecs call these at load/save time so the core pipeline always
 * operates in linear light.
 */

/** Converts a normalised sRGB-encoded value to linear light. */
fun Float.srgbToLinear(): Float =
    if (this <= 0.04045f) this / 12.92f
    else ((this + 0.055f) / 1.055f).pow(2.4f)

/** Converts a linear-light value to a normalised sRGB-encoded value. */
fun Float.linearToSrgb(): Float =
    if (this <= 0.0031308f) this * 12.92f
    else 1.055f * this.pow(1f / 2.4f) - 0.055f
