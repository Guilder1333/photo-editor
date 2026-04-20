package org.photoedit.core

/**
 * ITU-R BT.709 luminance coefficients for converting linear-light RGB to luma.
 * Used by saturation, vibrance, highlights, and shadows adjustments.
 */
internal const val LUM_R = 0.2126f
internal const val LUM_G = 0.7152f
internal const val LUM_B = 0.0722f
