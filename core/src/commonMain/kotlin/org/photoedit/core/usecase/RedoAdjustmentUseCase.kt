package org.photoedit.core.usecase

import org.photoedit.core.model.EditSession
import org.photoedit.core.repository.EditSessionRepository

/**
 * Steps forward one change in the edit history (re-applies an undone change).
 *
 * Returns the session unchanged if there is nothing to redo. Otherwise returns
 * the updated session (next pipeline restored, current pipeline pushed back onto
 * the undo stack) and persists it.
 */
class RedoAdjustmentUseCase(private val sessionRepository: EditSessionRepository) {
    operator fun invoke(session: EditSession): EditSession {
        if (!session.history.canRedo) return session

        val (nextPipeline, newHistory) = session.history.redo(session.pipeline)
        val updated = session.copy(pipeline = nextPipeline, history = newHistory)
        sessionRepository.save(updated)
        return updated
    }
}
