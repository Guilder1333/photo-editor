package org.photoedit.core.storage

/**
 * Base [PhotoStore] that routes image slots (ORIGINAL, PREVIEW, THUMBNAIL) to the
 * platform file system and the EDITS slot to a local SQLite database.
 *
 * Subclasses supply seven platform-specific operations; all routing and the public
 * [PhotoStore] contract are handled here so there is no duplication between the
 * Android and iOS implementations.
 */
abstract class HybridPhotoStore : PhotoStore {

    final override fun write(photoId: String, slot: DataSlot, data: ByteArray) = when (slot) {
        DataSlot.EDITS -> writeJson(photoId, data.decodeToString())
        else           -> writeImageFile(photoId, slot, data)
    }

    final override fun read(photoId: String, slot: DataSlot): ByteArray? = when (slot) {
        DataSlot.EDITS -> readJson(photoId)?.encodeToByteArray()
        else           -> readImageFile(photoId, slot)
    }

    final override fun delete(photoId: String) {
        deleteJson(photoId)
        deleteImageFiles(photoId)
    }

    // The database is the authoritative source of photo IDs: every full save writes an
    // edits row, so any photo with at least one complete save appears here.
    final override fun listPhotoIds(): List<String> = listJsonIds().sorted()

    // ── Database (EDITS slot) ─────────────────────────────────────────────────

    protected abstract fun writeJson(photoId: String, json: String)
    protected abstract fun readJson(photoId: String): String?
    protected abstract fun deleteJson(photoId: String)
    protected abstract fun listJsonIds(): List<String>

    // ── File system (image slots) ─────────────────────────────────────────────

    protected abstract fun writeImageFile(photoId: String, slot: DataSlot, data: ByteArray)
    protected abstract fun readImageFile(photoId: String, slot: DataSlot): ByteArray?
    protected abstract fun deleteImageFiles(photoId: String)
}
