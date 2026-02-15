package org.photoedit.remote.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.photoedit.remote.model.GalleryImage
import org.photoedit.remote.repository.GalleryRepository
import java.util.UUID

data class SelectionState(
    val active: Boolean = false,
    val selectedIds: Set<String> = emptySet()
) {
    val count: Int get() = selectedIds.size
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GalleryRepository(application)

    private val _images = MutableStateFlow<List<GalleryImage>>(emptyList())
    val images: StateFlow<List<GalleryImage>> = _images.asStateFlow()

    private val _selection = MutableStateFlow(SelectionState())
    val selection: StateFlow<SelectionState> = _selection.asStateFlow()

    private val _currentEditImage = MutableStateFlow<GalleryImage?>(null)
    val currentEditImage: StateFlow<GalleryImage?> = _currentEditImage.asStateFlow()

    init {
        _images.value = repository.loadImages()
    }

    fun addImage(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Not all URIs support persistable permissions; store the reference anyway
        }

        val image = GalleryImage(id = UUID.randomUUID().toString(), uri = uri.toString())
        repository.addImage(image)
        _images.value = repository.loadImages()
    }

    /** Long tap on an image: enter selection mode with that image selected. */
    fun enterSelectionMode(id: String) {
        _selection.value = SelectionState(active = true, selectedIds = setOf(id))
    }

    /** Single tap in selection mode: toggle the image; exit mode when nothing is selected. */
    fun toggleSelection(id: String) {
        val current = _selection.value
        if (!current.active) return
        val newIds = if (id in current.selectedIds) current.selectedIds - id
                     else current.selectedIds + id
        _selection.value = if (newIds.isEmpty()) SelectionState()
                           else current.copy(selectedIds = newIds)
    }

    fun deleteSelected() {
        _selection.value.selectedIds.forEach { repository.removeImage(it) }
        _images.value = repository.loadImages()
        _selection.value = SelectionState()
    }

    fun clearSelection() {
        _selection.value = SelectionState()
    }

    fun openImage(id: String) {
        _currentEditImage.value = _images.value.find { it.id == id }
    }

    fun closeImage() {
        _currentEditImage.value = null
    }

    fun removeImage(id: String) {
        repository.removeImage(id)
        _images.value = repository.loadImages()
    }
}
