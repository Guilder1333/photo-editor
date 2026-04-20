package org.photoedit.core.usecase

import org.photoedit.core.Adjustment
import org.photoedit.core.model.EditSession
import org.photoedit.core.repository.EditSessionRepository

/**
 * Applies an [Adjustment] to the session's pipeline.
 *
 * The current pipeline is pushed onto the undo stack *before* the new adjustment
 * is applied, so the change is immediately undoable. Applying any adjustment
 * clears the redo stack (standard linear-history model).
 *
 * Returns the updated [EditSession] and persists it.
 */
class ApplyAdjustmentUseCase(private val sessionRepository: EditSessionRepository) {
    operator fun invoke(session: EditSession, adjustment: Adjustment): EditSession {
        val updated = session.copy(
            pipeline = session.pipeline.withAdjustment(adjustment),
            history = session.history.push(session.pipeline),
        )
        sessionRepository.save(updated)
        return updated
    }
}
