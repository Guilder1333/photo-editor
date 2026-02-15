package org.photoedit.remote.ui

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

private data class EditMenuItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

private val editMenuItems = listOf(
    EditMenuItem(Icons.Default.Tune,    "Adjustments",      {}),
    EditMenuItem(Icons.Default.Palette, "Color Correction", {}),
)

@Composable
fun EditMenuPanel(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    val tint = MaterialTheme.colorScheme.onSurfaceVariant

    if (isLandscape) {
        // Right sidebar — mirrors MenuPanel left sidebar
        Column(
            modifier = modifier
                .fillMaxHeight()
                .animateContentSize(tween(200))
                .background(background)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit tools", tint = tint)
            }

            Spacer(Modifier.height(4.dp))
            editMenuItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (expanded) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = tint
                        )
                    }
                    IconButton(onClick = item.onClick, modifier = Modifier.size(40.dp)) {
                        Icon(item.icon, contentDescription = item.label, tint = tint,
                            modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    } else {
        // Bottom bar — mirrors MenuPanel top bar, expands upward
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(tween(200))
                .background(background),
            horizontalAlignment = Alignment.Start
        ) {
            if (expanded) {
                editMenuItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(item.icon, contentDescription = item.label, tint = tint,
                            modifier = Modifier.size(24.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = tint
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Always-visible icon row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                editMenuItems.forEach { item ->
                    IconButton(onClick = item.onClick) {
                        Icon(item.icon, contentDescription = item.label, tint = tint)
                    }
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit tools", tint = tint)
                }
            }
        }
    }
}
