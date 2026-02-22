package org.photoedit.remote.model

data class ImageAdjustments(
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val vibrance: Float = 0f,
    val saturation: Float = 0f
)
