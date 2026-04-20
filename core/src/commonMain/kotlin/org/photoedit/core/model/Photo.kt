package org.photoedit.core.model

/**
 * Immutable record for a photo in the gallery.
 *
 * [uri] is intentionally a plain [String] so this class stays platform-agnostic.
 * On Android it will hold a content URI; on desktop/iOS a file path.
 * Platform adapters are responsible for resolving the URI to actual bytes.
 */
data class Photo(
    val id: String,
    val uri: String,
    val width: Int,
    val height: Int,
    /** Creation or last-modified time in milliseconds since the Unix epoch. */
    val timestampMs: Long,
)
