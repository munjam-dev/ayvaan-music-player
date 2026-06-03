package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.Song
import com.example.player.RepeatMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import kotlin.math.abs

@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onCollapse: () -> Unit
) {
    val currentSong by viewModel.currentPlayingSongWithFavorite.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.playbackDuration.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val spectrums by viewModel.visualizerAmplitudes.collectAsState()
    val sleepRemaining by viewModel.sleepTimeRemaining.collectAsState()
    val artworkDominantColor by viewModel.artworkDominantColor.collectAsState()

    var showArtworkOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            currentSong?.id?.let { songId ->
                viewModel.saveCustomArtwork(songId, uri)
            }
        }
    }

    if (currentSong == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(PitchBlack),
            contentAlignment = Alignment.Center
        ) {
            Text("Select an offline song from library to load player", color = TextDim)
        }
        return
    }

    val song = currentSong ?: return

    // Double Tap Heart Animations configuration
    var heartAnimTrigger by remember(song.id) { mutableStateOf(0) }
    var showHeartAnimation by remember(song.id) { mutableStateOf(false) }
    val heartScale = remember(song.id) { Animatable(0f) }
    val heartAlpha = remember(song.id) { Animatable(0f) }

    LaunchedEffect(heartAnimTrigger) {
        if (heartAnimTrigger > 0) {
            try {
                showHeartAnimation = true
                coroutineScope {
                    launch {
                        heartScale.snapTo(0f)
                        heartScale.animateTo(
                            targetValue = 1.0f,
                            animationSpec = keyframes {
                                durationMillis = 800
                                0f at 0 with FastOutSlowInEasing
                                1.4f at 350 with FastOutSlowInEasing
                                1.0f at 800 with LinearEasing
                            }
                        )
                    }
                    launch {
                        heartAlpha.snapTo(1.0f)
                        heartAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 800, delayMillis = 100, easing = LinearEasing)
                        )
                    }
                }
            } finally {
                showHeartAnimation = false
                heartScale.snapTo(0f)
                heartAlpha.snapTo(0f)
            }
        }
    }

    // Artwork Rotation Animation Loop
    val infiniteTransition = rememberInfiniteTransition(label = "ArtworkSpin")
    val spinningRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "Spin"
    )

    // Sweep dynamics to swap tracks (Gestures!)
    var dragOffsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (abs(dragOffsetX) > 200f) {
                            if (dragOffsetX > 0) {
                                // Swept Right -> Play Previous
                                viewModel.previousTrend()
                            } else {
                                // Swept Left -> Play Next
                                viewModel.nextTrend()
                            }
                        }
                        dragOffsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetX += dragAmount.x
                    }
                )
            }
            .testTag("now_playing_screen")
    ) {
        // 7. Dynamic Floating Animated Background System (60 / 120 FPS)
        AnimatedDynamicGradientBackground(
            song = song,
            dominantColor = artworkDominantColor ?: Color(0xFF8E24AA)
        )

        // Soft, high-contrast dark overlay to keep primary text elements highly legible while letting gradients pop gracefully
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // A. TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_minimize_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize Player",
                        tint = TextLight,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "NOW PLAYING",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                    color = TextDim,
                    textAlign = TextAlign.Center
                )

                var showOptionsMenu by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { showOptionsMenu = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_dots_menu")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = TextLight,
                        modifier = Modifier.size(26.dp)
                    )
                }

                if (showOptionsMenu) {
                    NowPlayingOptionsDialog(
                        song = song,
                        viewModel = viewModel,
                        onDismiss = { showOptionsMenu = false }
                    )
                }

                if (showArtworkOptions) {
                    ArtworkOptionsDialog(
                        song = song,
                        onChooseFromGallery = {
                            galleryLauncher.launch("image/*")
                            showArtworkOptions = false
                        },
                        onRemoveCustomArtwork = {
                            viewModel.removeCustomArtwork(song.id)
                            showArtworkOptions = false
                        },
                        onDismiss = { showArtworkOptions = false }
                    )
                }
            }

            // B. CENTERING WRAPPER FOR ALBUM ART
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient Glow behind album art
                    AnimatedContent(
                        targetState = song.artworkUri,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(500))
                        },
                        label = "AuraAnim"
                    ) { targetUri ->
                        val resolvedUri = if (targetUri.isNullOrEmpty() || targetUri.startsWith("content://media/external/audio/albumart/")) {
                            Song.getDeterministicFallbackUrl(song.title, song.artist)
                        } else {
                            targetUri
                        }
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(resolvedUri)
                                .size(150)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize(0.9f)
                                .blur(24.dp)
                                .offset(y = 16.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .alpha(0.65f)
                        )
                    }

                    // Main Glassmorphic Picture frame
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                            .pointerInput(song) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        viewModel.toggleFavorite(song)
                                        heartAnimTrigger++
                                    }
                                )
                            }
                            .border(
                                width = 1.5.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        Color.White.copy(alpha = 0.08f),
                                        CyberCyan.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .background(Color.White.copy(alpha = 0.03f))
                    ) {
                        AnimatedContent(
                            targetState = song.artworkUri,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.95f, animationSpec = tween(600)) togetherWith
                                fadeOut(animationSpec = tween(400))
                            },
                            label = "CoverAnim"
                        ) { targetUri ->
                            val resolvedUri = if (targetUri.isNullOrEmpty() || targetUri.startsWith("content://media/external/audio/albumart/")) {
                                Song.getDeterministicFallbackUrl(song.title, song.artist)
                            } else {
                                targetUri
                            }
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(resolvedUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        // Glass shine gloss overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.15f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                        )

                        // Floating Sparkle Pen Edit Button (Bottom-Right, ultra clean)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(CyberPink, CyberViolet)))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .clickable { showArtworkOptions = true }
                                .testTag("edit_artwork_pen_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Edit Custom Artwork",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Heart Pop-up Layer on double tap
                        if (showHeartAnimation) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Double Tapped Heart Glow Effect",
                                    tint = CyberPink,
                                    modifier = Modifier
                                        .scale(heartScale.value)
                                        .alpha(heartAlpha.value)
                                        .size(90.dp)
                                        .shadow(elevation = 20.dp * heartScale.value, shape = CircleShape, spotColor = CyberPink)
                                )
                            }
                        }
                    }
                }
            }

            // C. SONG INFORMATION SECTION (Left Aligned Title + Right Aligned Heart)
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            val activeAccentColor = artworkDominantColor ?: CyberPink

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge.copy(
                            shadow = Shadow(
                                color = activeAccentColor.copy(alpha = 0.4f),
                                offset = Offset(0f, 0f),
                                blurRadius = 12f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (song.album.isNotEmpty() && song.album != "Unknown") {
                            "${song.artist}  •  ${song.album}"
                        } else {
                            song.artist
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = CyberCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Heart Button on the right
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.toggleFavorite(song)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("now_playing_fav_toggle")
                ) {
                    val heartScaleState = remember { Animatable(1f) }
                    LaunchedEffect(song.isFavorite) {
                        if (song.isFavorite) {
                            heartScaleState.animateTo(1.3f, tween(150, easing = FastOutSlowInEasing))
                            heartScaleState.animateTo(1.0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                    }

                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (song.isFavorite) CyberPink else TextLight.copy(alpha = 0.8f),
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = heartScaleState.value
                                scaleY = heartScaleState.value
                            }
                            .size(30.dp)
                            .testTag("now_playing_favorite_button")
                    )
                }
            }

            // D. PREMIUM FLAT PROGRESS BAR (Apple Music style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 12.dp)
            ) {
                val formattedProgress = Song.formatDuration(position)
                val formattedTotal = Song.formatDuration(duration)

                var isDraggingSeek by remember { mutableStateOf(false) }
                var seekDragProgress by remember { mutableStateOf(0f) }
                val currentProgress = if (isDraggingSeek) seekDragProgress else (if (duration > 0) position.toFloat() / duration else 0f)

                // Smooth scale-in / fade-in for the circular thumb when dragging or interacting
                val thumbScale by animateFloatAsState(
                    targetValue = if (isDraggingSeek) 1f else 0f,
                    animationSpec = tween(150, easing = FastOutSlowInEasing),
                    label = "SeekThumbScale"
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("now_playing_seekbar")
                ) {
                    val barWidthPx = constraints.maxWidth.toFloat()
                    val barHeightDp = 20.dp
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(duration) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        isDraggingSeek = true
                                        val initialProg = (offset.x / barWidthPx).coerceIn(0f, 1f)
                                        seekDragProgress = initialProg
                                        try {
                                            awaitRelease()
                                            viewModel.seekTo((seekDragProgress * duration).toLong())
                                        } finally {
                                            isDraggingSeek = false
                                        }
                                    }
                                )
                            }
                            .pointerInput(duration) {
                                detectDragGestures(
                                    onDragStart = { _ ->
                                        isDraggingSeek = true
                                        seekDragProgress = if (duration > 0) position.toFloat() / duration else 0f
                                    },
                                    onDragEnd = {
                                        viewModel.seekTo((seekDragProgress * duration).toLong())
                                        isDraggingSeek = false
                                    },
                                    onDragCancel = {
                                        isDraggingSeek = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        seekDragProgress = (seekDragProgress + dragAmount.x / barWidthPx).coerceIn(0f, 1f)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeightDp)) {
                            val w = size.width
                            val h = size.height
                            val activeW = currentProgress * w

                            // Premium thin flat horizontal line (4dp to 6dp - we use 4dp)
                            val lineHeight = 4.dp.toPx()
                            val yOffset = (h - lineHeight) / 2

                            // 1. Remaining Portion: White with 25% Opacity, fully rounded
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.25f),
                                topLeft = Offset(0f, yOffset),
                                size = Size(w, lineHeight),
                                cornerRadius = CornerRadius(lineHeight / 2, lineHeight / 2)
                            )

                            // 2. Played Portion: Pure White, fully rounded
                            if (activeW > 0f) {
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(0f, yOffset),
                                    size = Size(activeW, lineHeight),
                                    cornerRadius = CornerRadius(lineHeight / 2, lineHeight / 2)
                                )
                            }

                            // 3. Circular Thumb: Pure White, appears only while interacting/dragging
                            if (thumbScale > 0f) {
                                val thumbRadius = 6.dp.toPx() * thumbScale
                                drawCircle(
                                    color = Color.White,
                                    radius = thumbRadius,
                                    center = Offset(activeW, h / 2)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedProgress,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formattedTotal,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // E. PLAYBACK CONTROLS PANEL (Glassmorphism inspired, beautifully aligned)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.toggleShuffle()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("play_control_shuffle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleMode) CyberCyan else TextDim,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Previous song
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.previousTrend()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("play_control_previous")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Song",
                            tint = TextLight,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // LARGE PLAY/PAUSE GLASSMORPHISM CIRCULAR BUTTON
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(width = 1.5.dp, brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))), shape = CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.playPause()
                            }
                            .testTag("play_control_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient radial background glow inside play button
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.85f)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)))
                        )
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = TextLight,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next Song
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.nextTrend()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("play_control_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Song",
                            tint = TextLight,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Repeat Mode cycle toggle
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.toggleRepeat()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("play_control_repeat")
                    ) {
                        val repeatIcon = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        }
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = "Repeat",
                            tint = if (repeatMode != RepeatMode.NONE) CyberPink else TextDim,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun NowPlayingOptionsDialog(
    song: Song,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showPlaylistAdd by remember { mutableStateOf(false) }
    var showSongDetails by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQueueDialog by remember { mutableStateOf(false) }
    var showShareNotification by remember { mutableStateOf(false) }
    var showRingtoneNotification by remember { mutableStateOf(false) }
    var ringtoneStatusMessage by remember { mutableStateOf("") }
    var showRingtonePermissionDialog by remember { mutableStateOf(false) }
    val enablePermanentDeletion by viewModel.enablePermanentDeletion.collectAsState()

    // Conditional render of child dialogs
    if (showEqualizer) {
        EqualizerDialog(viewModel = viewModel, onDismiss = { showEqualizer = false })
    }
    if (showSleepTimer) {
        SleepTimerDialogDirect(viewModel = viewModel, onDismiss = { showSleepTimer = false })
    }
    if (showPlaylistAdd) {
        AddToPlaylistDialog(song = song, viewModel = viewModel, onDismiss = { showPlaylistAdd = false })
    }
    if (showSongDetails) {
        SongDetailsDialog(song = song, onDismiss = { showSongDetails = false })
    }
    if (showSpeedDialog) {
        PlaybackSpeedDialog(viewModel = viewModel, onDismiss = { showSpeedDialog = false })
    }
    if (showQueueDialog) {
        QueueDialog(viewModel = viewModel, onDismiss = { showQueueDialog = false })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "OPTIONS",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.title,
                    color = TextLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .padding(vertical = 4.dp)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    val optionsList = listOf(
                        "Play Next" to Icons.Default.HourglassTop,
                        "Play Last" to Icons.Default.LowPriority,
                        "Play Similar Songs" to Icons.Default.QueueMusic,
                        "Add To Playlist" to Icons.Default.PlaylistAdd,
                        "Sleep Timer" to Icons.Default.Timer,
                        "Share Song" to Icons.Default.Share,
                        "Set As Ringtone" to Icons.Default.Notifications,
                        "Equalizer" to Icons.Default.Equalizer,
                        "Song Details" to Icons.Default.Info,
                        "Playback Speed" to Icons.Default.Speed,
                        (if (enablePermanentDeletion) "Delete Song" else "Remove From Library") to Icons.Default.Delete
                    )

                    optionsList.forEach { (label, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (label) {
                                        "Play Next" -> {
                                            viewModel.playNext(song)
                                            android.widget.Toast.makeText(context, "Added to play next", android.widget.Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                        "Play Last" -> {
                                            viewModel.playLast(song)
                                            android.widget.Toast.makeText(context, "Queued to play last", android.widget.Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                        "Play Similar Songs" -> {
                                            viewModel.playSimilarSongs(song)
                                            android.widget.Toast.makeText(context, "Queue refreshed with similar tracks", android.widget.Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        }
                                        "Equalizer" -> showEqualizer = true
                                        "Sleep Timer" -> showSleepTimer = true
                                        "Add To Playlist" -> showPlaylistAdd = true
                                        "Song Details" -> showSongDetails = true
                                        "Playback Speed" -> showSpeedDialog = true
                                        "Share Song" -> {
                                            viewModel.shareSong(song) { success, msg ->
                                                if (!success) {
                                                    ringtoneStatusMessage = msg
                                                    showShareNotification = true
                                                }
                                            }
                                        }
                                        "Set As Ringtone" -> {
                                            viewModel.setAsRingtone(song) { success, msg ->
                                                if (!success && msg == "PERMISSION_REQUIRED") {
                                                    showRingtonePermissionDialog = true
                                                } else {
                                                    ringtoneStatusMessage = msg
                                                    showRingtoneNotification = true
                                                }
                                            }
                                        }
                                        "Delete Song" -> {
                                            viewModel.requestDeleteSong(song)
                                            onDismiss()
                                        }
                                        "Remove From Library" -> {
                                            viewModel.requestDeleteSong(song)
                                            onDismiss()
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (label == "Favorite" && song.isFavorite) CyberPink else CyberViolet,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = label,
                                color = TextLight,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyberPink, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF161622)
    )

    // Secondary action triggers
    if (showShareNotification) {
        AlertDialog(
            onDismissRequest = { showShareNotification = false },
            title = { Text("Share Song", color = TextLight, fontWeight = FontWeight.Bold) },
            text = { Text(ringtoneStatusMessage, color = TextDim) },
            confirmButton = {
                TextButton(onClick = { showShareNotification = false }) {
                    Text("OK", color = CyberCyan)
                }
            },
            containerColor = Color(0xFF1E1E2A)
        )
    }

    if (showRingtoneNotification) {
        AlertDialog(
            onDismissRequest = { showRingtoneNotification = false },
            title = { Text("Set as Ringtone", color = TextLight, fontWeight = FontWeight.Bold) },
            text = { Text(ringtoneStatusMessage, color = TextDim) },
            confirmButton = {
                TextButton(onClick = { showRingtoneNotification = false }) {
                    Text("Awesome", color = CyberCyan)
                }
            },
            containerColor = Color(0xFF1E1E2A)
        )
    }

    if (showRingtonePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showRingtonePermissionDialog = false },
            title = { Text("Write Settings Permission", color = TextLight, fontWeight = FontWeight.Bold) },
            text = { Text("Ayvaan needs permission to modify system settings to update your primary device ringtone.\n\nPress 'Grant' below to authorize.", color = TextDim) },
            confirmButton = {
                TextButton(onClick = {
                    showRingtonePermissionDialog = false
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                            data = android.net.Uri.parse("package:" + context.packageName)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }) {
                    Text("Grant", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRingtonePermissionDialog = false }) {
                    Text("Cancel", color = TextDim)
                }
            },
            containerColor = Color(0xFF1E1E2A)
        )
    }
}

@Composable
fun EqualizerDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val enabled by viewModel.equalizerEnabled.collectAsState()
    val bassBoost by viewModel.bassBoost.collectAsState()
    val surroundSound by viewModel.surroundSound.collectAsState()
    val bands by viewModel.equalizerBands.collectAsState()
    val preset by viewModel.equalizerPreset.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Equalizer Engine", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Switch(
                    checked = enabled,
                    onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PitchBlack,
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = TextDim,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                    )
                )
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (enabled) {
                    // Custom Sinusoid Waveform Visualizer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(0xFF0C0D14), RoundedCornerShape(16.dp))
                            .border(1.dp, CyberViolet.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val animTime = rememberInfiniteTransition(label = "EqWave")
                        val phase by animTime.animateFloat(
                            initialValue = 0f,
                            targetValue = 2f * Math.PI.toFloat(),
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = LinearEasing),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                            ),
                            label = "Phase"
                        )

                        val currentCyan = CyberCyan
                        val currentPink = CyberPink
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val points = 50
                            val dx = w / points
                            
                            val path1 = androidx.compose.ui.graphics.Path()
                            val path2 = androidx.compose.ui.graphics.Path()
                            
                            path1.moveTo(0f, h / 2f)
                            path2.moveTo(0f, h / 2f)
                            
                            for (i in 0..points) {
                                val x = i * dx
                                val waveOffset1 = kotlin.math.sin(phase + i * 0.15f) * (h * 0.25f * (bassBoost / 100f))
                                val waveOffset2 = kotlin.math.sin(phase * 1.5f + i * 0.22f) * (h * 0.18f * (surroundSound / 100f))
                                
                                path1.lineTo(x, (h / 2f) + waveOffset1)
                                path2.lineTo(x, (h / 2f) - waveOffset2)
                            }
                            
                            drawPath(
                                path = path1,
                                color = currentCyan,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                            )
                            drawPath(
                                path = path2,
                                color = currentPink,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    // Presets Horizontal list
                    Text("PRESETS", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    val presets = listOf("Cyberpunk", "Bass Booster", "Treble Booster", "Cinematic", "Vocal", "Flat")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { item ->
                            val isSelected = preset == item
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = if (isSelected) {
                                            Brush.linearGradient(colors = listOf(CyberViolet, CyberPink))
                                        } else {
                                            Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.05f)))
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setEqualizerPreset(item) }
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = item,
                                    color = if (isSelected) TextLight else TextDim,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Standard Boost Sliders
                    Text("SOUND ENVIRONMENT", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Bass Boost: ${(bassBoost).toInt()}%", color = TextLight, fontSize = 13.sp)
                        Slider(
                            value = bassBoost,
                            onValueChange = { viewModel.setBassBoost(it) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(activeTrackColor = CyberPink, thumbColor = TextLight)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Surround Sound: ${(surroundSound).toInt()}%", color = TextLight, fontSize = 13.sp)
                        Slider(
                            value = surroundSound,
                            onValueChange = { viewModel.setSurroundSound(it) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(activeTrackColor = CyberCyan, thumbColor = TextLight)
                        )
                    }

                    Text("FREQUENCY BANDS (5-BAND)", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    
                    val frequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
                    frequencies.forEachIndexed { index, freq ->
                        val bandVal = bands.getOrElse(index) { 50f }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(freq, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${bandVal.toInt()} dB", color = CyberCyan, fontSize = 12.sp)
                            }
                            Slider(
                                value = bandVal,
                                onValueChange = { viewModel.setEqualizerBand(index, it) },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(activeTrackColor = CyberViolet, thumbColor = TextLight)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Equalizer Engine Disabled", color = TextDim, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Apply", color = CyberCyan, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF161622)
    )
}

@Composable
fun SleepTimerDialogDirect(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val sleepRemaining by viewModel.sleepTimeRemaining.collectAsState()
    val timerMode by viewModel.sleepTimerMode.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Sleep Timer", color = TextLight, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (sleepRemaining > 0) {
                        val mins = (sleepRemaining / 1000) / 60
                        val secs = (sleepRemaining / 1000) % 60
                        String.format("Active countdown: %02d:%02d remaining", mins, secs)
                    } else if (timerMode == com.example.player.SleepTimerMode.END_OF_SONG) {
                        "Music will stop dynamically when the current song completes."
                    } else if (timerMode == com.example.player.SleepTimerMode.END_OF_PLAYLIST) {
                        "Music will stop dynamically when the active playlist completes."
                    } else {
                        "Set timer duration to automatically suspend active playback."
                    },
                    color = TextDim,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val options = listOf(
                    "Turn Off" to com.example.player.SleepTimerMode.OFF,
                    "10 Minutes" to com.example.player.SleepTimerMode.MINUTES_10,
                    "20 Minutes" to com.example.player.SleepTimerMode.MINUTES_20,
                    "30 Minutes" to com.example.player.SleepTimerMode.MINUTES_30,
                    "60 Minutes" to com.example.player.SleepTimerMode.MINUTES_60,
                    "End of Current Song" to com.example.player.SleepTimerMode.END_OF_SONG,
                    "End of Playlist" to com.example.player.SleepTimerMode.END_OF_PLAYLIST
                )

                options.forEach { (label, modeOption) ->
                    val isSelected = timerMode == modeOption
                    Button(
                        onClick = {
                            viewModel.setSleepTimerMode(modeOption)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) CyberCyan.copy(alpha = 0.2f) else if (modeOption == com.example.player.SleepTimerMode.OFF) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.07f),
                            contentColor = if (isSelected) CyberCyan else TextLight
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("sleep_timer_mode_${modeOption.name}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CyberPink)
            }
        },
        containerColor = Color(0xFF1E1E2A)
    )
}

@Composable
fun AddToPlaylistDialog(
    song: Song,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    var showCreateForm by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Song to Playlist", color = TextLight, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showCreateForm) {
                    TextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedLabelColor = CyberCyan,
                            unfocusedLabelColor = TextDim
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    viewModel.createPlaylist(newPlaylistName)
                                    newPlaylistName = ""
                                    showCreateForm = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                        ) {
                            Text("Create", color = PitchBlack)
                        }
                        TextButton(onClick = { showCreateForm = false }) {
                            Text("Cancel", color = CyberPink)
                        }
                    }
                } else {
                    Button(
                        onClick = { showCreateForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Playlist", tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Playlist", color = TextLight)
                    }

                    if (playlists.isEmpty()) {
                        Text("No playlists created yet. Create one above!", color = TextDim, modifier = Modifier.padding(top = 16.dp))
                    } else {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(scrollState)
                        ) {
                            playlists.forEach { pl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addSongToPlaylist(pl.id, song)
                                            onDismiss()
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlaylistPlay, contentDescription = pl.name, tint = CyberViolet, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(pl.name, color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyberPink)
            }
        },
        containerColor = Color(0xFF1E1E2A)
    )
}

@Composable
fun SongDetailsDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Song Metadata", color = TextLight, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val details = listOf(
                    "Title" to song.title,
                    "Artist" to song.artist,
                    "Album" to song.album,
                    "File Type" to (if (song.filePath.startsWith("virtual_track_")) "FLAC (Dynamic AudioTrack)" else "MP3 File"),
                    "Sample Bitrate" to "320 kbps High-Gloss",
                    "Format Details" to "44.1 kHz, 16-bit PCM Linear Mono",
                    "Device Path" to song.filePath
                )

                details.forEach { (label, value) ->
                    Column {
                        Text(label.uppercase(), color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(value, color = TextLight, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = CyberCyan, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1E1E2A)
    )
}

@Composable
fun PlaybackSpeedDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val currentSpeed by viewModel.playbackSpeed.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed", color = TextLight, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                speeds.forEach { speed ->
                    val isSelected = currentSpeed == speed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSelected) CyberViolet.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.setPlaybackSpeed(speed)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${speed}x Tempo", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Active Speed", tint = CyberCyan)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CyberPink)
            }
        },
        containerColor = Color(0xFF1E1E2A)
    )
}

