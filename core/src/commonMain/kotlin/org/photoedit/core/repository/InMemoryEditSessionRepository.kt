package org.photoedit.core.repository

import org.photoedit.core.model.EditSession

/**
 * Volatile, in-process implementation of [EditSessionRepository].
 *
 * Useful for unit tests and as a drop-in baseline before a persistent
 * platform-specific adapter is wired in. Not thread-safe.
 */
class InMemoryEditSessionRepository : EditSessionRepository {
    private val store = mutableMapOf<String, EditSession>()

    override fun save(session: EditSession) {
        store[session.photoId] = session
    }

    override fun load(photoId: String): EditSession? = store[photoId]

    override fun delete(photoId: String) {
        store.remove(photoId)
    }
}
