package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*;
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onSongSelected: (Song) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val allSongs by viewModel.songsWithFavoritesState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val profilePicUri by viewModel.profilePicUri.collectAsState()
    val enablePermanentDeletion by viewModel.enablePermanentDeletion.collectAsState()

    // Local lists
    val recentlyPlayedSongs by viewModel.recentlyPlayedSongs.collectAsState()
    val newlyAddedSongs by viewModel.newlyAddedSongs.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val albumsList by viewModel.albumsFlow.collectAsState()
    val artistsList by viewModel.artistsFlow.collectAsState()
    val foldersList by viewModel.foldersFlow.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.updateProfilePicUri(uri.toString())
        }
    }

    val homeTabs = listOf(
        "All Songs",
        "Recently Played",
        "Newly Added",
        "Most Played",
        "Favorites",
        "Albums",
        "Artists",
        "Folders"
    )
    var activeTab by remember { mutableStateOf("All Songs") }

    // Dialog state for drills/album/artist explore inside home
    var drillDownTitle by remember { mutableStateOf<String?>(null) }
    var drillDownSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var activeMenuSong by remember { mutableStateOf<Song?>(null) }

    // Dynamic Greeting based on time
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning, explorer"
            in 12..16 -> "Good Afternoon, pilot"
            else -> "Good Evening, Carter"
        }
    }
    val sortedAll = remember(allSongs) { allSongs.sortedBy { it.title.lowercase() } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_lazy_column"),
        contentPadding = PaddingValues(bottom = 130.dp) // Avoid overlap with bottom nav
    ) {
        // 1. GREETING HEADER
        item(key = "greeting_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CyberViolet.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_ayvan_logo),
                                contentDescription = "Ayvaaan Logo",
                                modifier = Modifier.size(38.dp) // 32dp-40dp requested
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Ayvaaan",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextLight,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Feel Music Beyond Sound",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CyberCyan.copy(alpha = 0.85f),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Settings shortcut button
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                    .testTag("home_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings Icon Control",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Cyber Avatar with upload capabilities
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                CyberViolet.copy(alpha = 0.35f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .border(2.dp, Brush.linearGradient(colors = listOf(CyberViolet, CyberPink)), CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") }
                                    .testTag("profile_avatar_container")
                            ) {
                                if (!profilePicUri.isNullOrBlank()) {
                                    AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(profilePicUri)
                .size(160)
                .crossfade(true)
                .build(),
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color(0xFF222232)),
            contentDescription = "User Avatar",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF13131D)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Placeholder silhouette",
                                            tint = CyberCyan.copy(alpha = 0.85f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Overlay edit photo badge
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(CyberPink)
                                        .border(1.dp, Color.White, CircleShape)
                                        .align(Alignment.BottomEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Upload Photo Badge",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. SEARCH BAR
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Search songs, artists, albums",
                                color = TextDim,
                                fontSize = 14.sp
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = TextLight,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { onNavigateToLibrary() }) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Library tuner",
                                    tint = CyberPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextDim,
                            focusedBorderColor = CyberViolet.copy(alpha = 0.8f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = Color(0x221B1B26),
                            unfocusedContainerColor = Color(0x221B1B26)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("home_search_bar")
                    )
                }
            }
        }

        // Live Search Results Override
        if (searchQuery.isNotBlank()) {
            item(key = "search_title_override") {
                Text(
                    text = "Search Results (${filteredSongs.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            if (filteredSongs.isEmpty()) {
                item(key = "search_empty_override") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.ui.components.AyvanEmptyState(
                            title = "No Matches Found",
                            message = "No local songs match your query."
                        )
                    }
                }
            } else {
                items(filteredSongs.take(25), key = { "search_${it.id}" }) { song ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        HorizontalSongItem(
                            song = song,
                            onClick = { onSongSelected(song) },
                            onAddToPlaylist = { viewModel.showAddToPlaylistDialog(song) },
                            viewModel = viewModel,
                            enablePermanentDeletion = enablePermanentDeletion,
                            onOptionsClick = { activeMenuSong = song }
                        )
                    }
                }
            }
        } else {
            // NORMAL FLOW

            // 3. CATEGORY TABS
            item(key = "category_tabs_stripe") {
                ScrollableTabRow(
                    edgePadding = 24.dp,
                    selectedTabIndex = homeTabs.indexOf(activeTab).coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {},
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    homeTabs.forEach { tab ->
                        val selected = tab == activeTab
                        Tab(
                            selected = selected,
                            onClick = { activeTab = tab },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        brush = if (selected) {
                                            Brush.linearGradient(colors = listOf(CyberViolet, CyberPink))
                                        } else {
                                            Brush.linearGradient(colors = listOf(Color(0x33ffffff), Color(0x1aFFFFFF)))
                                        }
                                    )
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) TextLight else TextDim
                                )
                            }
                        }
                    }
                }
            }

            // Display dynamic list based on selected category tab
            when (activeTab) {
                "All Songs" -> {
                    if (sortedAll.isEmpty()) {
                        item(key = "all_songs_empty") {
                            com.example.ui.components.AyvanEmptyState(
                                title = "No local music found.",
                                message = "Add music files to your device and refresh your library.",
                                actionLabel = "Refresh Library",
                                onActionClick = { viewModel.rescanLibrary() }
                            )
                        }
                    } else {
                        val limitAll = sortedAll.take(6)
                        items(limitAll, key = { "tab_all_${it.id}" }) { song ->
                            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                HorizontalSongItem(
                                    song = song,
                                    onClick = { viewModel.selectAndPlayQueue(song, sortedAll) },
                                    onAddToPlaylist = { viewModel.showAddToPlaylistDialog(song) },
                                    viewModel = viewModel,
                                    enablePermanentDeletion = enablePermanentDeletion,
                                    onOptionsClick = { activeMenuSong = song }
                                )
                            }
                        }
                        
                        item(key = "view_all_songs_btn") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier
                                        .clickable { onNavigateToLibrary() }
                                        .testTag("view_all_songs_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "See More",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextLight
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Forward arrow",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "Recently Played" -> {
                    if (recentlyPlayedSongs.isEmpty()) {
                        item(key = "recently_played_empty") {
                            Text(
                                text = "No tracks in your listening history yet.",
                                color = TextDim,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        items(recentlyPlayedSongs, key = { "tab_recent_${it.id}" }) { song ->
                            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                HorizontalSongItem(
                                    song = song,
                                    onClick = { viewModel.selectAndPlayQueue(song, recentlyPlayedSongs) },
                                    onAddToPlaylist = { viewModel.showAddToPlaylistDialog(song) },
                                    viewModel = viewModel,
                                    enablePermanentDeletion = enablePermanentDeletion,
                                    onOptionsClick = { activeMenuSong = song }
                                )
                            }
                        }
                    }
                }
                "Newly Added" -> {
                    if (newlyAddedSongs.isEmpty()) {
                        item(key = "newly_added_empty") {
                            Text(
                                text = "No newly added songs.",
                                color = TextDim,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        items(newlyAddedSongs, key = { "tab_new_${it.id}" }) { song ->
                            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                HorizontalSongItem(
                                    song = song,
                                    onClick = { viewModel.selectAndPlayQueue(song, newlyAddedSongs) },
                                    onAddToPlaylist = { viewModel.showAddToPlaylistDialog(song) },
                                    viewModel = viewModel,
                                    enablePermanentDeletion = enablePermanentDeletion,
                                    onOptionsClick = { activeMenuSong = song }
                                )
                            }
                        }
                    }
                }
                "Most Played" -> {
                    if (mostPlayedSongs.isEmpty()) {
                        item(key = "most_played_empty") {
                            Text(
                                text = "Play songs to see your most popular local tracks here.",
                                color = TextDim,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        items(mostPlayedSongs, key = { "tab_most_${it.id}" }) { song ->
                            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                HorizontalSongItem(
                                    song = song,
                                    onClick = { viewModel.selectAndPlayQueue(song, mostPlayedSongs) },
                                    onAddToPlaylist = { viewModel.showAddToPlaylistDialog(song) },
                                    viewModel = viewModel,
                                    enablePermanentDeletion = enablePermanentDeletion,
                                    onOptionsClick = { activeMenuSong = song }
                                )
                            }
                        }
                    }
                }
                "Favorites" -> {
                    if (favoriteSongs.isEmpty()) {
                        item(key = "favorites_empty") {
                            Text(
                                text = "Tap the favorite icon while playing a song to view it here.",
                                color = TextDim,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        items(favoriteSongs, key = { "tab_fav_${it.id}" }) { song ->
                            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                HorizontalSongItem(
                                    song = song,
                                    onClick = { viewModel.selectAndPlayQueue(song, favoriteSongs) },
                                    onAddToPlaylist = { viewModel.showAddToPlaylistDialog(song) },
                                    viewModel = viewModel,
                                    enablePermanentDeletion = enablePermanentDeletion,
                                    onOptionsClick = { activeMenuSong = song }
                                )
                            }
                        }
                    }
                }
                "Albums" -> {
                    if (albumsList.isEmpty()) {
                        item(key = "albums_tab_empty") {
                            Text(
                                text = "No local albums found.",
                                color = TextDim,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        item(key = "albums_horizontal_grid") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(albumsList, key = { "album_tab_$it" }) { albumName ->
                                    val albumSongs = remember(allSongs, albumName) { allSongs.filter { it.album == albumName } }
                                    Box(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clickable {
                                                drillDownTitle = albumName
                                                drillDownSongs = albumSongs
                                            }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Column {
                                            Box(
                                                modifier = Modifier
                                                    .size(130.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(Color.White.copy(alpha = 0.05f))
                                            ) {
                                                val contextReq = androidx.compose.ui.platform.LocalContext.current
                                                val artReq = remember(albumSongs, contextReq) {
                                                    coil.request.ImageRequest.Builder(contextReq)
                                                        .data(albumSongs.firstOrNull()?.artworkUri)
                                                        .size(400)
                                                        .crossfade(true)
                                                        .build()
                                                }
                                                AsyncImage(
                                                    model = artReq,
                                                    contentDescription = albumName,
                                                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF222232)),
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = albumName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextLight,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${albumSongs.size} tracks",
                                                fontSize = 11.sp,
                                                color = TextDim
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "Artists" -> {
                    if (artistsList.isEmpty()) {
                        item(key = "artists_tab_empty") {
                            Text(
                                text = "No local artists found.",
                                color = TextDim,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        item(key = "artists_horizontal_grid") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(artistsList, key = { "artist_tab_$it" }) { artistName ->
                                    val artistSongs = remember(allSongs, artistName) { allSongs.filter { it.artist == artistName } }
                                    Box(
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clickable {
                                                drillDownTitle = artistName
                                                drillDownSongs = artistSongs
                                            }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(100.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.05f))
                                            ) {
                                                val contextReq = androidx.compose.ui.platform.LocalContext.current
                                                val artReq = remember(artistSongs, contextReq) {
                                                    coil.request.ImageRequest.Builder(contextReq)
                                                        .data(artistSongs.firstOrNull()?.artworkUri)
                                                        .size(400)
                                                        .crossfade(true)
                                                        .build()
                                                }
                                                AsyncImage(
                                                    model = artReq,
                                                    contentDescription = artistName,
                                                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF222232)),
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = artistName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextLight,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${artistSongs.size} tracks",
                                                fontSize = 11.sp,
                                                color = TextDim
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "Folders" -> {
                    if (foldersList.isEmpty()) {
                        item(key = "folders_tab_empty") {
                            Text(
                                text = "No folders found.",
                                color = TextDim,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        items(foldersList, key = { "tab_folder_$it" }) { folderName ->
                            val folderSongs = remember(allSongs, folderName) { allSongs.filter { it.folder == folderName } }
                            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardGrey.copy(alpha = 0.4f))
                                        .clickable {
                                            drillDownTitle = "Folder: $folderName"
                                            drillDownSongs = folderSongs
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Folder",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = folderName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextLight
                                        )
                                        Text(
                                            text = "${folderSongs.size} tracks",
                                            fontSize = 11.sp,
                                            color = TextDim
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                                        contentDescription = "Explore",
                                        tint = TextDim
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Render permanent decorative carousels *only* when viewing All Songs tab to reduce visual clutter and system load
            if (activeTab == "All Songs") {
                item(key = "extra_spacing_divider") {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // 4. RECENTLY PLAYED SECTION BELOW
                if (recentlyPlayedSongs.isNotEmpty()) {
                    item(key = "sec_recent_title") {
                        Text(
                            text = "Recently Played",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    item(key = "sec_recent_list") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(recentlyPlayedSongs.take(8), key = { "sec_rec_${it.id}" }) { song ->
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clickable { viewModel.selectAndPlayQueue(song, recentlyPlayedSongs) }
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                        ) {
                                            AsyncImage(
                                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(song.artworkUri).size(400).crossfade(true).build(),
                                                contentDescription = song.title,
                                                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF222232)),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = song.title,
                                            fontSize = 13.sp,
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            fontSize = 11.sp,
                                            color = TextDim,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "sec_recent_divider") {
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                // 5. MOST PLAYED SONGS SECTION
                val topTenMostPlayed = mostPlayedSongs.take(10)
                if (topTenMostPlayed.isNotEmpty()) {
                    item(key = "sec_most_title") {
                        Text(
                            text = "Most Played Songs",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    items(topTenMostPlayed, key = { "sec_most_${it.id}" }) { song ->
                        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectAndPlayQueue(song, topTenMostPlayed) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Artwork
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x33ffffff))
                                ) {
                                    val contextReq = androidx.compose.ui.platform.LocalContext.current
                                    val artReq = remember(song.artworkUri, contextReq) {
                                        coil.request.ImageRequest.Builder(contextReq)
                                            .data(song.artworkUri)
                                            .size(400)
                                            .crossfade(true)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = artReq,
                                        contentDescription = song.title,
                                        placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF222232)),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${song.artist} • ${song.album}",
                                        fontSize = 12.sp,
                                        color = TextDim,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Play Count Display
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CyberCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "▶ ${song.playCount} Plays",
                                        fontSize = 11.sp,
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    item(key = "sec_most_divider") {
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                // 6. NEWLY ADDED SONGS SECTION
                val displayNewlyAdded = newlyAddedSongs.take(8)
                if (displayNewlyAdded.isNotEmpty()) {
                    item(key = "sec_new_songs_title") {
                        Text(
                            text = "Newly Added Songs",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    item(key = "sec_newly_list") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(displayNewlyAdded, key = { "sec_new_${it.id}" }) { song ->
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clickable { viewModel.selectAndPlayQueue(song, displayNewlyAdded) }
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                        ) {
                                            AsyncImage(
                                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(song.artworkUri).size(400).crossfade(true).build(),
                                                contentDescription = song.title,
                                                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF222232)),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = song.title,
                                            fontSize = 13.sp,
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            fontSize = 11.sp,
                                            color = TextDim,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "sec_newly_divider") {
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                // 7. ALBUMS SECTION
                if (albumsList.isNotEmpty()) {
                    item(key = "sec_albums_title") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Albums",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight
                            )
                        }
                    }

                    item(key = "sec_albums_list") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(albumsList.take(6), key = { "sec_album_$it" }) { albumName ->
                                val albumSongs = remember(allSongs, albumName) { allSongs.filter { it.album == albumName } }
                                Box(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable {
                                            drillDownTitle = albumName
                                            drillDownSongs = albumSongs
                                        }
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .size(130.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                        ) {
                                            AsyncImage(
                                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(albumSongs.firstOrNull()?.artworkUri).size(400).crossfade(true).build(),
                                                contentDescription = albumName,
                                                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF222232)),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = albumName,
                                            fontSize = 13.sp,
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${albumSongs.size} tracks",
                                            fontSize = 11.sp,
                                            color = TextDim
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "sec_album_divider") {
                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }

                // 8. ARTISTS SECTION
                if (artistsList.isNotEmpty()) {
                    item(key = "sec_artists_title") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Artists",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight
                            )
                        }
                    }

                    item(key = "sec_artists_list") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(artistsList.take(6), key = { "sec_artist_$it" }) { artistName ->
                                val artistSongs = remember(allSongs, artistName) { allSongs.filter { it.artist == artistName } }
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .clickable {
                                            drillDownTitle = artistName
                                            drillDownSongs = artistSongs
                                        }
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.05f))
                                        ) {
                                            AsyncImage(
                                                model = artistSongs.firstOrNull()?.artworkUri,
                                                contentDescription = artistName,
                                                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF222232)),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = artistName,
                                            fontSize = 13.sp,
                                            color = TextLight,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${artistSongs.size} tracks",
                                            fontSize = 11.sp,
                                            color = TextDim
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal drilldown dialog to view folder/album/artist tracks
    drillDownTitle?.let { title ->
        FilteredSongsDialog(
            title = title,
            songs = drillDownSongs,
            onDismiss = {
                drillDownTitle = null
                drillDownSongs = emptyList()
            },
            onSongSelected = { song ->
                viewModel.selectAndPlayQueue(song, drillDownSongs)
                drillDownTitle = null
                drillDownSongs = emptyList()
            },
            viewModel = viewModel
        )
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
fun FilteredSongsDialog(
    title: String,
    songs: List<Song>,
    onDismiss: () -> Unit,
    onSongSelected: (Song) -> Unit,
    viewModel: MusicViewModel
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyberCyan)
            }
        },
        title = {
            Text(text = title, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 380.dp)) {
                if (songs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        com.example.ui.components.AyvanEmptyState(
                            title = "Nothing Here",
                            message = "No songs found for this selection."
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(songs, key = { "home_nested_${it.id}" }) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSongSelected(song)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(song.artworkUri)
                .size(160)
                .crossfade(true)
                .build(),
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(androidx.compose.ui.graphics.Color(0xFF222232)),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        fontSize = 11.sp,
                                        color = TextDim,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF161622),
        shape = RoundedCornerShape(24.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
fun HorizontalSongItem(
    song: Song,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    viewModel: MusicViewModel,
    enablePermanentDeletion: Boolean,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag("song_row_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork - optimized asynchronous downsampled decode
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x33ffffff))
        ) {
            val context = LocalContext.current
            val artRequest = remember(song.artworkUri) {
                coil.request.ImageRequest.Builder(context)
                    .data(song.artworkUri)
                    .size(120, 120) // Downsample to 120x120 to completely bypass heap pressure & GC scrolling stutter
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

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                fontSize = 12.sp,
                color = TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // More Menu Three Dots Button - triggers shared parent State dialog instead of allocating a separate popup
        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.testTag("home_more_${song.id}")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = TextLight,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SongOptionsMenuDialog(
    song: Song,
    onDismiss: () -> Unit,
    viewModel: MusicViewModel,
    enablePermanentDeletion: Boolean,
    onAddToPlaylist: () -> Unit
) {
    var showSongDetails by remember { mutableStateOf(false) }
    if (showSongDetails) {
        SongDetailsDialog(song = song, onDismiss = { showSongDetails = false })
    }
    
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x1affffff))
                ) {
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
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = song.title,
                        color = TextLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        color = TextDim,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                data class OptionItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val hue: Color, val clickAction: () -> Unit)
                val options = listOf(
                    OptionItem("Play Next", Icons.Default.SkipNext, CyberPink) {
                        viewModel.playNext(song)
                        Toast.makeText(context, "${song.title} added to play next", Toast.LENGTH_SHORT).show()
                    },
                    OptionItem("Play Last", Icons.Default.LowPriority, CyberCyan) {
                        viewModel.playLast(song)
                        Toast.makeText(context, "${song.title} added to play last", Toast.LENGTH_SHORT).show()
                    },
                    OptionItem("Play Similar Songs", Icons.Default.QueueMusic, CyberViolet) {
                        viewModel.playSimilarSongs(song)
                        Toast.makeText(context, "Queue created with similar songs", Toast.LENGTH_SHORT).show()
                    },
                    OptionItem("Add To Playlist", Icons.Default.PlaylistAdd, CyberCyan) {
                        onAddToPlaylist()
                    },
                    OptionItem("Set as Ringtone", Icons.Default.Notifications, CyberViolet) {
                        viewModel.setAsRingtone(song) { success, msg ->
                            if (!success && msg == "PERMISSION_REQUIRED") {
                                try {
                                    Toast.makeText(context, "Settings write permission required. Redirecting...", Toast.LENGTH_LONG).show()
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:" + context.packageName)
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    OptionItem("Share Song", Icons.Default.Share, CyberCyan) {
                        viewModel.shareSong(song) { success, msg ->
                            if (!success) {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    OptionItem("Song Details", Icons.Default.Info, CyberCyan) {
                        showSongDetails = true
                    },
                    OptionItem(if (enablePermanentDeletion) "Delete Song" else "Remove From Library", Icons.Default.Delete, Color.Red) {
                        viewModel.requestDeleteSong(song)
                    }
                )
                
                options.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (item.label != "Song Details") {
                                    onDismiss()
                                }
                                item.clickAction()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = item.hue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = item.label,
                            color = if (item.label.startsWith("Delete") || item.label.startsWith("Remove")) Color.Red else TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyberCyan, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF161622),
        shape = RoundedCornerShape(24.dp)
    )
}
