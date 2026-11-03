package com.musicu.player

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

data class Track(val id: Long, val title: String, val artist: String, val uri: Uri)

class MainActivity : ComponentActivity() {
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MusicUApp() }
    }

    private fun loadTracks(): List<Track> {
        val result = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )
        contentResolver.query(
            collection, projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Unknown title"
                val artist = cursor.getString(artistCol) ?: "Unknown artist"
                result += Track(id, title, artist, ContentUris.withAppendedId(collection, id))
            }
        }
        return result
    }

    @Composable
    private fun MusicUApp() {
        val bg = Color(0xFFF7F5FA)
        val primary = Color(0xFF6750A4)
        var tracks by remember { mutableStateOf(emptyList<Track>()) }
        var current by remember { mutableStateOf<Track?>(null) }
        var playing by remember { mutableStateOf(false) }
        var permissionGranted by remember {
            mutableStateOf(hasAudioPermission())
        }

        fun refresh() { tracks = loadTracks() }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permissionGranted = granted
            if (granted) refresh()
        }

        LaunchedEffect(Unit) {
            if (permissionGranted) refresh()
        }

        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = primary,
                onPrimary = Color.White,
                secondary = Color(0xFF625B71),
                background = bg,
                surface = Color(0xFFFFFBFE)
            )
        ) {
            Scaffold(
                containerColor = bg,
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFFECE6F0)) {
                        NavigationBarItem(
                            selected = true, onClick = {}, icon = { Icon(Icons.Default.LibraryMusic, null) },
                            label = { Text("Library") }
                        )
                        NavigationBarItem(
                            selected = false, onClick = {}, icon = { Icon(Icons.Default.FavoriteBorder, null) },
                            label = { Text("Favourites") }
                        )
                    }
                }
            ) { padding ->
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(22.dp))
                    Text("MusicU", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF292532))
                    Text("Your music, your colours.", color = Color(0xFF625B71), fontSize = 14.sp)
                    Spacer(Modifier.height(22.dp))
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFFE8DEF8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("NOW PLAYING", color = Color(0xFF494057), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(current?.title ?: "Choose a song", fontSize = 21.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF211B2B))
                            Text(current?.artist ?: "Your local music library", color = Color(0xFF625B71))
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()) {
                                IconButton(onClick = { /* Previous track can be added here */ }) {
                                    Icon(Icons.Default.SkipPrevious, "Previous", tint = primary)
                                }
                                FilledIconButton(
                                    onClick = {
                                        val track = current
                                        if (track != null) {
                                            if (playing) {
                                                player?.pause()
                                                playing = false
                                            } else {
                                                try { player?.start(); playing = true } catch (_: Exception) { }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(58.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = primary)
                                ) {
                                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause",
                                        modifier = Modifier.size(30.dp))
                                }
                                IconButton(onClick = { /* Next track can be added here */ }) {
                                    Icon(Icons.Default.SkipNext, "Next", tint = primary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Your library", fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${tracks.size} songs", color = Color(0xFF625B71), fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (!permissionGranted) {
                        Button(onClick = {
                            val permission = if (Build.VERSION.SDK_INT >= 33)
                                Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
                            permissionLauncher.launch(permission)
                        }) { Text("Allow music access") }
                    } else if (tracks.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MusicOff, null, tint = Color(0xFF797380), modifier = Modifier.size(42.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("No music found", fontWeight = FontWeight.SemiBold)
                                Text("Add audio files to your device and refresh.", color = Color(0xFF797380), fontSize = 13.sp)
                                TextButton(onClick = { refresh() }) { Text("Refresh library") }
                            }
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(tracks) { _, track ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            try {
                                                player?.release()
                                                player = MediaPlayer().apply {
                                                    setDataSource(this@MainActivity, track.uri)
                                                    setOnCompletionListener { playing = false }
                                                    prepare()
                                                    start()
                                                }
                                                current = track
                                                playing = true
                                            } catch (_: Exception) { playing = false }
                                        }.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE8DEF8)),
                                        contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.MusicNote, null, tint = primary)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(track.artist, color = Color(0xFF797380), fontSize = 12.sp, maxLines = 1)
                                    }
                                    Icon(Icons.Default.PlayCircle, null, tint = primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
