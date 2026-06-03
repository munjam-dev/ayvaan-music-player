package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onSongSelected: (Song) -> Unit
) {
    val songs by viewModel.filteredSongs.collectAsState()
    val rawSongs by viewModel.songsWithFavoritesState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favPaths by viewModel.favoritePaths.collectAsState()
    val enablePermanentDeletion by viewModel.enablePermanentDeletion.collectAsState()

    var activeMenuSong by remember { mutableStateOf<Song?>(null) }
    var activeSubTab by remember { mutableStateOf("All") }
    val libraryTabs = listOf("All", "Favourites", "Folders", "Artists", "Albums")

    // Filter list based on selected tab
    val filteredTabList = remember(songs, activeSubTab, favPaths) {
        when (activeSubTab) {
            "Favourites" -> songs.filter { it.isFavorite }
            else -> songs
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 120.dp) // Avoid overlap with bottom nav
    ) {
        // Glowing Background Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CyberPink.copy(alpha = 0.15f),
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
                            text = "Library",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = TextLight,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    // Add custom list indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33ffffff))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${rawSongs.size} Audio files",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fast Scroll Tabs (All, Favourites, folders, artists, albums)
                ScrollableTabRow(
                    edgePadding = 0.dp,
                    selectedTabIndex = libraryTabs.indexOf(activeSubTab).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    libraryTabs.forEach { tab ->
                        val isSelected = tab == activeSubTab
                        Tab(
                            selected = isSelected,
                            onClick = { activeSubTab = tab },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        brush = if (isSelected) {
                                            Brush.linearGradient(colors = listOf(CyberViolet, CyberPink))
                                        } else {
                                            Brush.linearGradient(colors = listOf(Color(0x1affffff), Color(0x0affffff)))
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextLight else TextDim
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Bar for library filtering
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search songs, artists, albums", color = TextDim, fontSize = 14.sp) },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = TextLight,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start
            ),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = CyberCyan, modifier = Modifier.size(20.dp)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                focusedBorderColor = CyberPink.copy(alpha = 0.8f),
                unfocusedTextColor = TextDim,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color(0x221B1B26),
                unfocusedContainerColor = Color(0x221B1B26)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .height(52.dp)
                .testTag("library_filter_bar")
        )

        // List Container
        if (filteredTabList.isEmpty() && activeSubTab == "Favourites") {
            com.example.ui.components.AyvanEmptyState(
                title = "No Favorites",
                message = "You haven't added any favorite tracks yet."
            )
        } else if (filteredTabList.isEmpty()) {
            com.example.ui.components.AyvanEmptyState(
                title = "No Matches Found",
                message = "No matching offline files scanned."
            )
        } else {
            // Display clean list of matching local items
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("library_track_list")
            ) {
                items(filteredTabList, key = { it.id }) { song ->
                    LibrarySongListItem(
                        song = song,
                        onClick = { onSongSelected(song) },
                        onOptionsClick = { activeMenuSong = song }
                    )
                }
            }
        }
    }

    activeMenuSong?.let { song ->
        SongOptionsMenuDialog(
            song = song,
            onDismiss = { activeMenuSong = null },
            viewModel = viewModel,
            enablePermanentDeletion = enablePermanentDeletion,
            onAddToPlaylist = {
                activeMenuSong = null
                viewModel.showAddToPlaylistDialog(song)
            }
        )
    }
}

@Composable
fun LibrarySongListItem(
    song: Song,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x191B1B26))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("library_song_item_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art/Poster on the left (56-64dp, rounded corners)
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x1affffff))
        ) {
            val context = LocalContext.current
            val artRequest = remember(song.artworkUri) {
                coil.request.ImageRequest.Builder(context)
                    .data(song.artworkUri)
                    .size(160, 160) // Downsample to 160x160px for premium performance
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = artRequest,
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF222232)),
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Details (Song Name and Artist Name in the center)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.artist,
                fontSize = 13.sp,
                color = TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Three Dot Menu on the right
        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.testTag("lib_more_${song.id}")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = TextLight,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
