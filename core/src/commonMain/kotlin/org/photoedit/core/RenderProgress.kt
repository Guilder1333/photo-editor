package org.photoedit.core

/**
 * Emitted by [Pipeline.renderAsync] to report rendering progress.
 *
 * Collectors receive one [InProgress] event after each adjustment completes,
 * followed by a single [Complete] event carrying the final image.
 */
sealed class RenderProgress {
    /**
     * Emitted after each adjustment step.
     *
     * @param percent Completion percentage in [0, 100].
     * @param step    1-based index of the adjustment just finished.
     * @param total   Total number of active (non-identity) adjustments.
     */
    data class InProgress(val percent: Int, val step: Int, val total: Int) : RenderProgress()

    /** Emitted once when all adjustments have been applied. */
    data class Complete(val image: ImageBuffer) : RenderProgress()
}
