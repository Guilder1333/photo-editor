package org.photoedit.core

/**
 * Bounds for a preview render. The pipeline downscales the source image to fit
 * within [maxWidth] × [maxHeight] before applying adjustments, so the full
 * adjustment stack runs on a smaller buffer — fast enough for slider drag feedback.
 *
 * @param maxWidth  Maximum preview width in pixels (must be ≥ 1).
 * @param maxHeight Maximum preview height in pixels (must be ≥ 1).
 */
data class PreviewSize(val maxWidth: Int, val maxHeight: Int) {
    init {
        require(maxWidth  >= 1) { "maxWidth must be at least 1" }
        require(maxHeight >= 1) { "maxHeight must be at least 1" }
    }
}
