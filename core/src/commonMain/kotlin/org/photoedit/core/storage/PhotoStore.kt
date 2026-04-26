package org.photoedit.core.storage

/**
 * Platform-agnostic byte-level storage for per-photo artefacts.
 *
 * Each photo is identified by a stable string [photoId]. Under each ID, four
 * named [DataSlot]s can be written and read independently. Implementations
 * decide where and how bytes are stored (filesystem, database, cloud, etc.).
 *
 * All operations are synchronous; callers are responsible for dispatching to a
 * background thread / coroutine context when needed.
 */
interface PhotoStore {
    /**
     * Writes [data] under [photoId] / [slot], replacing any previously stored bytes.
     */
    fun write(photoId: String, slot: DataSlot, data: ByteArray)

    /**
     * Returns the bytes previously stored under [photoId] / [slot], or `null` if
     * nothing has been written for that combination.
     */
    fun read(photoId: String, slot: DataSlot): ByteArray?

    /**
     * Removes all stored artefacts for [photoId]. A no-op if the ID is unknown.
     */
    fun delete(photoId: String)

    /**
     * Returns the IDs of every photo that has at least one stored artefact.
     */
    fun listPhotoIds(): List<String>
}
