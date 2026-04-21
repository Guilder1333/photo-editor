package org.photoedit.core.repository

import org.photoedit.core.model.Photo

/**
 * Volatile, in-process implementation of [PhotoRepository].
 *
 * Useful for unit tests and as a drop-in baseline before a persistent
 * platform-specific adapter is wired in. Not thread-safe.
 */
class InMemoryPhotoRepository : PhotoRepository {
    // LinkedHashMap preserves insertion order for getAll().
    private val store = LinkedHashMap<String, Photo>()

    override fun getAll(): List<Photo> = store.values.toList()

    override fun getById(id: String): Photo? = store[id]

    override fun add(photo: Photo) {
        store[photo.id] = photo
    }

    override fun remove(id: String) {
        store.remove(id)
    }
}
