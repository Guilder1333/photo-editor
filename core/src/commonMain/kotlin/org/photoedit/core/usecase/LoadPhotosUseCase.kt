package org.photoedit.core.usecase

import org.photoedit.core.model.Photo
import org.photoedit.core.repository.PhotoRepository

/**
 * Returns all photos from the repository in insertion order.
 */
class LoadPhotosUseCase(private val repository: PhotoRepository) {
    operator fun invoke(): List<Photo> = repository.getAll()
}
