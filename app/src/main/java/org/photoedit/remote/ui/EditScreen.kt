package org.photoedit.remote.ui

import android.content.res.Configuration
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import org.photoedit.remote.model.ImageAdjustments

@Composable
private fun ZoomableImage(
    imageUri: String,
    temperature: Float,
    tint: Float,
    vibrance: Float,
    saturation: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ssiv = remember(imageUri) {
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
            setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_CENTER)
            maxScale = 100f
            orientation = ssivOrientation
            setImage(ImageSource.uri(uri))
        }
    }

    AndroidView(
        factory = { ssiv },
        update = { view ->
            val colorMatrix = createTemperatureMatrix(temperature)
            colorMatrix.postConcat(createTintMatrix(tint))
            colorMatrix.postConcat(createVibranceMatrix(vibrance))
            colorMatrix.postConcat(createSaturationMatrix(saturation))
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }
            view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
        },
        modifier = modifier
    )
}

private fun createTemperatureMatrix(temperature: Float): ColorMatrix {
    // Temperature range: -100 (cool/blue) to +100 (warm/orange)
    // Normalize to -1.0 to 1.0
    val temp = temperature / 100f

    return ColorMatrix().apply {
        if (temp > 0) {
            // Warm: increase red, slightly increase green
            val warmth = temp * 0.3f
            val array = floatArrayOf(
                1f + warmth, 0f, 0f, 0f, 0f,
                0f, 1f + warmth * 0.5f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            set(array)
        } else {
            // Cool: increase blue, slightly reduce red
            val coolness = -temp * 0.3f
            val array = floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f + coolness, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            set(array)
        }
    }
}

private fun createVibranceMatrix(vibrance: Float): ColorMatrix {
    // Vibrance selectively boosts muted colors more than already-saturated ones.
    // Approximated with a saturation matrix using R/B-biased luma weights (vR=0.5, vG=0.0, vB=0.5)
    // so the effect is strongest on hues that don't contain much green (reds, blues, purples).
    val sat = 1f + vibrance / 100f * 0.5f
    val vR = 0.5f; val vG = 0.0f; val vB = 0.5f
    val array = floatArrayOf(
        vR * (1f - sat) + sat, vG * (1f - sat),         vB * (1f - sat),         0f, 0f,
        vR * (1f - sat),       vG * (1f - sat) + sat,   vB * (1f - sat),         0f, 0f,
        vR * (1f - sat),       vG * (1f - sat),         vB * (1f - sat) + sat,   0f, 0f,
        0f,                    0f,                      0f,                      1f, 0f
    )
    return ColorMatrix(array)
}

private fun createTintMatrix(tint: Float): ColorMatrix {
    // Tint range: -100 (green) to +100 (magenta)
    // Positive tint (magenta): boost R and B, suppress G
    // Negative tint (green): suppress R and B, boost G
    val t = tint / 100f * 0.3f
    val array = floatArrayOf(
        1f + t, 0f,     0f,     0f, 0f,
        0f,     1f - t, 0f,     0f, 0f,
        0f,     0f,     1f + t, 0f, 0f,
        0f,     0f,     0f,     1f, 0f
    )
    return ColorMatrix(array)
}

private fun createSaturationMatrix(saturation: Float): ColorMatrix {
    // Saturation range: -100 (grayscale) to +100 (double saturation), 0 = unchanged.
    // Maps to multiplier: -100 → 0.0, 0 → 1.0, 100 → 2.0
    val sat = 1f + saturation / 100f
    // Standard luma weights (BT.601)
    val rw = 0.213f; val gw = 0.715f; val bw = 0.072f
    val array = floatArrayOf(
        rw * (1f - sat) + sat, gw * (1f - sat),         bw * (1f - sat),         0f, 0f,
        rw * (1f - sat),       gw * (1f - sat) + sat,   bw * (1f - sat),         0f, 0f,
        rw * (1f - sat),       gw * (1f - sat),         bw * (1f - sat) + sat,   0f, 0f,
        0f,                    0f,                      0f,                      1f, 0f
    )
    return ColorMatrix(array)
}

@Composable
fun EditScreen(
    imageUri: String,
    initialAdjustments: ImageAdjustments = ImageAdjustments(),
    onSave: (ImageAdjustments) -> Unit = {},
    onClose: () -> Unit
) {
    var temperature by remember { mutableFloatStateOf(initialAdjustments.temperature) }
    var tint        by remember { mutableFloatStateOf(initialAdjustments.tint) }
    var vibrance    by remember { mutableFloatStateOf(initialAdjustments.vibrance) }
    var saturation  by remember { mutableFloatStateOf(initialAdjustments.saturation) }

    val handleClose = {
        onSave(ImageAdjustments(temperature, tint, vibrance, saturation))
        onClose()
    }

    BackHandler(onBack = handleClose)

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                ZoomableImage(
                    imageUri = imageUri,
                    temperature = temperature,
                    tint = tint,
                    vibrance = vibrance,
                    saturation = saturation,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = handleClose,
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
            EditMenuPanel(
                temperature = temperature,
                onTemperatureChange = { temperature = it },
                tint = tint,
                onTintChange = { tint = it },
                vibrance = vibrance,
                onVibranceChange = { vibrance = it },
                saturation = saturation,
                onSaturationChange = { saturation = it }
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                ZoomableImage(
                    imageUri = imageUri,
                    temperature = temperature,
                    tint = tint,
                    vibrance = vibrance,
                    saturation = saturation,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = handleClose,
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
            EditMenuPanel(
                temperature = temperature,
                onTemperatureChange = { temperature = it },
                tint = tint,
                onTintChange = { tint = it },
                vibrance = vibrance,
                onVibranceChange = { vibrance = it },
                saturation = saturation,
                onSaturationChange = { saturation = it }
            )
        }
    }
}
