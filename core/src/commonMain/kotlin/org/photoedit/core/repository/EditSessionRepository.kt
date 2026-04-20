package org.photoedit.core.repository

import org.photoedit.core.model.EditSession

/**
 * Platform-agnostic contract for persisting and restoring edit sessions.
 *
 * Sessions are keyed on [EditSession.photoId]. Storing the full [Pipeline]
 * (including its [ImageBuffer] source) is intentionally deferred — in the
 * in-memory implementation the object is kept as-is; a persistent
 * implementation will serialise the adjustment parameters only, and reload
 * the source image from the [PhotoRepository] at restore time.
 */
interface EditSessionRepository {
    /** Persists (or replaces) the session. Keyed on [EditSession.photoId]. */
    fun save(session: EditSession)

    /** Returns the session for [photoId], or `null` if none is stored. */
    fun load(photoId: String): EditSession?

    /** Removes the session for [photoId]. No-op if it does not exist. */
    fun delete(photoId: String)
}
