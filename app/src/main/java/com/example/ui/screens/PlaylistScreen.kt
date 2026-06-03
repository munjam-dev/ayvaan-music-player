package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import com.example.data.db.PlayHistoryEntity
import com.example.data.db.PlaylistEntity
import com.example.data.model.Song
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    viewModel: MusicViewModel,
    onSongSelected: (Song) -> Unit
) {
    val songs by viewModel.songsWithFavoritesState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()
    val playlistsWithSongs by viewModel.playlistsWithSongs.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var selectedPlaylistForDetail by remember { mutableStateOf<PlaylistEntity?>(null) }

    val detailState = selectedPlaylistForDetail
    if (detailState != null) {
        PlaylistDetailScreen(
            playlist = detailState,
            viewModel = viewModel,
            onSongSelected = onSongSelected,
            onBack = { selectedPlaylistForDetail = null }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp) // Avoid bottom floating nav clipping
        ) {
        // Glowing Header Block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CyberViolet.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column {
                Spacer(modifier = Modifier.height(28.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_ayvan_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Playlists",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = TextLight,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Create New Button
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("create_playlist_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(Brush.linearGradient(colors = listOf(CyberViolet, CyberPink)))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = TextLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Custom collections section
        Text(
            text = "My Custom Playlists",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextLight,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        if (playlists.isEmpty()) {
            com.example.ui.components.AyvanEmptyState(
                title = "No Playlists",
                message = "Create a playlist using 'New' button above."
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                playlists.forEach { playlist ->
                    val playlistSongs = playlistsWithSongs[playlist.id] ?: emptyList()
                    Card(
                        onClick = { selectedPlaylistForDetail = playlist },
                        colors = CardDefaults.cardColors(containerColor = CardGrey.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_card_${playlist.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.radialGradient(colors = listOf(CyberViolet, CyberCyan)))
                                ) {
                                    Icon(
                                        Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = TextLight,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = playlist.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (playlistSongs.isEmpty()) "0 tracks" else if (playlistSongs.size == 1) "1 track" else "${playlistSongs.size} tracks",
                                        fontSize = 11.sp,
                                        color = TextDim
                                    )
                                }
                            }

                            // Delete Playlist icon
                            IconButton(
                                onClick = { viewModel.deletePlaylist(playlist.id) },
                                modifier = Modifier.testTag("delete_playlist_${playlist.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Playlist",
                                    tint = CyberPink.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Listening History Section
        Text(
            text = "Recently Played Tracks",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextLight,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        if (recentHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Play tracks to compile playback history.", color = TextDim, fontSize = 13.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Map play history to actual song objects or display historical items elegantly
                recentHistory.take(10).forEach { history ->
                    UniqueHistoryRow(history = history, onClick = {
                        // Find matching song or play mock
                        val match = songs.find { it.filePath == history.songPath }
                        if (match != null) {
                            onSongSelected(match)
                        } else {
                            // Virtual playback fallback
                            val dummy = Song(
                                id = "history_dummy",
                                title = history.title,
                                artist = history.artist,
                                album = history.album,
                                duration = history.duration,
                                filePath = history.songPath,
                                folder = "History Archive"
                            )
                            onSongSelected(dummy)
                        }
                    })
                }
            }
        }
    }
}

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = CardGrey,
            title = {
                Text(
                    text = "New Offline Collection",
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = playlistNameInput,
                    onValueChange = { playlistNameInput = it },
                    placeholder = { Text("e.g. Cyber Glitch Beats", color = TextDim) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextLight,
                        focusedBorderColor = CyberPink,
                        unfocusedTextColor = TextDim,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistNameInput.isNotBlank()) {
                            viewModel.createPlaylist(playlistNameInput)
                            playlistNameInput = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("CREATE", color = TextLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("CANCEL", color = TextDim)
                }
            }
        )
    }
}

@Composable
fun PlaylistCollageArtwork(songsInPlaylist: List<Song>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E1E2A))
    ) {
        if (songsInPlaylist.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = CyberCyan.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
            }
        } else if (songsInPlaylist.size < 4) {
            AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(songsInPlaylist[0].artworkUri)
                .size(160)
                .crossfade(true)
                .build(),
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color(0xFF222232)),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(songsInPlaylist[0].artworkUri).size(400).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    AsyncImage(
                        model = songsInPlaylist[1].artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Row(modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = songsInPlaylist[2].artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    AsyncImage(
                        model = songsInPlaylist[3].artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity,
    viewModel: MusicViewModel,
    onSongSelected: (Song) -> Unit,
    onBack: () -> Unit
) {
    val playlistsWithSongs by viewModel.playlistsWithSongs.collectAsState()
    val songsInPlaylist = playlistsWithSongs[playlist.id] ?: emptyList()
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(playlist.name) }
    
    // Calculate total duration
    val totalSecs = songsInPlaylist.sumOf { (it.duration / 1000).toInt() }
    val durationFormatted = remember(totalSecs) {
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        if (hrs > 0) {
            "$hrs hr ${mins} min"
        } else if (mins > 0) {
            "$mins min ${secs} sec"
        } else {
            "$secs sec"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .statusBarsPadding()
    ) {
        // Custom Top Header Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("playlist_detail_back")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextLight
                )
            }
            
            Text(
                text = "Playlist Details",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight
            )

            // More Options for Playlist rename / clear / delete
            var showOptions by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showOptions = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu Options", tint = TextLight)
                }
                DropdownMenu(
                    expanded = showOptions,
                    onDismissRequest = { showOptions = false },
                    modifier = Modifier.background(CardGrey)
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename Playlist", color = TextLight) },
                        onClick = {
                            showOptions = false
                            renameInput = playlist.name
                            showRenameDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = CyberCyan) }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear All Songs", color = TextLight) },
                        onClick = {
                            showOptions = false
                            viewModel.clearPlaylist(playlist.id)
                        },
                        leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null, tint = CyberPink) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Playlist", color = TextLight) },
                        onClick = {
                            showOptions = false
                            viewModel.deletePlaylist(playlist.id)
                            onBack()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 120.dp) // Avoid overlap with bottom nav / mini player
        ) {
            // Header Hero Box of Playlist
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlaylistCollageArtwork(
                        songsInPlaylist = songsInPlaylist,
                        modifier = Modifier
                            .size(180.dp)
                            .testTag("playlist_detail_artwork")
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = playlist.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextLight,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${songsInPlaylist.size} tracks  •  $durationFormatted",
                        fontSize = 13.sp,
                        color = TextDim,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Buttons Block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Play Button
                        Button(
                            onClick = {
                                if (songsInPlaylist.isNotEmpty()) {
                                    viewModel.playPlaylist(songsInPlaylist, songsInPlaylist.first())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("play_playlist_btn"),
                            enabled = songsInPlaylist.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextLight)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PLAY ALL", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 13.sp)
                        }

                        // Shuffle Button
                        Button(
                            onClick = {
                                if (songsInPlaylist.isNotEmpty()) {
                                    viewModel.shufflePlaylist(songsInPlaylist)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("shuffle_playlist_btn"),
                            enabled = songsInPlaylist.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SHUFFLE", fontWeight = FontWeight.Bold, color = TextLight, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Playlist songs title header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Tracks List", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextLight)
                    Text(text = "Reorder / Edit", fontSize = 11.sp, color = TextDim)
                }
            }

            if (songsInPlaylist.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextDim.copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("This playlist is empty.", color = TextDim, fontSize = 13.sp)
                            Text("Add songs from Library or Home tab!", color = TextDim.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            } else {
                itemsIndexed(songsInPlaylist, key = { index, song -> "playlist_${song.id}_$index" }) { index, song ->
                    Box(modifier = Modifier.testTag("playlist_song_item_${index}")) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playPlaylist(songsInPlaylist, song) }
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Number indicator
                            Text(
                                text = "${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDim,
                                modifier = Modifier.width(24.dp)
                            )

                            // Album artwork
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x19ffffff))
                            ) {
                                val context = LocalContext.current
                                val artRequest = remember(song.artworkUri) {
                                    coil.request.ImageRequest.Builder(context)
                                        .data(song.artworkUri)
                                        .size(120, 120)
                                        .crossfade(true)
                                        .build()
                                }
                                AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(artRequest)
                .size(160)
                .crossfade(true)
                .build(),
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color(0xFF222232)),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Titles
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = song.artist,
                                    fontSize = 11.sp,
                                    color = TextDim,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Reordering controls
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        viewModel.reorderPlaylistSongs(playlist.id, songsInPlaylist, index, index - 1)
                                    }
                                },
                                modifier = Modifier.size(24.dp),
                                enabled = index > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move Up",
                                    tint = if (index > 0) CyberCyan else TextDim.copy(alpha = 0.2f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (index < songsInPlaylist.size - 1) {
                                        viewModel.reorderPlaylistSongs(playlist.id, songsInPlaylist, index, index + 1)
                                    }
                                },
                                modifier = Modifier.size(24.dp),
                                enabled = index < songsInPlaylist.size - 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move Down",
                                    tint = if (index < songsInPlaylist.size - 1) CyberCyan else TextDim.copy(alpha = 0.2f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Deletion option of this song in current playlist
                            IconButton(
                                onClick = { viewModel.removeSongFromPlaylist(playlist.id, song) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Song",
                                    tint = CyberPink.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = CardGrey,
            title = { Text("Rename Playlist", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextLight,
                        focusedBorderColor = CyberPink,
                        unfocusedTextColor = TextDim,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renamePlaylist(playlist.id, renameInput)
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                ) {
                    Text("RENAME", color = TextLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("CANCEL", color = TextDim)
                }
            }
        )
    }
}

@Composable
fun UniqueHistoryRow(history: PlayHistoryEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x19ffffff)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = CyberPink.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = history.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = history.artist,
                fontSize = 11.sp,
                color = TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "Played",
            fontSize = 11.sp,
            color = CyberCyan,
            fontWeight = FontWeight.Medium
        )
    }
}
