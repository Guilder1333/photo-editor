package org.photoedit.core

import org.photoedit.core.adjustments.*

/**
 * Central registry of every known [AdjustmentType].
 *
 * Each entry is the companion object of an [Adjustment] subclass. The registry
 * serves as the single source of truth for "what adjustment types exist" and can
 * be used by any subsystem — serialization, UI, command palette, etc. — without
 * coupling those subsystems to each other.
 */
object AdjustmentRegistry {
    val all: List<AdjustmentType> = listOf(
        Exposure, Brightness, Contrast, Highlights, Shadows, Whites, Blacks,
        Temperature, Tint, Saturation, Vibrance, Sharpness, Clarity,
        NoiseReduction, Vignette, Crop, Curves, Hsl,
    )
}
