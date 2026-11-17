package com.example.tryon.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tryon.data.Avatar
import com.example.tryon.data.Garment
import com.example.tryon.fit.Adjust
import com.example.tryon.fit.Fitter
import com.example.tryon.fit.Layer
import com.example.tryon.fit.SceneRenderer
import com.example.tryon.util.ImageIo

fun loadAsset(context: Context, path: String): Bitmap? =
    runCatching {
        context.assets.open(path).use { BitmapFactory.decodeStream(it) }
    }.getOrNull()

/* ----------------------------- Avatar picker ----------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerScreen(avatars: List<Avatar>, onPick: (Avatar) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Choose a model") })
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gridItems(avatars) { avatar ->
                val bmp = remember(avatar.id) { loadAsset(context, avatar.file) }
                Card(onClick = { onPick(avatar) }) {
                    Column(
                        Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = avatar.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(bmp.width.toFloat() / bmp.height)
                            )
                        } else {
                            Text("Missing image: ${avatar.file}")
                        }
                        Text(avatar.name, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

/* ------------------------------- Try-on screen ------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryOnScreen(avatar: Avatar, garments: List<Garment>, onBack: () -> Unit) {
    val context = LocalContext.current
    val avatarBmp = remember(avatar.id) { loadAsset(context, avatar.file) }
    val garmentBmps = remember { garments.associate { it.id to loadAsset(context, it.file) } }

    val layers = remember(avatar.id) { mutableStateListOf<Layer>() }
    val adjusts = remember(avatar.id) { mutableStateMapOf<String, Adjust>() }
    var activeId by remember(avatar.id) { mutableStateOf<String?>(null) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    fun onGarmentTap(g: Garment) {
        val bmp = avatarBmp ?: return
        val existing = layers.firstOrNull { it.garment.id == g.id }
        if (existing != null) {                                   // tap again = remove
            layers.remove(existing)
            adjusts.remove(g.id)
            if (activeId == g.id) activeId = layers.lastOrNull()?.garment?.id
            return
        }
        val gb = garmentBmps[g.id]
        if (gb == null) { toast("Missing image: ${g.file}"); return }
        val layer = Fitter.createLayer(avatar, bmp, g, gb)
        if (layer == null) { toast("This garment can't be fitted to this model"); return }
        layers.filter { it.garment.category == g.category }.forEach {   // one per category
            layers.remove(it); adjusts.remove(it.garment.id)
        }
        layers.add(layer)
        adjusts[g.id] = Adjust()
        activeId = g.id
    }

    fun currentPairs() = layers.map { it to (adjusts[it.garment.id] ?: Adjust()) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(avatar.name) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (avatarBmp == null) {
            Text("Missing image: ${avatar.file}", Modifier.padding(16.dp))
            return@Column
        }

        // Preview. Drag / pinch / twist moves the selected garment.
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF2F2F2)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                Modifier
                    .aspectRatio(avatarBmp.width.toFloat() / avatarBmp.height)
                    .pointerInput(avatar.id) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            val id = activeId ?: return@detectTransformGestures
                            val vs = size.width.toFloat() / avatarBmp.width
                            val a = adjusts[id] ?: Adjust()
                            adjusts[id] = a.copy(
                                dx = a.dx + pan.x / vs,
                                dy = a.dy + pan.y / vs,
                                scale = (a.scale * zoom).coerceIn(0.3f, 3f),
                                rotation = a.rotation + rotation
                            )
                        }
                    }
            ) {
                val vs = size.width / avatarBmp.width
                drawIntoCanvas { c ->
                    val nc = c.nativeCanvas
                    nc.save()
                    nc.scale(vs, vs)
                    SceneRenderer.draw(nc, avatarBmp, currentPairs())
                    nc.restore()
                }
            }
        }

        Text(
            if (activeId == null) "Pick a garment below"
            else "Drag, pinch or twist to fine-tune the selected item",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        // Garment catalog
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(112.dp)
        ) {
            items(garments) { g ->
                val gb = garmentBmps[g.id]
                val selected = layers.any { it.garment.id == g.id }
                Card(
                    onClick = { onGarmentTap(g) },
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.size(96.dp)
                ) {
                    Column(
                        Modifier.padding(6.dp).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (gb != null) {
                            Image(
                                bitmap = gb.asImageBitmap(),
                                contentDescription = g.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                            )
                        }
                        Text(g.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }

        // Actions
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { activeId?.let { adjusts[it] = Adjust() } },
                enabled = activeId != null,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, null); Text(" Reset")
            }
            Button(
                onClick = {
                    val out = SceneRenderer.export(avatarBmp, currentPairs())
                    toast(if (ImageIo.saveToGallery(context, out) != null) "Saved to Pictures/TryOn" else "Save failed")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Save, null); Text(" Save")
            }
            Button(
                onClick = {
                    val out = SceneRenderer.export(avatarBmp, currentPairs())
                    val uri = ImageIo.saveToGallery(context, out)
                    if (uri != null) ImageIo.share(context, uri) else toast("Share failed")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, null); Text(" Share")
            }
        }
    }
}
