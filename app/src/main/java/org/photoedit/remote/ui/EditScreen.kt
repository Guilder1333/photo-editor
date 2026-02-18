package org.photoedit.remote.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import android.media.ExifInterface
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

@Composable
fun EditScreen(
    imageUri: String,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val image: @Composable (Modifier) -> Unit = { mod ->
        AndroidView(
            factory = { context ->
                val uri = imageUri.toUri()

                // ORIENTATION_USE_EXIF only works for file paths, not content URIs.
                // Read the EXIF tag ourselves through the ContentResolver.
                val exifDegrees = try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                    } ?: ExifInterface.ORIENTATION_NORMAL
                } catch (_: Exception) {
                    ExifInterface.ORIENTATION_NORMAL
                }

                val ssivOrientation = when (exifDegrees) {
                    ExifInterface.ORIENTATION_ROTATE_90  -> SubsamplingScaleImageView.ORIENTATION_90
                    ExifInterface.ORIENTATION_ROTATE_180 -> SubsamplingScaleImageView.ORIENTATION_180
                    ExifInterface.ORIENTATION_ROTATE_270 -> SubsamplingScaleImageView.ORIENTATION_270
                    else                                 -> SubsamplingScaleImageView.ORIENTATION_0
                }

                SubsamplingScaleImageView(context).apply {
                    setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                    setMaxScale(10f)
                    orientation = ssivOrientation
                    setImage(ImageSource.uri(uri))
                }
            },
            modifier = mod
        )
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                image(Modifier.fillMaxSize())
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
            EditMenuPanel()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                image(Modifier.fillMaxSize())
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
            EditMenuPanel()
        }
    }
}
