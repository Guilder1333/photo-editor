package org.photoedit.core.storage

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.sqlite3.*

/**
 * [PhotoStore] for iOS.
 *
 * Image slots (ORIGINAL, PREVIEW, THUMBNAIL) are stored as files under [rootDir].
 * The EDITS slot is stored in a SQLite database ([rootDir]/photo_edits.db) so edits
 * are queryable and separate from the image files.
 *
 * Directory layout:
 * ```
 * rootDir/
 *   photo_edits.db
 *   <sanitised-photoId>/
 *     original.jpg
 *     preview.jpg
 *     thumbnail.jpg
 * ```
 *
 * Typical usage: pass the app's Documents directory as [rootDir].
 * ```kotlin
 * val docs = NSSearchPathForDirectoriesInDomains(
 *     NSDocumentDirectory, NSUserDomainMask, true
 * ).first() as String
 * val store = IosPhotoStore("$docs/PhotoEditor")
 * ```
 */
class IosPhotoStore(private val rootDir: String) : HybridPhotoStore() {

    private val fileManager = NSFileManager.defaultManager
    private val db: CPointer<sqlite3>

    init {
        fileManager.createDirectoryAtPath(
            rootDir, withIntermediateDirectories = true, attributes = null, error = null,
        )
        db = memScoped {
            val ptr = allocPointerTo<sqlite3>()
            val rc = sqlite3_open("$rootDir/$DB_NAME", ptr.ptr)
            check(rc == SQLITE_OK) { "Failed to open SQLite database (rc=$rc)" }
            ptr.value!!
        }
        sqlite3_exec(
            db,
            "CREATE TABLE IF NOT EXISTS $TABLE ($COL_ID TEXT PRIMARY KEY, $COL_JSON TEXT NOT NULL)",
            null, null, null,
        )
    }

    // ── Database ──────────────────────────────────────────────────────────────

    override fun writeJson(photoId: String, json: String) {
        exec("INSERT OR REPLACE INTO $TABLE ($COL_ID, $COL_JSON) VALUES (?, ?)") { stmt ->
            sqlite3_bind_text(stmt, 1, photoId, -1, SQLITE_TRANSIENT)
            sqlite3_bind_text(stmt, 2, json,    -1, SQLITE_TRANSIENT)
            sqlite3_step(stmt)
        }
    }

    override fun readJson(photoId: String): String? {
        var result: String? = null
        exec("SELECT $COL_JSON FROM $TABLE WHERE $COL_ID = ?") { stmt ->
            sqlite3_bind_text(stmt, 1, photoId, -1, SQLITE_TRANSIENT)
            if (sqlite3_step(stmt) == SQLITE_ROW) {
                result = sqlite3_column_text(stmt, 0)?.toKString()
            }
        }
        return result
    }

    override fun deleteJson(photoId: String) {
        exec("DELETE FROM $TABLE WHERE $COL_ID = ?") { stmt ->
            sqlite3_bind_text(stmt, 1, photoId, -1, SQLITE_TRANSIENT)
            sqlite3_step(stmt)
        }
    }

    override fun listJsonIds(): List<String> {
        val ids = mutableListOf<String>()
        exec("SELECT $COL_ID FROM $TABLE") { stmt ->
            while (sqlite3_step(stmt) == SQLITE_ROW) {
                sqlite3_column_text(stmt, 0)?.toKString()?.let { ids.add(it) }
            }
        }
        return ids
    }

    // ── File system ───────────────────────────────────────────────────────────

    override fun writeImageFile(photoId: String, slot: DataSlot, data: ByteArray) {
        val dir = photoDir(photoId)
        fileManager.createDirectoryAtPath(
            dir, withIntermediateDirectories = true, attributes = null, error = null,
        )
        data.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = data.size.toULong())
                .writeToFile("$dir/${slot.filename}", atomically = true)
        }
    }

    override fun readImageFile(photoId: String, slot: DataSlot): ByteArray? {
        val path = "${photoDir(photoId)}/${slot.filename}"
        val nsData = NSData.dataWithContentsOfFile(path) ?: return null
        return ByteArray(nsData.length.toInt()).also { bytes ->
            bytes.usePinned { pinned ->
                nsData.getBytes(pinned.addressOf(0), length = nsData.length)
            }
        }
    }

    override fun deleteImageFiles(photoId: String) {
        fileManager.removeItemAtPath(photoDir(photoId), error = null)
    }

    private fun photoDir(photoId: String) = "$rootDir/${sanitize(photoId)}"

    private fun sanitize(id: String) = id.replace(Regex("[/\\\\:*?\"<>|]"), "_")

    // ── SQLite helpers ────────────────────────────────────────────────────────

    private inline fun exec(sql: String, block: (CPointer<sqlite3_stmt>) -> Unit) {
        memScoped {
            val stmtPtr = allocPointerTo<sqlite3_stmt>()
            if (sqlite3_prepare_v2(db, sql, -1, stmtPtr.ptr, null) != SQLITE_OK) return
            val stmt = stmtPtr.value ?: return
            try { block(stmt) } finally { sqlite3_finalize(stmt) }
        }
    }

    private companion object {
        const val DB_NAME = "photo_edits.db"
        const val TABLE   = "edits"
        const val COL_ID  = "photo_id"
        const val COL_JSON = "json"

        // SQLITE_TRANSIENT (-1 cast to function pointer): tells SQLite to copy the string.
        val SQLITE_TRANSIENT = (-1L).toCPointer<CFunction<(COpaquePointer?) -> Unit>>()
    }
}
