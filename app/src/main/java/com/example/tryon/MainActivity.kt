package com.example.tryon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.tryon.data.Avatar
import com.example.tryon.data.CatalogRepository
import com.example.tryon.ui.AvatarPickerScreen
import com.example.tryon.ui.TryOnScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().systemBarsPadding()) { App() }
                }
            }
        }
    }
}

@Composable
private fun App() {
    val context = LocalContext.current
    val catalog = remember {
        runCatching { CatalogRepository.load(context) }.getOrNull()
    }
    if (catalog == null) {
        Text("Could not read assets/catalog.json - check the file format.")
        return
    }
    var selected by remember { mutableStateOf<Avatar?>(null) }

    val avatar = selected
    if (avatar == null) {
        AvatarPickerScreen(catalog.avatars) { selected = it }
    } else {
        BackHandler { selected = null }
        TryOnScreen(avatar, catalog.garments) { selected = null }
    }
}
