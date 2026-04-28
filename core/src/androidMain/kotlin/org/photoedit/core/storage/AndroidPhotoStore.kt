package org.photoedit.core.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

/**
 * [PhotoStore] for Android.
 *
 * Image slots (ORIGINAL, PREVIEW, THUMBNAIL) are stored as files under [rootDir].
 * The EDITS slot is stored in a SQLite database so edits are queryable and don't
 * occupy the file namespace.
 *
 * Directory layout under [rootDir]:
 * ```
 * rootDir/
 *   <sanitised-photoId>/
 *     original.jpg
 *     preview.jpg
 *     thumbnail.jpg
 * photo_edits.db          ← managed by SQLiteOpenHelper (not inside rootDir)
 * ```
 *
 * @param context Android [Context] used to locate the database.
 * @param rootDir Directory for image files. Created automatically if absent.
 */
class AndroidPhotoStore(
    context: Context,
    private val rootDir: File,
) : HybridPhotoStore() {

    private val db: SQLiteDatabase

    init {
        rootDir.mkdirs()
        db = EditsDbHelper(context).writableDatabase
    }

    // ── Database ──────────────────────────────────────────────────────────────

    override fun writeJson(photoId: String, json: String) {
        val values = ContentValues().apply {
            put(COL_ID, photoId)
            put(COL_JSON, json)
        }
        db.insertWithOnConflict(TABLE, null, values, CONFLICT_REPLACE)
    }

    override fun readJson(photoId: String): String? =
        db.query(TABLE, arrayOf(COL_JSON), "$COL_ID = ?", arrayOf(photoId), null, null, null)
            .use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    override fun deleteJson(photoId: String) {
        db.delete(TABLE, "$COL_ID = ?", arrayOf(photoId))
    }

    override fun listJsonIds(): List<String> =
        db.query(TABLE, arrayOf(COL_ID), null, null, null, null, null)
            .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    // ── File system ───────────────────────────────────────────────────────────

    override fun writeImageFile(photoId: String, slot: DataSlot, data: ByteArray) {
        photoDir(photoId).also { it.mkdirs() }.resolve(slot.filename).writeBytes(data)
    }

    override fun readImageFile(photoId: String, slot: DataSlot): ByteArray? {
        val file = photoDir(photoId).resolve(slot.filename)
        return if (file.exists()) file.readBytes() else null
    }

    override fun deleteImageFiles(photoId: String) {
        photoDir(photoId).deleteRecursively()
    }

    private fun photoDir(photoId: String) = rootDir.resolve(sanitize(photoId))

    private fun sanitize(id: String) = id.replace(Regex("[/\\\\:*?\"<>|]"), "_")

    // ── Schema ────────────────────────────────────────────────────────────────

    private class EditsDbHelper(context: Context) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE $TABLE ($COL_ID TEXT PRIMARY KEY, $COL_JSON TEXT NOT NULL)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE")
            onCreate(db)
        }
    }

    private companion object {
        const val DB_NAME    = "photo_edits.db"
        const val DB_VERSION = 1
        const val TABLE      = "edits"
        const val COL_ID     = "photo_id"
        const val COL_JSON   = "json"
    }
}
