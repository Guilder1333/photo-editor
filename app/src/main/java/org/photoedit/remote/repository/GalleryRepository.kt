package org.photoedit.remote.repository

import android.content.Context
import android.content.SharedPreferences
import org.photoedit.remote.model.GalleryImage

class
GalleryRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadImages(): List<GalleryImage> {
        val stored = prefs.getString(KEY_URIS, "") ?: ""
        if (stored.isBlank()) return emptyList()
        return stored.split(SEPARATOR).mapNotNull { entry ->
            val sep = entry.indexOf('|')
            if (sep > 0) GalleryImage(
                id = entry.substring(0, sep),
                uri = entry.substring(sep + 1)
            ) else null
        }
    }

    fun addImage(image: GalleryImage) {
        val entry = "${image.id}|${image.uri}"
        val current = prefs.getString(KEY_URIS, "") ?: ""
        val updated = if (current.isBlank()) entry else "$entry$SEPARATOR$current"
        prefs.edit().putString(KEY_URIS, updated).apply()
    }

    fun removeImage(id: String) {
        val current = prefs.getString(KEY_URIS, "") ?: ""
        val updated = current.split(SEPARATOR)
            .filter { !it.startsWith("$id|") }
            .joinToString(SEPARATOR)
        prefs.edit().putString(KEY_URIS, updated).apply()
    }

    companion object {
        private const val PREFS_NAME = "gallery_prefs"
        private const val KEY_URIS = "gallery_uris"
        private const val SEPARATOR = "\n"
    }
}
