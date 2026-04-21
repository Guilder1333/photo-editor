package org.photoedit.core.usecase

import org.photoedit.core.ImageBuffer
import org.photoedit.core.Pipeline
import org.photoedit.core.model.EditSession
import org.photoedit.core.repository.EditSessionRepository
import org.photoedit.core.repository.PhotoRepository

/**
 * Opens (or resumes) an edit session for a photo.
 *
 * If a session already exists for [photoId] it is returned as-is, preserving
 * any in-progress adjustments and undo history. Otherwise a fresh session is
 * created, saved, and returned.
 *
 * [source] must be supplied by the platform adapter that decoded the image into
 * an [ImageBuffer]. This keeps image I/O out of `commonMain`.
 *
 * @throws IllegalArgumentException if [photoId] does not exist in [photoRepository].
 */
class StartEditSessionUseCase(
    private val photoRepository: PhotoRepository,
    private val sessionRepository: EditSessionRepository,
) {
    operator fun invoke(photoId: String, source: ImageBuffer): EditSession {
        requireNotNull(photoRepository.getById(photoId)) {
            "Photo '$photoId' not found in repository"
        }
        val existing = sessionRepository.load(photoId)
        if (existing != null) return existing

        val session = EditSession(
            photoId = photoId,
            pipeline = Pipeline(source),
        )
        sessionRepository.save(session)
        return session
    }
}
