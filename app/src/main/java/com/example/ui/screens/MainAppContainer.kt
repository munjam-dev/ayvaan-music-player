package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.Song
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel

enum class AppNavigationTab {
    HOME, LIBRARY, EXPLORE, PLAYLIST, SETTINGS
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppContainer(viewModel: MusicViewModel) {
    var activeTab by remember { mutableStateOf(AppNavigationTab.HOME) }
    val isNowPlayingVisible by viewModel.isNowPlayingVisible.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    val activeDeleteRequestSong by viewModel.activeDeleteRequestSong.collectAsState()
    val enablePermanentDeletion by viewModel.enablePermanentDeletion.collectAsState()
    val pendingUndoState by viewModel.pendingUndoState.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                return@remember currentContext
            }
            if (currentContext.baseContext == currentContext) break
            currentContext = currentContext.baseContext
        }
        null
    }

    // Intercept back presses
    // 1. If Now Playing is visible, close it
    BackHandler(enabled = isNowPlayingVisible) {
        viewModel.setNowPlayingVisible(false)
    }

    // 2. If on any secondary tab, navigate to Home Tab
    BackHandler(enabled = !isNowPlayingVisible && activeTab != AppNavigationTab.HOME) {
        activeTab = AppNavigationTab.HOME
    }

    // 3. If on Home tab, move task to back like a premium media player
    BackHandler(enabled = !isNowPlayingVisible && activeTab == AppNavigationTab.HOME) {
        activity?.moveTaskToBack(true)
    }

    val currentSong by viewModel.currentPlayingSongWithFavorite.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PitchBlack,
        bottomBar = {
            // Persistent Floating Bottom Navigation Row (Respects system insets!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // SAFE AREA: keeps bar above gesture bar
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // Outer glow glass cards
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xD912121D)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .testTag("app_bottom_nav_bar")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home Tab
                        BottomNavItem(
                            icon = Icons.Default.Home,
                            label = "Home",
                            isSelected = activeTab == AppNavigationTab.HOME,
                            onClick = { activeTab = AppNavigationTab.HOME },
                            tag = "nav_home"
                        )

                        // Library Tab
                        BottomNavItem(
                            icon = Icons.Default.QueueMusic,
                            label = "Library",
                            isSelected = activeTab == AppNavigationTab.LIBRARY,
                            onClick = { activeTab = AppNavigationTab.LIBRARY },
                            tag = "nav_library"
                        )

                        // Explore Tab
                        BottomNavItem(
                            icon = Icons.Default.Explore,
                            label = "Explore",
                            isSelected = activeTab == AppNavigationTab.EXPLORE,
                            onClick = { activeTab = AppNavigationTab.EXPLORE },
                            tag = "nav_explore"
                        )

                        // Playlists / Collections Tab
                        BottomNavItem(
                            icon = Icons.Default.Folder,
                            label = "Playlists",
                            isSelected = activeTab == AppNavigationTab.PLAYLIST,
                            onClick = { activeTab = AppNavigationTab.PLAYLIST },
                            tag = "nav_playlist"
                        )

                        // Settings Tab
                        BottomNavItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            isSelected = activeTab == AppNavigationTab.SETTINGS,
                            onClick = { activeTab = AppNavigationTab.SETTINGS },
                            tag = "nav_settings"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Main Screen Router with transitions
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack)
        ) {
            when (activeTab) {
                AppNavigationTab.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onSongSelected = { song ->
                        viewModel.selectAndPlay(song)
                        viewModel.setNowPlayingVisible(true)
                    },
                    onNavigateToLibrary = { activeTab = AppNavigationTab.LIBRARY },
                    onNavigateToSettings = { activeTab = AppNavigationTab.SETTINGS }
                )
                AppNavigationTab.LIBRARY -> LibraryScreen(
                    viewModel = viewModel,
                    onSongSelected = { song ->
                        viewModel.selectAndPlay(song)
                        viewModel.setNowPlayingVisible(true)
                    }
                )
                AppNavigationTab.EXPLORE -> ExploreScreen(
                    viewModel = viewModel,
                    onSongSelected = { song ->
                        viewModel.selectAndPlay(song)
                        viewModel.setNowPlayingVisible(true)
                    }
                )
                AppNavigationTab.PLAYLIST -> PlaylistScreen(
                    viewModel = viewModel,
                    onSongSelected = { song ->
                        viewModel.selectAndPlay(song)
                        viewModel.setNowPlayingVisible(true)
                    }
                )
                AppNavigationTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel
                )
            }

            // Universal Floating Mini Player
            val currentSongState = currentSong
            if (currentSongState != null) {
                // Sit above bottom navigation dynamically! (Avoid overlap)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding() // Respect safe areas
                        .padding(bottom = 92.dp) // Lift precisely above bottom bar
                        .padding(horizontal = 24.dp)
                ) {
                    FloatingMiniPlayer(
                        song = currentSongState,
                        isPlaying = isPlaying,
                        positionFlow = viewModel.playbackPosition,
                        durationFlow = viewModel.playbackDuration,
                        onPlayPauseToggle = { viewModel.playPause() },
                        onNext = { viewModel.nextTrend() },
                        onClick = { viewModel.setNowPlayingVisible(true) }
                    )
                }
            }

            // Universal Floating Undo Banner
            val undoState = pendingUndoState
            if (undoState != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 175.dp) // Float perfectly above the Mini Player
                        .padding(horizontal = 24.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xF21E1E2B)),
                        border = BorderStroke(1.dp, CyberPink.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Song Deleted: ${undoState.song.title}",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "UNDO",
                                color = CyberPink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .clickable { viewModel.performUndoDelete() }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    val deleteSongState = activeDeleteRequestSong
    if (deleteSongState != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteRequest() },
            title = {
                Text(
                    text = if (enablePermanentDeletion) "Delete Song?" else "Remove From Library?",
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (enablePermanentDeletion) {
                            "Are you sure you want to permanently delete this song from your device?"
                        } else {
                            "Are you sure you want to remove this song from your library?"
                        },
                        color = TextDim,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = deleteSongState.title,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = deleteSongState.artist,
                        color = TextLight.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSong(deleteSongState)
                        viewModel.dismissDeleteRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (enablePermanentDeletion) Color.Red else CyberPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (enablePermanentDeletion) "Delete" else "Remove", color = TextLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteRequest() }) {
                    Text("Cancel", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E1E2A)
        )
    }

    val globalAddToPlaylistSong by viewModel.activeAddToPlaylistSong.collectAsState()
    val addtoPlaylistSongState = globalAddToPlaylistSong
    if (addtoPlaylistSongState != null) {
        AddToPlaylistDialog(
            song = addtoPlaylistSongState,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissAddToPlaylistDialog() }
        )
    }

    // Overlay full-screen Now Playing screen slide-up animation
    AnimatedVisibility(
        visible = isNowPlayingVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(400, easing = EaseOutCubic)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(400, easing = EaseInCubic)
        )
    ) {
        NowPlayingScreen(
            viewModel = viewModel,
            onCollapse = { viewModel.setNowPlayingVisible(false) }
        )
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val activeColor = CyberCyan
    val inactiveColor = TextDim

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        // Icon with ambient pill indicator
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(42.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                // Glowing background pill
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberViolet.copy(alpha = 0.25f))
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) activeColor else inactiveColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) TextLight else TextDim,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MiniPlayerProgressBar(
    positionFlow: kotlinx.coroutines.flow.StateFlow<Long>,
    durationFlow: kotlinx.coroutines.flow.StateFlow<Long>,
    modifier: Modifier = Modifier
) {
    val position by positionFlow.collectAsState()
    val duration by durationFlow.collectAsState()
    if (duration > 0) {
        val ratio = position.toFloat() / duration
        Box(
            modifier = modifier
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .height(2.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(CyberViolet, CyberPink)
                    )
                )
        )
    }
}

@Composable
fun FloatingMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    positionFlow: kotlinx.coroutines.flow.StateFlow<Long>,
    durationFlow: kotlinx.coroutines.flow.StateFlow<Long>,
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xEC12121F)),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.22f)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .testTag("floating_mini_player")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle active profile cover image
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33ffffff))
                ) {
                    val resolvedUri = if (song.artworkUri.isNullOrEmpty() || song.artworkUri.startsWith("content://media/external/audio/albumart/")) {
                        Song.getDeterministicFallbackUrl(song.title, song.artist)
                    } else {
                        song.artworkUri
                    }
                    AsyncImage(
                        model = resolvedUri,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track name & creator
                Column(modifier = Modifier.weight(1f)) {
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
                        color = CyberCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play Pause control button inside Mini Player (avoid double clicks triggering overlay launch)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onPlayPauseToggle() }
                        .testTag("mini_player_play_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Toggle playback state",
                        tint = TextLight,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Skip Next button inside Mini Player
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("mini_player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Skip track",
                        tint = TextLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Real-time micro edge seek indicator at the bottom border! (Brilliant premium detail)
            MiniPlayerProgressBar(
                positionFlow = positionFlow,
                durationFlow = durationFlow,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
