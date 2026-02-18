package org.photoedit.remote.ui.tools.adjustments

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import org.photoedit.remote.ui.tools.EditTool

object AdjustmentsTool : EditTool {
    override val icon: ImageVector = Icons.Default.Tune
    override val label: String = "Adjustments"
    override val hint: String = "Adjust brightness, contrast, and more"

    @Composable
    override fun Content() {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "Adjustments panel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
