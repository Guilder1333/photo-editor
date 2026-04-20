package org.photoedit.core.repository

import org.photoedit.core.model.Photo

/**
 * Platform-agnostic contract for photo CRUD.
 *
 * All operations are synchronous at this layer. Platform-specific adapters
 * (Android Room, desktop file-based, iOS CoreData) implement this interface
 * and may expose suspend variants in their own layer. Coroutines are not a
 * dependency of `commonMain` yet.
 */
interface PhotoRepository {
    /** Returns all photos in insertion order. */
    fun getAll(): List<Photo>

    /** Returns the photo with [id], or `null` if it does not exist. */
    fun getById(id: String): Photo?

    /** Inserts or replaces the photo record. Keyed on [Photo.id]. */
    fun add(photo: Photo)

    /** Removes the photo with [id]. No-op if it does not exist. */
    fun remove(id: String)
}
