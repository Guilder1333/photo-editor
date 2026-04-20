package org.photoedit.core.model

import org.photoedit.core.Pipeline

/**
 * Immutable undo/redo history for an edit session.
 *
 * Each entry in [undoStack] is a [Pipeline] snapshot taken *before* a change was
 * applied. [redoStack] holds snapshots that were undone and can be stepped forward
 * again. Both stacks are cleared or extended as a value — no mutation.
 *
 * Applying any new adjustment clears [redoStack] (standard linear-history model).
 */
data class EditHistory(
    val undoStack: List<Pipeline> = emptyList(),
    val redoStack: List<Pipeline> = emptyList(),
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * Records [current] as an undoable snapshot and clears the redo stack.
     * Call this *before* applying a change to the pipeline.
     */
    fun push(current: Pipeline): EditHistory =
        copy(undoStack = undoStack + current, redoStack = emptyList())

    /**
     * Steps back one change. Returns the previous [Pipeline] and an updated
     * [EditHistory] that has [current] on the redo stack.
     *
     * @throws IllegalStateException if [canUndo] is false.
     */
    fun undo(current: Pipeline): Pair<Pipeline, EditHistory> {
        check(canUndo) { "Nothing to undo" }
        val previous = undoStack.last()
        return previous to copy(
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + current,
        )
    }

    /**
     * Steps forward one change. Returns the next [Pipeline] and an updated
     * [EditHistory] that has [current] back on the undo stack.
     *
     * @throws IllegalStateException if [canRedo] is false.
     */
    fun redo(current: Pipeline): Pair<Pipeline, EditHistory> {
        check(canRedo) { "Nothing to redo" }
        val next = redoStack.last()
        return next to copy(
            undoStack = undoStack + current,
            redoStack = redoStack.dropLast(1),
        )
    }
}
