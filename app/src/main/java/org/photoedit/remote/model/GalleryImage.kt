package org.photoedit.remote.model

data class GalleryImage(
    val id: String,
    val uri: String,
    val adjustments: ImageAdjustments = ImageAdjustments()
)
