package org.photoedit.remote.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Contract for a tool entry in the EditMenuPanel.
 *
 * Implement this interface to register a new editor tool:
 *  - [icon]    — icon shown in the always-visible icon strip
 *  - [label]   — human-readable name shown as the sub-panel title
 *  - [hint]    — accessibility / tooltip description (used for contentDescription)
 *  - [Content] — composable drawn inside the expanded sub-panel when this tool is selected
 */
interface EditTool {
    val icon: ImageVector
    val label: String
    val hint: String

    @Composable
    fun Content()
}
