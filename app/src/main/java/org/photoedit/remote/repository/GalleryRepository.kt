package org.photoedit.remote.repository

import android.content.Context
import android.content.SharedPreferences
import org.photoedit.remote.model.GalleryImage
import org.photoedit.remote.model.ImageAdjustments
import androidx.core.content.edit

class GalleryRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadImages(): List<GalleryImage> {
        val stored = prefs.getString(KEY_URIS, "") ?: ""
        if (stored.isBlank()) return emptyList()
        return stored.split(SEPARATOR).mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 2) {
                val adjustments = if (parts.size >= 6) {
                    ImageAdjustments(
                        temperature = parts[2].toFloatOrNull() ?: 0f,
                        tint        = parts[3].toFloatOrNull() ?: 0f,
                        vibrance    = parts[4].toFloatOrNull() ?: 0f,
                        saturation  = parts[5].toFloatOrNull() ?: 0f
                    )
                } else {
                    ImageAdjustments()
                }
                GalleryImage(id = parts[0], uri = parts[1], adjustments = adjustments)
            } else null
        }
    }

    fun addImage(image: GalleryImage) {
        val entry = serialize(image)
        val current = prefs.getString(KEY_URIS, "") ?: ""
        val updated = if (current.isBlank()) entry else "$entry$SEPARATOR$current"
        prefs.edit { putString(KEY_URIS, updated) }
    }

    fun removeImage(id: String) {
        val current = prefs.getString(KEY_URIS, "") ?: ""
        val updated = current.split(SEPARATOR)
            .filter { it.split("|").firstOrNull() != id }
            .joinToString(SEPARATOR)
        prefs.edit { putString(KEY_URIS, updated) }
    }

    fun updateAdjustments(id: String, adjustments: ImageAdjustments) {
        val current = prefs.getString(KEY_URIS, "") ?: ""
        val updated = current.split(SEPARATOR).joinToString(SEPARATOR) { entry ->
            val parts = entry.split("|")
            if (parts.size >= 2 && parts[0] == id) {
                serialize(GalleryImage(id = parts[0], uri = parts[1], adjustments = adjustments))
            } else {
                entry
            }
        }
        prefs.edit { putString(KEY_URIS, updated) }
    }

    private fun serialize(image: GalleryImage): String {
        val a = image.adjustments
        return "${image.id}|${image.uri}|${a.temperature}|${a.tint}|${a.vibrance}|${a.saturation}"
    }

    companion object {
        private const val PREFS_NAME = "gallery_prefs"
        private const val KEY_URIS = "gallery_uris"
        private const val SEPARATOR = "\n"
    }
}
