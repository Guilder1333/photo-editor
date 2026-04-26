package org.photoedit.core.storage

/**
 * The four data artefacts persisted per photo.
 *
 * [filename] is the leaf name used by file-based [PhotoStore] implementations;
 * other backends may use it as a logical key suffix.
 */
enum class DataSlot(val filename: String) {
    /** Original source image (JPEG). Loaded as the pipeline base when a session is restored. */
    ORIGINAL("original.jpg"),

    /** JSON list of active [org.photoedit.core.Adjustment]s. Reconstructs the edit state. */
    EDITS("edits.json"),

    /**
     * Downscaled render of the current edit state (JPEG), sized for display (e.g. 1920 × 1080).
     * Loaded directly for preview without re-rendering.
     */
    PREVIEW("preview.jpg"),

    /**
     * Tiny render for grid/filmstrip UI (JPEG), typically 256 × 256.
     * Loaded to populate thumbnails without touching the full pipeline.
     */
    THUMBNAIL("thumbnail.jpg"),
}
