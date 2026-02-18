package org.photoedit.remote.ui.tools.colorcorrection

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import org.photoedit.remote.ui.tools.EditTool

object ColorCorrectionTool : EditTool {
    override val icon: ImageVector = Icons.Default.Palette
    override val label: String = "Color Correction"
    override val hint: String = "Correct and grade image colors"

    @Composable
    override fun Content() {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "Color Correction panel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
