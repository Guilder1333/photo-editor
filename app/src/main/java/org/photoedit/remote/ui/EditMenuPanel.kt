package org.photoedit.remote.ui

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import org.photoedit.remote.ui.tools.EditTool
import org.photoedit.remote.ui.tools.adjustments.AdjustmentsTool
import org.photoedit.remote.ui.tools.colorcorrection.ColorCorrectionTool

/** Default tools shown when no explicit list is provided to [EditMenuPanel]. */
val defaultEditTools: List<EditTool> = listOf(AdjustmentsTool, ColorCorrectionTool)

/**
 * Tool menu for the image editor.
 *
 * Always shows a strip of tool icons. Tapping an icon selects that tool and
 * expands its [EditTool.Content] composable in a sub-panel. Tapping the active
 * icon again collapses the sub-panel.
 *
 * Landscape: right sidebar — icon column on the right, sub-panel grows to the left.
 * Portrait:  bottom bar   — icon row at the bottom, sub-panel grows upward.
 *
 * @param tools List of [EditTool] implementations to display. Defaults to [defaultEditTools].
 */
@Composable
fun EditMenuPanel(
    modifier: Modifier = Modifier,
    tools: List<EditTool> = defaultEditTools,
    temperature: Float = 0f,
    onTemperatureChange: (Float) -> Unit = {},
    tint: Float = 0f,
    onTintChange: (Float) -> Unit = {},
    vibrance: Float = 0f,
    onVibranceChange: (Float) -> Unit = {},
    saturation: Float = 0f,
    onSaturationChange: (Float) -> Unit = {}
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val selectedTool = selectedIndex?.let { tools.getOrNull(it) }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeTint = MaterialTheme.colorScheme.primary

    if (isLandscape) {
        // Right sidebar: [sub-panel | icon column]
        Row(
            modifier = modifier
                .fillMaxHeight()
                .animateContentSize(tween(200))
                .background(background)
        ) {
            // Sub-panel — left of the icon column, shown when a tool is selected
            if (selectedTool != null) {
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .padding(12.dp)
                ) {
                    Text(
                        text = selectedTool.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = iconColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                    ) {
                        if (selectedTool == ColorCorrectionTool) {
                            ColorCorrectionTool.Content(
                                temperature = temperature,
                                onTemperatureChange = onTemperatureChange,
                                tint = tint,
                                onTintChange = onTintChange,
                                vibrance = vibrance,
                                onVibranceChange = onVibranceChange,
                                saturation = saturation,
                                onSaturationChange = onSaturationChange
                            )
                        } else {
                            selectedTool.Content()
                        }
                    }
                }
            }

            // Icon column — always visible
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                tools.forEachIndexed { index, tool ->
                    IconButton(
                        onClick = { selectedIndex = if (selectedIndex == index) null else index },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.hint,
                            tint = if (selectedIndex == index) activeTint else iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Bottom bar: [sub-panel above] / [icon row]
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(tween(200))
                .background(background)
        ) {
            // Sub-panel — above the icon row, shown when a tool is selected
            if (selectedTool != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = selectedTool.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = iconColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                    ) {
                        if (selectedTool == ColorCorrectionTool) {
                            ColorCorrectionTool.Content(
                                temperature = temperature,
                                onTemperatureChange = onTemperatureChange,
                                tint = tint,
                                onTintChange = onTintChange,
                                vibrance = vibrance,
                                onVibranceChange = onVibranceChange,
                                saturation = saturation,
                                onSaturationChange = onSaturationChange
                            )
                        } else {
                            selectedTool.Content()
                        }
                    }
                }
            }

            // Icon row — always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                tools.forEachIndexed { index, tool ->
                    IconButton(
                        onClick = { selectedIndex = if (selectedIndex == index) null else index }
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.hint,
                            tint = if (selectedIndex == index) activeTint else iconColor
                        )
                    }
                }
            }
        }
    }
}
