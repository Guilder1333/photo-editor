package org.photoedit.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import org.photoedit.remote.ui.EditScreen
import org.photoedit.remote.ui.GalleryScreen
import org.photoedit.remote.ui.theme.RemotePhotoEditorTheme
import org.photoedit.remote.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemotePhotoEditorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: GalleryViewModel = viewModel()
                    val currentEditImage by viewModel.currentEditImage.collectAsState()

                    if (currentEditImage != null) {
                        EditScreen(
                            imageUri = currentEditImage!!.uri,
                            onClose = { viewModel.closeImage() }
                        )
                    } else {
                        GalleryScreen(
                            viewModel = viewModel,
                            onOpenImage = { id -> viewModel.openImage(id) }
                        )
                    }
                }
            }
        }
    }
}
