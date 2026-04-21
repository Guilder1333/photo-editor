package org.photoedit.core

/**
 * Immutable edit pipeline. Every mutating operation returns a new [Pipeline];
 * this makes undo/redo trivial (keep a stack of [Pipeline] snapshots).
 *
 * Rendering always starts from [source] (or the last [Checkpoint]) and re-applies
 * all active adjustments in their canonical [Order]. No intermediate caching —
 * caching can be added later without changing the interface.
 */
class Pipeline(
    private val source: ImageBuffer,
    private val adjustments: List<Adjustment> = emptyList(),
    private val checkpoints: List<Checkpoint> = emptyList(),
) {
    /**
     * Renders the current edit state to a new [ImageBuffer].
     * Skips adjustments whose [Adjustment.isIdentity] returns true.
     */
    fun render(): ImageBuffer {
        val base = checkpoints.lastOrNull()?.image ?: source
        val active = adjustments
            .sortedBy { it.order }
            .filterNot { it.isIdentity() }
        return active.fold(base) { img, adj -> adj.apply(img) }
    }

    /**
     * Returns a new [Pipeline] with [a] added or replacing any existing adjustment
     * with the same [AdjustmentId]. Preserves canonical order at render time.
     */
    fun withAdjustment(a: Adjustment): Pipeline =
        copy(adjustments = adjustments.filter { it.id != a.id } + a)

    /**
     * Bakes the current output into a new base image (a checkpoint), then resets
     * the active adjustment list so subsequent adjustments stack on top.
     *
     * AI operations always produce checkpoints because they are expensive and
     * non-commutative with the existing adjustment stack.
     */
    fun addCheckpoint(image: ImageBuffer, producedBy: String): Pipeline =
        copy(
            checkpoints = checkpoints + Checkpoint(image, producedBy, adjustments),
            adjustments = emptyList(),
        )

    private fun copy(
        adjustments: List<Adjustment> = this.adjustments,
        checkpoints: List<Checkpoint> = this.checkpoints,
    ) = Pipeline(source, adjustments, checkpoints)
}

/**
 * A baked intermediate image produced by an expensive or non-commutative operation
 * (e.g. an AI inpainting pass). Adjustments after a checkpoint stack on its output
 * rather than on the original source.
 */
data class Checkpoint(
    val image: ImageBuffer,
    val producedBy: String,
    val bakedAdjustments: List<Adjustment>,
)
