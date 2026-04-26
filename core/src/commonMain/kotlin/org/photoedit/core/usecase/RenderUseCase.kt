package org.photoedit.core.usecase

import kotlinx.coroutines.flow.Flow
import org.photoedit.core.PreviewSize
import org.photoedit.core.RenderProgress
import org.photoedit.core.model.EditSession

/**
 * Renders the current pipeline state of an [EditSession].
 *
 * Returns a [Flow] that emits [RenderProgress.InProgress] after each adjustment
 * step and [RenderProgress.Complete] with the final image when done.
 *
 * Pass a [PreviewSize] to get a fast downscaled render suitable for slider drag
 * feedback. Omit it (or pass `null`) for a full-resolution render.
 *
 * Apply `flowOn(Dispatchers.Default)` at the call site to run pixel work off the
 * main thread.
 */
class RenderUseCase {
    operator fun invoke(
        session: EditSession,
        previewSize: PreviewSize? = null,
    ): Flow<RenderProgress> = if (previewSize != null)
        session.pipeline.renderPreviewAsync(previewSize.maxWidth, previewSize.maxHeight)
    else
        session.pipeline.renderAsync()
}
