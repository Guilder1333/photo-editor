package org.photoedit.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.photoedit.remote.ui.GalleryScreen
import org.photoedit.remote.ui.theme.RemotephotoeditingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemotephotoeditingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GalleryScreen()
                }
            }
        }
    }
}