@Composable
fun QueueDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val queue by viewModel.currentQueue.collectAsState()
    val activeSong by viewModel.currentSong.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Play Queue", color = TextLight, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("TAP A TRACK TO PLAY DIRECTLY", color = CyberCyan, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                
                if (queue.isEmpty()) {
                    Text("No tracks loaded in active queue", color = TextDim)
                } else {
                    Box(modifier = Modifier.heightIn(max = 240.dp)) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            queue.forEach { song ->
                                val isActive = activeSong?.filePath == song.filePath
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isActive) CyberPink.copy(alpha = 0.10f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            viewModel.selectAndPlay(song)
                                            onDismiss()
                                        }
                                        .padding(vertical = 8.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isActive) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                                        contentDescription = song.title,
                                        tint = if (isActive) CyberPink else CyberViolet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = if (isActive) CyberCyan else TextLight,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            color = TextDim,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = CyberCyan)
            }
        },
        containerColor = Color(0xFF1E1E2A)
    )
}

@Composable
fun ArtworkOptionsDialog(
    song: Song,
    onChooseFromGallery: () -> Unit,
    onRemoveCustomArtwork: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CUSTOM ARTWORK",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.title,
                    color = TextLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onChooseFromGallery,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.06f),
                        contentColor = TextLight
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Gallery Icon",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Choose From Gallery", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onRemoveCustomArtwork,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.06f),
                        contentColor = TextLight
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberPink.copy(alpha = 0.3f))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Icon",
                            tint = CyberPink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Remove Custom Artwork", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextDim, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF161622)
    )
}

