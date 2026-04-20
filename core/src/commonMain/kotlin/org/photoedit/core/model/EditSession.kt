package org.photoedit.core.model

import org.photoedit.core.Pipeline

/**
 * An active editing session for a single photo.
 *
 * [EditSession] is immutable — use cases return updated copies rather than
 * mutating state. This aligns with [Pipeline]'s own immutable design and makes
 * undo/redo trivial.
 *
 * @param photoId Stable reference back to the [Photo] being edited.
 * @param pipeline The current (live) edit pipeline.
 * @param history Undo/redo stack of [Pipeline] snapshots.
 */
data class EditSession(
    val photoId: String,
    val pipeline: Pipeline,
    val history: EditHistory = EditHistory(),
)
