package org.photoedit.core.storage

import org.photoedit.core.ImageBuffer
import org.photoedit.core.Pipeline
import org.photoedit.core.PreviewSize

/**
 * High-level service that persists and restores a photo's edit state.
 *
 * Four artefacts are written per photo (see [DataSlot]):
 * - **original**: the unedited source image, encoded as JPEG.
 * - **edits**: the active adjustment list serialized to JSON.
 * - **preview**: the current edit result downscaled to [previewSize], encoded as JPEG.
 * - **thumbnail**: the current edit result downscaled to [thumbnailSize], encoded as JPEG.
 *
 * The service is stateless and thread-safe; [PhotoStore] and [ImageCodec] must be
 * thread-safe if called from multiple coroutines.
 *
 * @param store         Byte-level storage backend (filesystem, DB, …).
 * @param codec         Platform image encoder/decoder.
 * @param previewSize   Maximum dimensions for the cached preview image.
 * @param thumbnailSize Maximum dimensions for the cached thumbnail image.
 */
class PhotoStorageService(
    private val store: PhotoStore,
    private val codec: ImageCodec,
    private val previewSize: PreviewSize   = PreviewSize(1920, 1080),
    private val thumbnailSize: PreviewSize = PreviewSize(256, 256),
) {

    /**
     * Persists the full edit state for [photoId]:
     * - original source image
     * - active adjustments as JSON
     * - downscaled preview and thumbnail renders of the current edit result
     *
     * Calling this again for the same [photoId] overwrites all four slots.
     */
    fun save(photoId: String, pipeline: Pipeline) {
        store.write(photoId, DataSlot.ORIGINAL,   codec.encodeJpeg(pipeline.source))
        store.write(photoId, DataSlot.EDITS,      AdjustmentSerializer.toJson(pipeline.adjustments).encodeToByteArray())
        store.write(photoId, DataSlot.PREVIEW,    codec.encodeJpeg(pipeline.renderPreview(previewSize.maxWidth, previewSize.maxHeight)))
        store.write(photoId, DataSlot.THUMBNAIL,  codec.encodeJpeg(pipeline.renderPreview(thumbnailSize.maxWidth, thumbnailSize.maxHeight)))
    }

    /**
     * Restores the [Pipeline] for [photoId] from the stored original image and edits.
     *
     * Returns `null` if no original has been saved for this ID.
     * Missing edits are treated as an empty adjustment list (identity pipeline).
     */
    fun loadPipeline(photoId: String): Pipeline? {
        val originalBytes = store.read(photoId, DataSlot.ORIGINAL) ?: return null
        val source = codec.decode(originalBytes)

        val editsBytes = store.read(photoId, DataSlot.EDITS)
        val adjustments = editsBytes
            ?.decodeToString()
            ?.let { AdjustmentSerializer.fromJson(it) }
            ?: emptyList()

        return adjustments.fold(Pipeline(source)) { p, adj -> p.withAdjustment(adj) }
    }

    /**
     * Returns the cached thumbnail for [photoId], or `null` if not stored.
     * Does not re-render; use [save] to update the cached thumbnail.
     */
    fun loadThumbnail(photoId: String): ImageBuffer? =
        store.read(photoId, DataSlot.THUMBNAIL)?.let { codec.decode(it) }

    /**
     * Returns the cached preview image for [photoId], or `null` if not stored.
     * Does not re-render; use [save] to update the cached preview.
     */
    fun loadPreview(photoId: String): ImageBuffer? =
        store.read(photoId, DataSlot.PREVIEW)?.let { codec.decode(it) }

    /**
     * Deletes all stored artefacts for [photoId].
     */
    fun delete(photoId: String) = store.delete(photoId)

    /**
     * Returns the IDs of all photos that have at least one stored artefact.
     */
    fun listPhotoIds(): List<String> = store.listPhotoIds()
}
