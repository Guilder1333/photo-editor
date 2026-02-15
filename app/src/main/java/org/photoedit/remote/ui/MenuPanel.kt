package org.photoedit.remote.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.dp

private data class MenuItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

private val menuItems = listOf(
    MenuItem(Icons.Default.Tune,     "Filter") {},
    MenuItem(Icons.Default.Settings, "Preferences") {},
    MenuItem(Icons.Default.Info,     "About") {},
    MenuItem(Icons.AutoMirrored.Filled.Help,     "Help") {},
)

@Composable
fun MenuPanel(
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val background = MaterialTheme.colorScheme.surfaceVariant
    val tint = MaterialTheme.colorScheme.onSurfaceVariant

    if (isLandscape) {
        // Left panel — always shows icons; expands to add labels alongside
        Column(
            modifier = modifier
                .fillMaxHeight()
                .animateContentSize(tween(200))
                .background(background)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = tint)
            }

            Spacer(Modifier.height(4.dp))
            menuItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = tint,
                        modifier = Modifier.size(24.dp))
                    if (expanded) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = tint
                        )
                    }
                }
            }
        }
    } else {
        // Top panel — always shows icons; expands to show labels below each icon
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(tween(200))
                .background(background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = tint)
                }
                menuItems.forEach { item ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = item.onClick) {
                            Icon(item.icon, contentDescription = item.label, tint = tint)
                        }
                        if (expanded) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = tint,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
