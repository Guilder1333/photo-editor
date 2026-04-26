package org.photoedit.core

/**
 * Stable identity for an adjustment type. Used by [Pipeline.withAdjustment] to
 * replace an existing adjustment of the same kind rather than stacking duplicates.
 */
@JvmInline
value class AdjustmentId(val value: String)

/**
 * Canonical pipeline slot constants. Adjustments are always applied in ascending
 * [order] regardless of the order the user tweaked them in.
 *
 * Gaps between slots intentionally leave room to insert new adjustments without
 * renumbering existing ones (changing order constants after users have saved edits
 * would shift their results).
 */
object Order {
    const val CROP           =  50
    const val EXPOSURE       = 100
    const val BRIGHTNESS     = 150
    const val CONTRAST       = 200
    const val HIGHLIGHTS     = 300
    const val SHADOWS        = 310
    const val WHITES         = 320
    const val BLACKS         = 330
    const val CURVES         = 400
    const val TEMPERATURE    = 450
    const val TINT           = 460
    const val HSL            = 500
    const val SATURATION     = 510
    const val VIBRANCE       = 520
    const val CLARITY        = 600
    const val NOISE_REDUCTION = 700
    const val VIGNETTE       = 800
    const val SHARPEN        = 900
    // Gaps leave room to insert new adjustments without renumbering.
}

/**
 * Contract for a single, composable image adjustment.
 *
 * Implementations MUST:
 * - Be pure functions of `(input, parameters)` — same inputs → identical output.
 * - Never mutate [ImageBuffer.pixels] of the input; always return a new buffer.
 * - Return `true` from [isIdentity] when parameters are at their neutral value so
 *   the pipeline can skip the call entirely.
 * - Use [order] to declare their fixed slot in the canonical pipeline.
 */
interface Adjustment {
    val id: AdjustmentId
    val order: Int
    fun isIdentity(): Boolean
    fun apply(input: ImageBuffer): ImageBuffer
}
