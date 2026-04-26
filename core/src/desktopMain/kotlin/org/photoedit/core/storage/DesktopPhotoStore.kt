package org.photoedit.core.storage

import java.io.File

/**
 * [PhotoStore] implementation that stores artefacts as files on the local filesystem.
 *
 * Layout under [rootDir]:
 * ```
 * rootDir/
 *   <photoId>/
 *     original.jpg
 *     edits.json
 *     preview.jpg
 *     thumbnail.jpg
 * ```
 *
 * @param rootDir Directory under which all photo sub-directories are created.
 *   Created automatically if it does not exist.
 */
class DesktopPhotoStore(private val rootDir: File) : PhotoStore {

    init { rootDir.mkdirs() }

    override fun write(photoId: String, slot: DataSlot, data: ByteArray) {
        photoDir(photoId).also { it.mkdirs() }
            .resolve(slot.filename)
            .writeBytes(data)
    }

    override fun read(photoId: String, slot: DataSlot): ByteArray? {
        val file = photoDir(photoId).resolve(slot.filename)
        return if (file.exists()) file.readBytes() else null
    }

    override fun delete(photoId: String) {
        photoDir(photoId).deleteRecursively()
    }

    override fun listPhotoIds(): List<String> =
        rootDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map    { it.name }
            ?.sorted()
            ?: emptyList()

    private fun photoDir(photoId: String): File = rootDir.resolve(sanitize(photoId))

    /** Strips characters that are unsafe as directory names on all major OSes. */
    private fun sanitize(photoId: String): String =
        photoId.replace(Regex("[/\\\\:*?\"<>|]"), "_")
}
