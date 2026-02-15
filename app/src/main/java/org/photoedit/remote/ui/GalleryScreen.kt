package org.photoedit.remote.ui

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.photoedit.remote.model.GalleryImage
import org.photoedit.remote.viewmodel.GalleryViewModel
import org.photoedit.remote.viewmodel.SelectionState

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onOpenImage: (String) -> Unit
) {
    val images by viewModel.images.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Back press exits selection mode
    BackHandler(enabled = selection.active) {
        viewModel.clearSelection()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.addImage(it) }
    }

    Scaffold(
        floatingActionButton = {
            if (!selection.active) {
                FloatingActionButton(onClick = { launcher.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add image")
                }
            }
        },
        bottomBar = {
            if (selection.active) {
                SelectionPanel(
                    selectedCount = selection.count,
                    onDelete = { viewModel.deleteSelected() }
                )
            }
        }
    ) { innerPadding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MenuPanel(isLandscape = true)
                GalleryGrid(
                    images = images,
                    selection = selection,
                    onLongClick = { viewModel.enterSelectionMode(it) },
                    onTap = { viewModel.toggleSelection(it) },
                    onOpen = onOpenImage,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MenuPanel(isLandscape = false)
                GalleryGrid(
                    images = images,
                    selection = selection,
                    onLongClick = { viewModel.enterSelectionMode(it) },
                    onTap = { viewModel.toggleSelection(it) },
                    onOpen = onOpenImage,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GalleryGrid(
    images: List<GalleryImage>,
    selection: SelectionState,
    onLongClick: (String) -> Unit,
    onTap: (String) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(images, key = { it.id }) { image ->
            GalleryThumbnail(
                image = image,
                isSelected = image.id in selection.selectedIds,
                isSelectionMode = selection.active,
                onLongClick = { onLongClick(image.id) },
                onTap = { onTap(image.id) },
                onOpen = { onOpen(image.id) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryThumbnail(
    image: GalleryImage,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onLongClick: () -> Unit,
    onTap: () -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val borderColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = { if (isSelectionMode) onTap() else onOpen() }
            )
            .then(
                if (isSelected) Modifier.border(3.dp, borderColor)
                else Modifier
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(image.uri))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