fun getDeterministicFallbackRequest(song: Song): String {
    return Song.getDeterministicFallbackUrl(song.title, song.artist)
}

// Helper function to shift the hue of a Color perfectly to generate natural complementary colors
fun shiftColorHue(color: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).toInt().coerceIn(0, 255),
        (color.green * 255).toInt().coerceIn(0, 255),
        (color.blue * 255).toInt().coerceIn(0, 255),
        hsv
    )
    hsv[0] = (hsv[0] + degrees) % 360f
    if (hsv[0] < 0) hsv[0] += 360f
    // Maintain highly polished premium music aesthetic ranges (vibrant but readable)
    hsv[1] = (hsv[1] * 1.15f).coerceIn(0.45f, 0.95f)
    hsv[2] = (hsv[2] * 0.95f).coerceIn(0.55f, 0.90f)
    val rgb = android.graphics.Color.HSVToColor(hsv)
    return Color(rgb)
}

@Composable
fun AnimatedDynamicGradientBackground(
    song: Song,
    dominantColor: Color?
) {
    val titleLower = song.title.lowercase()
    
    // Resolve theme colors dynamically based on song genres/keywords or extracted artwork color
    val themeColors = remember(song, dominantColor) {
        val base = dominantColor ?: Color(0xFF8E24AA)
        when {
            // Krishna Songs (Blue + Cyan gradients)
            titleLower.contains("krishna") || titleLower.contains("govinda") || titleLower.contains("radha") || titleLower.contains("gopal") || titleLower.contains("hare") || titleLower.contains("iskcon") -> {
                listOf(Color(0xFF0D47A1), Color(0xFF00E5FF), Color(0xFF006064))
            }
            // Devotional Songs (Gold + Orange gradients)
            titleLower.contains("shiva") || titleLower.contains("hanuman") || titleLower.contains("ram") || titleLower.contains("chalisa") || titleLower.contains("bhakti") || titleLower.contains("devotional") || titleLower.contains("aarti") || titleLower.contains("stotra") -> {
                listOf(Color(0xFFE65100), Color(0xFFFF8F00), Color(0xFFFFD54F))
            }
            // Romantic Songs (Pink + Purple gradients)
            titleLower.contains("romantic") || titleLower.contains("romance") || titleLower.contains("love") || titleLower.contains("dil") || titleLower.contains("prema") || titleLower.contains("pranay") || titleLower.contains("melody") || titleLower.contains("heart") -> {
                listOf(Color(0xFF880E4F), Color(0xFF8E24AA), Color(0xFFEC407A))
            }
            // Pop Songs (Neon Purple + Blue gradients)
            titleLower.contains("pop") || titleLower.contains("chart") || titleLower.contains("top") || titleLower.contains("vibe") || titleLower.contains("retro") || titleLower.contains("disco") || titleLower.contains("party") -> {
                listOf(Color(0xFF9D00FF), Color(0xFF0051FF), Color(0xFF7B1FA2))
            }
            // EDM Songs (Electric Cyan + Magenta gradients)
            titleLower.contains("edm") || titleLower.contains("remix") || titleLower.contains("electronic") || titleLower.contains("synth") || titleLower.contains("trap") || titleLower.contains("bass") || titleLower.contains("electro") -> {
                listOf(Color(0xFF00E5FF), Color(0xFFFF00FF), Color(0xFF0051FF))
            }
            // Pop / Default (Extract dynamically from artwork base color)
            else -> {
                val secondary = shiftColorHue(base, 42f)
                val tertiary = shiftColorHue(base, -42f)
                listOf(base, secondary, tertiary)
            }
        }
    }

    // Create 10–20 seconds slow, highly subtle moving animations for float-shifting depth effects (60 FPS)
    val infiniteTransition = rememberInfiniteTransition(label = "DynamicBgTransition")

    val animX1 by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(17000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "X1"
    )
    val animY1 by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(21000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "Y1"
    )

    val animX2 by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "X2"
    )
    val animY2 by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "Y2"
    )

    val animRadius1 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(13000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "Radius1"
    )

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val width = size.width
        val height = size.height

        // Premium Obsidian Cinematic Footprint Base Layer
        drawRect(color = Color(0xFF030305))

        // Aura Sphere 1 (Vibrant Base color)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(themeColors[0].copy(alpha = 0.44f), Color.Transparent),
                center = Offset(animX1 * width, animY1 * height),
                radius = width * animRadius1
            )
        )

        // Aura Sphere 2 (Second harmonious extracted complementary tone)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(themeColors[1].copy(alpha = 0.38f), Color.Transparent),
                center = Offset(animX2 * width, animY2 * height),
                radius = width * 1.35f
            )
        )

        // Central Ambient Glow (Third extracted mood-lit hue centered for depth structure)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(themeColors[2].copy(alpha = 0.28f), Color.Transparent),
                center = Offset(width * 0.5f, height * 0.45f),
                radius = width * 1.5f
            )
        )
    }
}

