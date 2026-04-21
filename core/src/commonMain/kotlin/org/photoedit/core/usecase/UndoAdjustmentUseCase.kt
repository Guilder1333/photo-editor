package org.photoedit.core.usecase

import org.photoedit.core.model.EditSession
import org.photoedit.core.repository.EditSessionRepository

/**
 * Steps back one change in the edit history.
 *
 * Returns the session unchanged if there is nothing to undo. Otherwise returns
 * the updated session (previous pipeline restored, current pipeline pushed to
 * the redo stack) and persists it.
 */
class UndoAdjustmentUseCase(private val sessionRepository: EditSessionRepository) {
    operator fun invoke(session: EditSession): EditSession {
        if (!session.history.canUndo) return session

        val (previousPipeline, newHistory) = session.history.undo(session.pipeline)
        val updated = session.copy(pipeline = previousPipeline, history = newHistory)
        sessionRepository.save(updated)
        return updated
    }
}
