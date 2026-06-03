package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun ExploreScreen(
    viewModel: MusicViewModel,
    onSongSelected: (Song) -> Unit
) {
    val songs by viewModel.songsWithFavoritesState.collectAsState()
    val playHistory by viewModel.recentHistory.collectAsState()
    val newlyAddedSongs by viewModel.newlyAddedSongs.collectAsState()

    // 1. DYNAMIC STATISTICS CALCULATIONS
    val totalSongs = songs.size
    val totalAlbums = remember(songs) { songs.map { it.album }.distinct().size }
    val totalArtists = remember(songs) { songs.map { it.artist }.distinct().size }
    val totalDurationMs = remember(songs) { songs.sumOf { it.duration } }
    val totalDurationFormatted = remember(totalDurationMs) {
        val hrs = totalDurationMs / 3600000
        val mins = (totalDurationMs % 3600000) / 60000
        if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }

    val totalStorageBytes = remember(songs) {
        var size = 0L
        songs.forEach { s ->
            if (s.filePath.startsWith("virtual_track_")) {
                size += 6L * 1024 * 1024 // 6MB representational
            } else {
                try {
                    val file = java.io.File(s.filePath)
                    if (file.exists()) {
                        size += file.length()
                    } else {
                        size += 5L * 1024 * 1024
                    }
                } catch (e: Exception) {
                    size += 5L * 1024 * 1024
                }
            }
        }
        size
    }

    val storageText = remember(totalStorageBytes) {
        val mb = totalStorageBytes.toDouble() / (1024 * 1024)
        if (mb > 1024) {
            String.format("%.2f GB", mb / 1024)
        } else {
            String.format("%.1f MB", mb)
        }
    }

    // 2. RECENTLY ADDED ALBUMS (Sorted by highest dateAdded)
    val recentlyAddedAlbums = remember(songs) {
        songs.groupBy { it.album }
            .map { (albumName, albumSongs) ->
                val newestSong = albumSongs.maxByOrNull { it.dateAdded }
                val artist = albumSongs.firstOrNull()?.artist ?: "Unknown Artist"
                val artworkUri = albumSongs.firstOrNull()?.artworkUri
                val maxDate = newestSong?.dateAdded ?: 0L
                Triple(albumName, artist, artworkUri) to maxDate
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    // 3. TOP ARTISTS (Based on local track count in storage)
    val topArtists = remember(songs) {
        songs.groupBy { it.artist }
            .map { (artistName, artistSongs) ->
                artistName to artistSongs
            }
            .sortedByDescending { it.second.size }
            .take(10)
    }

    // Most Played Artists from Listening History
    val mostPlayedArtists = remember(songs, playHistory) {
        if (playHistory.isEmpty()) {
            emptyList()
        } else {
            val pathArtistMap = songs.associate { it.filePath to it.artist }
            playHistory.mapNotNull { pathArtistMap[it.songPath] }
                .groupBy { it }
                .map { (artistName, plays) ->
                    val matchingSongs = songs.filter { it.artist == artistName }
                    artistName to Pair(plays.size, matchingSongs)
                }
                .sortedByDescending { it.second.first }
                .take(6)
        }
    }

    // 4. GENRE COLLECTIONS UTILITIES
    fun detectGenre(song: Song): String {
        val text = "${song.title} ${song.artist} ${song.album} ${song.filePath}".lowercase()
        return when {
            text.contains("telugu") || text.contains("tollywood") || text.contains("sid sriram") || text.contains("dsp") -> "Telugu"
            text.contains("hindi") || text.contains("bollywood") || text.contains("arijit") || text.contains("shreya") -> "Hindi"
            text.contains("tamil") || text.contains("kollywood") || text.contains("ar rahman") || text.contains("anirudh") -> "Tamil"
            text.contains("devotional") || text.contains("bhakti") || text.contains("stotra") || text.contains("shiva") || text.contains("temple") -> "Devotional"
            text.contains("classical") || text.contains("carnatic") || text.contains("tabla") -> "Classical"
            text.contains("folk") || text.contains("janapada") || text.contains("desi") -> "Folk"
            text.contains("rock") || text.contains("pink floyd") || text.contains("linkin") || text.contains("metallica") -> "Rock"
            text.contains("pop") || text.contains("swift") || text.contains("sheeran") || text.contains("bieber") || text.contains("dido") -> "Pop"
            text.contains("cyber") || text.contains("synth") || text.contains("neon") || text.contains("original") -> "Synth & Electronic"
            else -> "English"
        }
    }

    val detectedGenres = remember(songs) {
        songs.groupBy { detectGenre(it) }.filter { it.value.isNotEmpty() }
    }

    // 5. FOLDER EXPLORER UTILITIES
    val folderGroups = remember(songs) {
        songs.groupBy { it.folder }
    }

    // 6. LISTENING INSIGHTS
    val insights = remember(songs, playHistory) {
        val list = mutableListOf<String>()
        if (playHistory.isNotEmpty()) {
            val calendar = java.util.Calendar.getInstance()
            val hourDistribution = playHistory.map {
                calendar.timeInMillis = it.timestamp
                calendar.get(java.util.Calendar.HOUR_OF_DAY)
            }
            val peakHour = hourDistribution.groupBy { it }.maxByOrNull { it.value.size }?.key
            val peakTimeName = when (peakHour) {
                null -> "Quiet hours"
                in 5..11 -> "Morning rhythm (5 AM - 12 PM)"
                in 12..16 -> "Afternoon chill (12 PM - 5 PM)"
                in 17..21 -> "Sunset vibes (5 PM - 10 PM)"
                else -> "Midnight focus (10 PM - 5 AM)"
            }
            list.add("Peak Hour: $peakTimeName")

            val histArtistMap = songs.associate { it.filePath to it.artist }
            val favHistoryArtist = playHistory.mapNotNull { histArtistMap[it.songPath] }
                .groupBy { it }
                .maxByOrNull { it.value.size }?.key
            if (favHistoryArtist != null) {
                list.add("Most Played Artist: $favHistoryArtist")
            }
        }
        if (songs.isNotEmpty()) {
            val avgSecs = (songs.map { it.duration }.average() / 1000).toInt()
            list.add("Average Song length: ${avgSecs / 60}m ${avgSecs % 60}s")
        }
        if (list.isEmpty()) {
            list.add("Keep listening to offline tracks to unlock full analytics!")
            list.add("Insights update live as you navigate and play tracks.")
        }
        list
    }

    // Drill down sheets state
    var selectedDrillTitle by remember { mutableStateOf<String?>(null) }
    var selectedDrillSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp) // Avoid overlapping with global bottom players
    ) {
        // Atmosphere Explorer header block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_ayvan_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Explore",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextLight,
                        letterSpacing = (-0.5).sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Explore actual local audio collections and listening stats.",
                    fontSize = 12.sp,
                    color = TextDim
                )
            }
        }

        // 1. GENRES
        if (detectedGenres.isNotEmpty()) {
            Text(
                text = "Genres Found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            ScrollableRowGenres(
                genres = detectedGenres,
                onGenreClick = { genreName, list ->
                    selectedDrillTitle = "$genreName Collection"
                    selectedDrillSongs = list
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 2. RECENTLY ADDED ALBUMS (ALBUMS)
        if (recentlyAddedAlbums.isNotEmpty()) {
            Text(
                text = "Recently Added Albums",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(recentlyAddedAlbums.take(8), key = { "album_${it.first}_${it.second}" }) { triple ->
                    val (albumName, artist, artworkUri) = triple
                    val albumSongs = remember(songs) { songs.filter { it.album == albumName } }
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .clickable {
                                selectedDrillTitle = albumName
                                selectedDrillSongs = albumSongs
                            }
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                            ) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(artworkUri).size(400).crossfade(true).build(),
                                    contentDescription = albumName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = albumName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = artist,
                                fontSize = 11.sp,
                                color = TextDim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. TOP ARTISTS & MOST PLAYED ARTISTS (ARTISTS)
        if (topArtists.isNotEmpty()) {
            Text(
                text = "Top Artists",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(topArtists.take(8), key = { "artist_${it.first}" }) { pair ->
                    val (artistName, artistSongs) = pair
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .clickable {
                                selectedDrillTitle = artistName
                                selectedDrillSongs = artistSongs
                            }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.04f))
                            ) {
                                AsyncImage(
                                    model = artistSongs.firstOrNull()?.artworkUri,
                                    contentDescription = artistName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = artistName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${artistSongs.size} tracks",
                                fontSize = 10.sp,
                                color = TextDim
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (mostPlayedArtists.isNotEmpty()) {
            Text(
                text = "Most Played Artists",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(mostPlayedArtists, key = { "most_played_artist_${it.first}" }) { triple ->
                    val artistName = triple.first
                    val (plays, artistSongs) = triple.second
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .clickable {
                                selectedDrillTitle = artistName
                                selectedDrillSongs = artistSongs
                            }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.04f))
                            ) {
                                AsyncImage(
                                    model = artistSongs.firstOrNull()?.artworkUri,
                                    contentDescription = artistName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = artistName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${plays} plays",
                                fontSize = 10.sp,
                                color = CyberCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. FOLDER EXPLORER (FOLDERS)
        if (folderGroups.isNotEmpty()) {
            Text(
                text = "Folder Explorer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                folderGroups.forEach { (folderName, list) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardGrey.copy(alpha = 0.4f))
                            .clickable {
                                selectedDrillTitle = "Folder: $folderName"
                                selectedDrillSongs = list
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folder Icon",
                            tint = CyberCyan,
                            modifier = Modifier.size(26.dp)
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
                                text = "${list.size} local files",
                                fontSize = 11.sp,
                                color = TextDim
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                            contentDescription = "Open",
                            tint = TextDim
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5. RECENTLY ADDED (newlyAddedSongs)
        if (newlyAddedSongs.isNotEmpty()) {
            Text(
                text = "Recently Added Songs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(newlyAddedSongs.take(8), key = { "explore_new_${it.id}" }) { song ->
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .clickable {
                                onSongSelected(song)
                            }
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                            ) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current).data(song.artworkUri).size(400).crossfade(true).build(),
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = song.title,
                                fontSize = 13.sp,
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
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 6. MUSIC STATISTICS
        Text(
            text = "Music Statistics",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextLight,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CardGrey.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
                .testTag("explore_statistics_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMetricColumn(label = "Total Songs", value = totalSongs.toString(), icon = Icons.Default.MusicNote)
                    StatMetricColumn(label = "Total Albums", value = totalAlbums.toString(), icon = Icons.Default.Album)
                    StatMetricColumn(label = "Total Artists", value = totalArtists.toString(), icon = Icons.Default.Person)
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatMetricColumn(label = "Total Duration", value = totalDurationFormatted, icon = Icons.Default.Timer)
                    StatMetricColumn(label = "Storage Used", value = storageText, icon = Icons.Default.Storage)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7. LISTENING INSIGHTS CARD
        Text(
            text = "Listening Insights",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextLight,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CardGrey.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
                .testTag("explore_insights_card")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QueryStats,
                        contentDescription = "Insights Icon",
                        tint = CyberPink,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your Listening Habits",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                insights.forEach { insight ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberCyan)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = insight,
                            fontSize = 13.sp,
                            color = TextDim,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    // Modal drilldown songs view
    selectedDrillTitle?.let { title ->
        FilteredSongsDialog(
            title = title,
            songs = selectedDrillSongs,
            onDismiss = {
                selectedDrillTitle = null
                selectedDrillSongs = emptyList()
            },
            onSongSelected = { song ->
                viewModel.selectAndPlayQueue(song, selectedDrillSongs)
                selectedDrillTitle = null
                selectedDrillSongs = emptyList()
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun StatMetricColumn(label: String, value: String, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CyberCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextDim,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = TextLight
        )
    }
}

@Composable
fun ScrollableRowGenres(
    genres: Map<String, List<Song>>,
    onGenreClick: (String, List<Song>) -> Unit
) {
    val genreColors = listOf(CyberViolet, CyberPink, CyberCyan, CyberIndigo, Color(0xFFFF0055), Color(0xFF00FFCC))
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val keysList = genres.keys.toList()
        items(keysList.size) { index ->
            val genreName = keysList[index]
            val list = genres[genreName] ?: emptyList()
            val color = genreColors[index % genreColors.size]
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(85.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                color.copy(alpha = 0.55f),
                                color.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(1.dp, color.copy(alpha = 0.40f), RoundedCornerShape(18.dp))
                    .clickable { onGenreClick(genreName, list) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Genre Icon",
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = genreName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${list.size} songs",
                            fontSize = 10.sp,
                            color = TextDim
                        )
                    }
                }
            }
        }
    }
}
