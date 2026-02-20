package org.photoedit.remote.ui.tools.colorcorrection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.photoedit.remote.ui.tools.EditTool

object ColorCorrectionTool : EditTool {
    override val icon: ImageVector = Icons.Default.Palette
    override val label: String = "Color Correction"
    override val hint: String = "Correct and grade image colors"

    @Composable
    override fun Content() {
        // Default implementation for tools that don't use parameters
    }

    @Composable
    fun Content(
        temperature: Float,
        onTemperatureChange: (Float) -> Unit,
        tint: Float,
        onTintChange: (Float) -> Unit
    ) {
        var vibrance by remember { mutableFloatStateOf(0f) }
        var saturation by remember { mutableFloatStateOf(0f) }

        Column(modifier = Modifier.fillMaxWidth()) {
            ColorSlider(
                label = "Temperature",
                value = temperature,
                onValueChange = onTemperatureChange
            )
            ColorSlider(
                label = "Tint",
                value = tint,
                onValueChange = onTintChange
            )
            ColorSlider(
                label = "Vibrance",
                value = vibrance,
                onValueChange = { vibrance = it }
            )
            ColorSlider(
                label = "Saturation",
                value = saturation,
                onValueChange = { saturation = it }
            )
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -100f..100f,
            modifier = Modifier.fillMaxWidth().height(20.dp)
        )
    }
}
