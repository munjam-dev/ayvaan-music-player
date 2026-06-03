package com.example.ui.screens

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import com.example.data.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe persistent states from ViewModel
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColors by viewModel.dynamicColors.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val customAccentHex by viewModel.customAccentHex.collectAsState()
    val crossfade by viewModel.crossfade.collectAsState()
    val gapless by viewModel.gapless.collectAsState()
    val resumeOnLaunch by viewModel.resumeOnLaunch.collectAsState()
    val trebleBoost by viewModel.trebleBoost.collectAsState()
    val loudnessEnhancement by viewModel.loudnessEnhancement.collectAsState()
    val audioBalance by viewModel.audioBalance.collectAsState()
    val autoDownloadArtwork by viewModel.autoDownloadArtwork.collectAsState()
    val showNotificationControls by viewModel.showNotificationControls.collectAsState()
    val lockScreenControls by viewModel.lockScreenControls.collectAsState()
    val ignoreShortAudio by viewModel.ignoreShortAudio.collectAsState()
    val hiddenFolders by viewModel.hiddenFolders.collectAsState()
    val enablePermanentDeletion by viewModel.enablePermanentDeletion.collectAsState()

    // Equalizer & Speed states
    val equalizerPreset by viewModel.equalizerPreset.collectAsState()
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val sleepTimeRemaining by viewModel.sleepTimeRemaining.collectAsState()

    // Real-time app metrics
    val songsList by viewModel.songsWithFavoritesState.collectAsState()
    val albumsList by viewModel.albumsFlow.collectAsState()
    val artistsList by viewModel.artistsFlow.collectAsState()

    // Local state for actions response messaging
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var customHexInput by remember { mutableStateOf(customAccentHex) }
    var showHiddenFoldersDialog by remember { mutableStateOf(false) }

    // Dialog sheets controllers
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showUserAgreement by remember { mutableStateOf(false) }
    var showAboutApp by remember { mutableStateOf(false) }

    // Dismiss message automatically
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            kotlinx.coroutines.delay(4000)
            statusMessage = null
        }
    }

    var showFeedbackScreen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
        // Glowing Futuristic Deck Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CyberViolet.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_ayvan_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DECK SYSTEM",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextLight,
                        letterSpacing = 1.sp,
                        modifier = Modifier.testTag("settings_header_title")
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure your premium audio cybernetic hardware deck.",
                    fontSize = 12.sp,
                    color = CyberCyan,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Status Toast HUD overlay
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(CyberViolet.copy(alpha = 0.85f), CyberPink.copy(alpha = 0.85f))))
                    .padding(14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "HUD log", tint = TextLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusMessage ?: "",
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ========================== SECTION 1: APPEARANCE & THEME MODEL ==========================
        SettingsSectionHeader(title = "Theme & Aesthetics")

        // 1.1 Dynamic colors (Extract from artwork)
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = "Palette", tint = CyberPink, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Dynamic Palette System", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Draw colors natively from album artwork", color = TextDim, fontSize = 11.sp)
                    }
                }
                Switch(
                    checked = dynamicColors,
                    onCheckedChange = { viewModel.setDynamicColors(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PitchBlack,
                        checkedTrackColor = CyberPink,
                        uncheckedThumbColor = TextDim,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                    )
                )
            }
        }

        // 1.3 Preset & Custom Accent Selector
        if (!dynamicColors) {
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ColorLens, contentDescription = "Accent", tint = CyberViolet, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Cyber Accent Preset", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                     Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val accents = listOf("Cyber Accent", "Electric Purple", "Neon Blue", "Cyan Glow", "Pink Glow", "Custom")
                        accents.forEach { acc ->
                            val isSel = acc == accentColor
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) CyberViolet.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, if (isSel) CyberViolet else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setAccentColor(acc) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (acc) {
                                        "Cyber Accent" -> "Cyber"
                                        "Electric Purple" -> "Purple"
                                        "Neon Blue" -> "Blue"
                                        "Cyan Glow" -> "Cyan"
                                        "Pink Glow" -> "Pink"
                                        else -> acc
                                    },
                                    color = if (isSel) CyberCyan else TextDim,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (accentColor == "Custom") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customHexInput,
                            onValueChange = {
                                customHexInput = it
                                if (it.startsWith("#") && (it.length == 7 || it.length == 9)) {
                                    viewModel.setCustomAccentHex(it)
                                }
                            },
                            label = { Text("Custom Accent HEX Format", color = TextDim) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextLight, fontWeight = FontWeight.Bold),
                            placeholder = { Text("#8A2BE2", color = TextDim) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberViolet,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 1.4 Artwork Pool Manager
        SettingsCard {
            val poolImages by viewModel.artworkPoolFiles.collectAsState()
            val pickImagesLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetMultipleContents()
            ) { uris ->
                if (uris.isNotEmpty()) {
                    viewModel.uploadToArtworkPool(uris)
                    statusMessage = "Added ${uris.size} images to custom artwork pool. Auto-distributed to nameless tracks!"
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Artwork Pool",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Artwork Pool Manager",
                                color = TextLight,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Upload multiple images to auto-distribute across tracks",
                                color = TextDim,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = { pickImagesLauncher.launch("image/*") },
                        modifier = Modifier
                            .testTag("upload_artwork_btn")
                            .border(1.dp, CyberCyan, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Upload images",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (poolImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        poolImages.forEachIndexed { index, path ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = path,
                                    contentDescription = "Pool Image $index",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(20.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .clickable {
                                            viewModel.deleteFromArtworkPool(path)
                                            statusMessage = "Image removed from custom pool."
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = CyberPink,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No images in the pool yet. Upload 1 or more to begin round-robin distribution.",
                        color = TextDim,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val customArtworks by viewModel.customArtworks.collectAsState(initial = emptyList())
                val assignedCount = customArtworks.size
                val withoutArtwork = songsList.count { it.artworkUri == null || it.artworkUri?.startsWith("http") == true }

                Spacer(modifier = Modifier.height(16.dp))
                if (poolImages.isNotEmpty()) {
                    Text("Uploaded Images: ${poolImages.size}", color = TextDim, fontSize = 12.sp)
                }
                Text("Songs Assigned: $assignedCount", color = TextDim, fontSize = 12.sp)
                Text("Songs Without Custom Artwork: $withoutArtwork", color = TextDim, fontSize = 12.sp)
                
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.rebuildArtworkAssignments()
                        statusMessage = "Rebuilding artwork assignments..."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Rebuild Artwork Assignments", color = CyberCyan)
                }
                
                var showDiagnostics by remember { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.refreshPosterDiagnostics()
                        showDiagnostics = true 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13131D).copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Poster Diagnostics", color = TextLight, fontSize = 13.sp)
                }

                if (showDiagnostics) {
                    PosterDiagnosticsDialog(viewModel = viewModel, onDismiss = { showDiagnostics = false })
                }
            }
        }

        // ========================== SECTION 2: AUDIO ENGINE ARCHITECTURE ==========================
        SettingsSectionHeader(title = "Audio Engine Hardware Options")

        // 2.1 Hardware Equalizer Preset Selector
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "EQ", tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Hardware Equalizer API", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Direct System DAC custom tuning profiles", color = TextDim, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = equalizerEnabled,
                        onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PitchBlack,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = TextDim,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                        )
                    )
                }

                if (equalizerEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val presets = listOf("Cyberpunk", "Bass Booster", "Treble Booster", "Cinematic", "Vocal", "Flat")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { pr ->
                            val isSel = pr == equalizerPreset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) CyberCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, if (isSel) CyberCyan else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .clickable { viewModel.setEqualizerPreset(pr) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(pr, color = if (isSel) CyberCyan else TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2.2 Time stretching Speed Adjuster
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = "Speed", tint = CyberPink, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Stretching Playback Speed", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Accoustically locked speed slider: ${playbackSpeed}x", color = TextDim, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { sp ->
                        val isSel = sp == playbackSpeed
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) CyberPink.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (isSel) CyberPink else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .clickable { viewModel.setPlaybackSpeed(sp) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${sp}x", color = if (isSel) CyberPink else TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2.3 Sleep count Countdown clock
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Snooze, contentDescription = "Sleep", tint = CyberViolet, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Automatic Sleep Countdown", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            if (sleepTimeRemaining > 0) {
                                val mins = sleepTimeRemaining / 1000 / 60
                                val secs = (sleepTimeRemaining / 1000) % 60
                                Text("Shutting down in " + String.format("%02d:%02d", mins, secs), color = CyberPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Safely stop audio when timers finish", color = TextDim, fontSize = 11.sp)
                            }
                        }
                    }
                    if (sleepTimeRemaining > 0) {
                        IconButton(onClick = { viewModel.setSleepTimer(0) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel timer", tint = CyberPink)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val schedules = listOf("Off" to 0, "15m" to 15, "30m" to 30, "45m" to 45, "60m" to 60)
                    schedules.forEach { (lbl, min) ->
                        val isSel = (sleepTimeRemaining <= 0 && min == 0) || (sleepTimeRemaining > 0 && min > 0 && Math.abs((sleepTimeRemaining/1000/60) - min) <= 1)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) CyberViolet.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (isSel) CyberViolet else Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .clickable { viewModel.setSleepTimer(min) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(lbl, color = if (isSel) CyberViolet else TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2.4 Gapless & Crossfade Tuning Sliders
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PausePresentation, contentDescription = "Gapless", tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("True Gapless Playback", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Pre-loads succeeding audio tracks instantly", color = TextDim, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = gapless,
                        onCheckedChange = { viewModel.setGapless(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PitchBlack,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = TextDim,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AvTimer, contentDescription = "Crossfade", tint = CyberPink, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Automatic Crossfade Seconds", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Duration transition blend: ${crossfade} seconds", color = TextDim, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = crossfade.toFloat(),
                        onValueChange = { viewModel.setCrossfade(it.toInt()) },
                        valueRange = 0f..15f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberPink,
                            activeTrackColor = CyberPink
                        )
                    )
                }
            }
        }

        // 2.5 Loudness enhancer & Audio structural balance
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Hearing, contentDescription = "Loudness", tint = CyberViolet, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Loudness Enhancement API", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Increases output gain limit by +10 dB", color = TextDim, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = loudnessEnhancement,
                        onCheckedChange = { viewModel.setLoudnessEnhancement(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PitchBlack,
                            checkedTrackColor = CyberViolet,
                            uncheckedThumbColor = TextDim,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VolumeMute, contentDescription = "Balance", tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("System Audio Left/Right Balance", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            val balText = when {
                                audioBalance < -0.1f -> "Left Weighted (${String.format("%.1f", -audioBalance)})"
                                audioBalance > 0.1f -> "Right Weighted (${String.format("%.1f", audioBalance)})"
                                else -> "Centered Balanced State"
                            }
                            Text(balText, color = TextDim, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = audioBalance,
                        onValueChange = { viewModel.setAudioBalance(it) },
                        valueRange = -1.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan
                        )
                    )
                }
            }
        }

        // 2.6 Resume on Launch Engine Settings Mode
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = "Resume", tint = CyberPink, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Restore Session On Launch", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Automatically resume last song and track cursor position", color = TextDim, fontSize = 11.sp)
                    }
                }
                Switch(
                    checked = resumeOnLaunch,
                    onCheckedChange = { viewModel.setResumeOnLaunch(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PitchBlack,
                        checkedTrackColor = CyberPink,
                        uncheckedThumbColor = TextDim,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                    )
                )
            }
        }

        // ========================== SECTION 3: MEDIA DECK ARCHITECTURE ==========================
        SettingsSectionHeader(title = "Media Deck Scans & Notifications")

        // 3.1 Background notification switches
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = "Notify", tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Show Foreground Music Panel", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Displays background player status tray controls", color = TextDim, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = showNotificationControls,
                        onCheckedChange = { viewModel.setShowNotificationControls(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PitchBlack,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = TextDim,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = "Lock", tint = CyberPink, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Lock Screen Interactive Controls", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Grant media command visibility on active locks", color = TextDim, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = lockScreenControls,
                        onCheckedChange = { viewModel.setLockScreenControls(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PitchBlack,
                            checkedTrackColor = CyberPink,
                            uncheckedThumbColor = TextDim,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                        )
                    )
                }
            }
        }

        // 3.2 Ignore short audio slider, hidden folders config, rescan database
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Short audio", tint = CyberViolet, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ignore Audio Files Under Size", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Exclude voice notes or sounds below ${ignoreShortAudio}s", color = TextDim, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = ignoreShortAudio.toFloat(),
                        onValueChange = { viewModel.setIgnoreShortAudio(it.toInt()) },
                        valueRange = 0f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberViolet,
                            activeTrackColor = CyberViolet
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHiddenFoldersDialog = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.FolderOff, contentDescription = "Folders", tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Exclude Scanned File Catalogs", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("${hiddenFolders.size} folder tracks currently excluded from list scans", color = TextDim, fontSize = 11.sp)
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open folders dialog", tint = TextDim)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.rescanLibrary()
                        statusMessage = "Media library indices updated from disk scanned songs!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = PitchBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Scan", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Trigger Media scan Refresh Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // ========================== SECTION 3.5: LIBRARY ==========================
        SettingsSectionHeader(title = "Library")

        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Permanent deletion", tint = CyberPink, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Enable Permanent Song Deletion", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Delete actual file from device storage, otherwise only remove from database", color = TextDim, fontSize = 11.sp)
                    }
                }
                Switch(
                    checked = enablePermanentDeletion,
                    onCheckedChange = { viewModel.setEnablePermanentDeletion(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PitchBlack,
                        checkedTrackColor = CyberPink,
                        uncheckedThumbColor = TextDim,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                    )
                )
            }
        }

        // ========================== SECTION 4: DATA MAINTENANCE ==========================
        SettingsSectionHeader(title = "System Storage & Query Maintenance")

        // 4.2 Storage optimization and Cache eviction
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = "Storage", tint = CyberViolet, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Cold Storage Deck Maintenance", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Free up disk space and compress index databases", color = TextDim, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.clearCache { statusMessage = it } },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear App Cache", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.clearArtworkCache { statusMessage = it } },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Art Cache", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { viewModel.optimizeDatabase { statusMessage = it } },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.OfflineBolt, contentDescription = "VACUUM", tint = TextLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VACUUM & Optimize Database Queries", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ========================== SECTION 6: FEEDBACK & SUPPORT ==========================
        SettingsSectionHeader(title = "Feedback & Support")
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = "Support Center Logo",
                        tint = CyberCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Ayvaaan Help Desk",
                        color = TextLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Help us improve Ayvaaan by reporting bugs, requesting features, and sharing suggestions.",
                    color = TextDim,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CONTACT EMAIL",
                            color = TextDim,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "arlo.myn@proton.me",
                            color = CyberPink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = { showFeedbackScreen = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan.copy(alpha = 0.15f),
                            contentColor = CyberCyan
                        ),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "SUBMIT TICKET",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // ========================== SECTION 5: PROTOCOLS & ABOUT DATA ==========================
        SettingsSectionHeader(title = "App Information & License")

        // 5.1 Privacy Policy sheet Link
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacyPolicy = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.PrivacyTip, contentDescription = "Privacy", tint = CyberCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Privacy Policy protocols", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("We respect user database media records and device files", color = TextDim, fontSize = 11.sp)
                    }
                }
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "View Privacy", tint = TextDim, modifier = Modifier.size(16.dp))
            }
        }

        // 5.2 User agreement sheet Link
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showUserAgreement = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Gavel, contentDescription = "License", tint = CyberPink, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("End User License terms", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Agreement regarding playing rights and custom tracks", color = TextDim, fontSize = 11.sp)
                    }
                }
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "View License", tint = TextDim, modifier = Modifier.size(16.dp))
            }
        }

        // 5.3 About Software grid card display
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAboutApp = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Info, contentDescription = "About", tint = CyberViolet, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("About Ayvaan Deck Hardware", color = TextLight, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Real-time telemetry information parameters", color = TextDim, fontSize = 11.sp)
                    }
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "View stats", tint = TextDim)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        val (appVersionName, appVersionCode) = remember(context) {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val code = if (android.os.Build.VERSION.SDK_INT >= 28) {
                    pInfo.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toString()
                }
                Pair(pInfo.versionName ?: "1.4.0", code)
            } catch (e: Exception) {
                Pair("1.4.0", "8408")
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_ayvan_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(48.dp)
                    .alpha(0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Arlo Labs Co. © 2026",
                color = TextLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "♥ Developed by Munjam Dev",
                color = CyberPink,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "App Version $appVersionName • Build $appVersionCode • Database v4",
                color = CyberCyan.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
    }

    // --- DIALOG MODALS DESIGN INTERCONNECTIONS ---

    // Hidden Folders Exclusion Dialog Sheet
    if (showHiddenFoldersDialog) {
        val foldersList = remember(songsList) { songsList.map { it.folder }.distinct().sorted() }
        AlertDialog(
            onDismissRequest = { showHiddenFoldersDialog = false },
            containerColor = CardGrey,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Select Folders to Exclude", color = TextLight, fontWeight = FontWeight.Black)
            },
            text = {
                Column(
                    modifier = Modifier
                        .maxHeightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (foldersList.isEmpty()) {
                        Text("No scanned music folders found yet.", color = TextDim, fontSize = 13.sp)
                    } else {
                        foldersList.forEach { f ->
                            val isHidden = hiddenFolders.contains(f)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleHiddenFolder(f) }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(f, color = TextLight, fontSize = 13.sp, maxLines = 1)
                                Checkbox(
                                    checked = isHidden,
                                    onCheckedChange = { viewModel.toggleHiddenFolder(f) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHiddenFoldersDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = PitchBlack)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // A. Privacy policy sheet
    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            containerColor = CardGrey,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = "Privacy Shield", tint = CyberCyan)
                    Text("Privacy Policy Protocols", color = TextLight, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.maxHeightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Last updated: June 2026\n\n" +
                               "1. Offline Privacy & Policy Statement\n" +
                               "Ayvaan is built by Arlo Labs Co. © 2026 as a 100% offline-first music player. We value your privacy above everything else. Contact us at arlo.myn@proton.me if you have questions.\n\n" +
                               "2. Zero Data Collection Mandate\n" +
                               "Ayvaan does NOT collect, sell, or share user data. We do not track user activity across other apps, compile listening profiles, or share credentials with advertisers. Your usage remains fully secure on your device.\n\n" +
                               "3. Storage Permissions & Data Access\n" +
                               "Ayvaan requests storage permissions (READ_MEDIA_AUDIO on Android 13+ or READ_EXTERNAL_STORAGE on earlier versions) solely to scan, index, and query your offline music files (.mp3, .wav, .flac, etc.). Ayvaan can read music metadata and cache album artwork only to power the Core Music Player functionality.\n\n" +
                               "4. Full User Control\n" +
                               "You possess complete mastery over your data. You can delete playlists, permanently delete songs from the library files, delete custom artwork caches, and clear all local databases. Data remains on your device and is kept only until deleted by the user.\n\n" +
                               "5. Local Security\n" +
                               "All play history, local settings, and customized playlists reside in a local SQLite Room database, securely encapsulated within private app memory on your physical Android terminal.",
                        color = TextDim, fontSize = 13.sp, lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }, colors = ButtonDefaults.textButtonColors(contentColor = CyberCyan)) {
                    Text("Dismiss", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // B. User agreement license sheet
    if (showUserAgreement) {
        AlertDialog(
            onDismissRequest = { showUserAgreement = false },
            containerColor = CardGrey,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(imageVector = Icons.Default.Gavel, contentDescription = "Legal Gavel", tint = CyberPink)
                    Text("User License Agreement", color = TextLight, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.maxHeightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Last updated: June 2026\n\n" +
                               "1. Limited License Grant\n" +
                               "Arlo Labs Co. grants you a limited, non-transferable, revocable license to utilize the Ayvaan application solely for personal, non-commercial offline playback of audio media files organized on your compatible device.\n\n" +
                               "2. Usage Restrictions\n" +
                               "You agree not to: (a) reverse engineer, decompile, or modify any protected components of this application, (b) copy or redistribute the application, or (c) bypass security elements built into the software deck.\n\n" +
                               "3. Copyright & Music Ownership\n" +
                               "Ayvaan does NOT provide or host copyrighted music. All music files, metadata, and licensing are the sole property of their respective creators and owners. Ayvaan only indexes and plays local files stored on the user's device. The user assumes full responsibility for all audio indexed.\n\n" +
                               "4. Intellectual Property\n" +
                               "All trademarks, logos, branding (including the Ayvaan logo design), and source code are the exclusive intellectual property of Arlo Labs Co. No ownership is transferred under this license.\n\n" +
                               "5. Disclaimer & Limitation of Liability\n" +
                               "The application is provided \"As Is\" without warranties of any kind. To the maximum extent permitted by governing laws, Arlo Labs Co. is not liable for device issues, storage corrupted tags, battery wear, or system malfunctions resulting from playback.\n\n" +
                               "6. Termination & Governing Law\n" +
                               "This license terminates automatically if you violate any terms. This agreement is governed by applicable local and federal laws.",
                        color = TextDim, fontSize = 13.sp, lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserAgreement = false }, colors = ButtonDefaults.textButtonColors(contentColor = CyberPink)) {
                    Text("Dismiss", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // C. Telemetry stats and About profile
    if (showAboutApp) {
        val storageUsed = remember(context) { getStorageUsage(context) }
        val packageInfo = remember(context) {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: Exception) {
                null
            }
        }
        val appVersion = packageInfo?.versionName ?: "1.4.0"
        val buildNumber = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString() ?: "28"
        } else {
            packageInfo?.versionCode?.toString() ?: "28"
        }
        val databaseVersion = "4" // Schema version 4 as configured in MusicDatabase.kt

        AlertDialog(
            onDismissRequest = { showAboutApp = false },
            containerColor = CardGrey,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("About Ayvaan", color = TextLight, fontWeight = FontWeight.Black)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AyvanLogo(modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ayvaan Player", color = TextLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("♥ Developed by Munjam Dev.", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Arlo Labs Co. © 2026.", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PitchBlack, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("App Version:", color = TextDim, fontSize = 12.sp)
                            Text(appVersion, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Build Number:", color = TextDim, fontSize = 12.sp)
                            Text(buildNumber, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Database Version:", color = TextDim, fontSize = 12.sp)
                            Text(databaseVersion, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Tracks:", color = TextDim, fontSize = 12.sp)
                            Text("${songsList.size} tracks", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Storage Occupied:", color = TextDim, fontSize = 12.sp)
                            Text(storageUsed, color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutApp = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet, contentColor = TextLight)
                ) {
                    Text("Dismiss stats", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // --- SUPPORT & FEEDBACK COMPOSABLE SCREEN WRAPPER ---
    AnimatedVisibility(
        visible = showFeedbackScreen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        var fbName by remember { mutableStateOf("") }
        var fbEmail by remember { mutableStateOf("") }
        var fbDevice by remember { mutableStateOf("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}") }
        var fbAndroid by remember { mutableStateOf("Android ${android.os.Build.VERSION.RELEASE}") }
        var fbAppVersion by remember { mutableStateOf("v1.0.0") }
        var fbCategory by remember { mutableStateOf("Bug Report") }
        var fbSubject by remember { mutableStateOf("") }
        var fbMessage by remember { mutableStateOf("") }
        var fbScreenshotUri by remember { mutableStateOf<Uri?>(null) }
        var fbIncludeDiagnostics by remember { mutableStateOf(false) }

        var fbError by remember { mutableStateOf<String?>(null) }
        
        // Multi-phase flow states: 0 = Form, 1 = Preview Ticket, 2 = Success, 3 = Fallback Error
        var screenPhase by remember { mutableIntStateOf(0) }
        var activeTicketId by remember { mutableStateOf("") }

        val buildEmailBody = {
            val timestampStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            "━━━━━━━━━━━━━━━━━━━━\n\n" +
            "🎵 AYVAAAN SUPPORT TICKET\n\n" +
            "Ticket ID:\n" +
            "$activeTicketId\n\n" +
            "Date:\n" +
            "$timestampStr\n\n" +
            "━━━━━━━━━━━━━━━━━━━━\n\n" +
            "USER DETAILS\n\n" +
            "Name:\n" +
            "${fbName.trim()}\n\n" +
            "Email:\n" +
            "${fbEmail.trim()}\n\n" +
            "━━━━━━━━━━━━━━━━━━━━\n\n" +
            "DEVICE INFORMATION\n\n" +
            "Device:\n" +
            "${fbDevice.trim()}\n\n" +
            "Android:\n" +
            "${fbAndroid.trim()}\n\n" +
            "App Version:\n" +
            "${fbAppVersion}\n\n" +
            "━━━━━━━━━━━━━━━━━━━━\n\n" +
            "CATEGORY\n\n" +
            "$fbCategory\n\n" +
            "━━━━━━━━━━━━━━━━━━━━\n\n" +
            "SUBJECT\n\n" +
            "${fbSubject.trim()}\n\n" +
            "━━━━━━━━━━━━━━━━━━━━\n\n" +
            "MESSAGE\n\n" +
            "${fbMessage.trim()}\n\n" +
            "━━━━━━━━━━━━━━━━━━━━\n\n" +
            "Generated by Ayvaaan Music Player\n\n" +
            "Arlo Labs Co. © 2026\n\n" +
            "━━━━━━━━━━━━━━━━━━━━"
        }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                fbScreenshotUri = uri
            }
        }

        val categoriesList = listOf(
            "Bug Report" to Icons.Default.BugReport,
            "Feature Request" to Icons.Default.Bolt,
            "Performance Issue" to Icons.Default.Speed,
            "UI/UX Suggestion" to Icons.Default.Palette,
            "Playlist Issue" to Icons.Default.QueueMusic,
            "Artwork Issue" to Icons.Default.Image,
            "Playback Issue" to Icons.Default.PlayArrow,
            "General Feedback" to Icons.Default.Chat
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack),
            color = PitchBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Top Custom Help Center Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = "Support Setup",
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SUPPORT DESK",
                            color = TextLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    }
                    IconButton(
                        onClick = { 
                            // Reset state and exit support deck
                            fbName = ""
                            fbEmail = ""
                            fbSubject = ""
                            fbMessage = ""
                            fbScreenshotUri = null
                            fbIncludeDiagnostics = false
                            activeTicketId = ""
                            fbError = null
                            screenPhase = 0
                            showFeedbackScreen = false
                        },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                when (screenPhase) {
                    0 -> {
                        // PHASE 0: EXPANDED DETAILED SUPPORT FORM SCREEN
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(top = 20.dp, bottom = 120.dp)
                        ) {
                            // Direct Support & Call Copy Segment
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.015f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.SupportAgent, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Direct Help Center", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Have questions or need manual assistance? Copy our support email below:",
                                        color = TextDim,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val contextLocal = LocalContext.current
                                    var showCopiedText by remember { mutableStateOf(false) }
                                    Button(
                                        onClick = {
                                            val clipboard = contextLocal.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Support Email", "arlo.myn@proton.me")
                                            clipboard.setPrimaryClip(clip)
                                            showCopiedText = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = TextLight),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(11.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (showCopiedText) "COPIED!" else "COPY EMAIL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Privacy Notice Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberCyan.copy(alpha = 0.02f)),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Outlined.PrivacyTip, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Privacy Notice: Feedback is sent only through your selected email application. Ayvaaan does not upload feedback to external servers.",
                                        color = TextDim,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text("CREATE A SUPPORT TICKET", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Form Error Row
                            if (fbError != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF331114)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(fbError ?: "", color = TextLight, fontSize = 13.sp)
                                    }
                                }
                            }

                            // 1. Full Name
                            Text("FULL NAME *", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = fbName,
                                onValueChange = { fbName = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("feedback_name_input"),
                                placeholder = { Text("Enter your full name", color = TextDim.copy(alpha = 0.5f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.02f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 2. Email Address
                            Text("EMAIL ADDRESS *", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = fbEmail,
                                onValueChange = { fbEmail = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("feedback_email_input"),
                                placeholder = { Text("Enter your email address", color = TextDim.copy(alpha = 0.5f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.02f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 3. Category Chip Selection Flow
                            Text("FEEDBACK CATEGORY *", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val splitRows = categoriesList.chunked(2)
                                splitRows.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { (catName, catIcon) ->
                                            val isSelected = fbCategory == catName
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSelected) CyberCyan.copy(alpha = 0.12f)
                                                        else Color.White.copy(alpha = 0.02f)
                                                    )
                                                    .border(
                                                        width = 1.2.dp,
                                                        color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.08f),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { fbCategory = catName }
                                                    .padding(horizontal = 10.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = catIcon,
                                                        contentDescription = null,
                                                        tint = if (isSelected) CyberCyan else TextDim.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = catName,
                                                        color = if (isSelected) TextLight else TextDim,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 4. Subject
                            Text("SUBJECT *", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = fbSubject,
                                onValueChange = { if (it.length <= 100) fbSubject = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("feedback_subject_input"),
                                placeholder = { Text("Brief high-level summary (max 100 chars)", color = TextDim.copy(alpha = 0.5f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.02f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 5. Message
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text("FEEDBACK MESSAGE *", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(
                                    text = "${fbMessage.length}/2000 chars",
                                    color = if (fbMessage.length in 20..2000) CyberCyan else TextDim,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = fbMessage,
                                onValueChange = { if (it.length <= 2000) fbMessage = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .testTag("feedback_message_input"),
                                placeholder = { Text("What happened? Min 20, max 2000 characters.", color = TextDim.copy(alpha = 0.5f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.02f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                ),
                                maxLines = 10,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 6. Device Model
                            Text("DEVICE MODEL", color = CyberPink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = fbDevice,
                                onValueChange = { fbDevice = it },
                                modifier = Modifier.fillMaxWidth().testTag("feedback_device_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight,
                                    focusedBorderColor = CyberPink,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.02f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // 7 & 8. Android & App version row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ANDROID VERSION", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = fbAndroid,
                                        onValueChange = { fbAndroid = it },
                                        modifier = Modifier.fillMaxWidth().testTag("feedback_android_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextLight,
                                            unfocusedTextColor = TextLight,
                                            focusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                            focusedContainerColor = Color.White.copy(alpha = 0.02f),
                                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("APP VERSION", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = fbAppVersion,
                                        onValueChange = {},
                                        enabled = false,
                                        modifier = Modifier.fillMaxWidth().testTag("feedback_app_version_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextDim,
                                            unfocusedTextColor = TextDim,
                                            disabledTextColor = TextDim,
                                            focusedBorderColor = Color.White.copy(alpha = 0.1f),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                            disabledBorderColor = Color.White.copy(alpha = 0.1f),
                                            focusedContainerColor = Color.White.copy(alpha = 0.02f),
                                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                                        ),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Optional Screen capture picker
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Screenshot", tint = CyberPink, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Screenshot Attachment", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                Text("JPG, PNG, or WEBP (Optional)", color = TextDim, fontSize = 11.sp)
                                            }
                                        }
                                        if (fbScreenshotUri == null) {
                                            TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                                Text("SELECT", color = CyberPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        } else {
                                            TextButton(onClick = { fbScreenshotUri = null }) {
                                                Text("REMOVE", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    if (fbScreenshotUri != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Image, contentDescription = "Image picked", tint = CyberCyan, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = fbScreenshotUri?.lastPathSegment ?: "screenshot_attached.png",
                                                color = TextLight,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Diagnostic stats toggling
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.015f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.DeveloperMode, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Include Technical Hardware Metrics", color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Device stats & memory allocations specs", color = TextDim, fontSize = 11.sp)
                                        }
                                    }
                                    Switch(
                                        checked = fbIncludeDiagnostics,
                                        onCheckedChange = { fbIncludeDiagnostics = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PitchBlack,
                                            checkedTrackColor = CyberViolet,
                                            uncheckedThumbColor = TextDim,
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Submit Button
                            val contextLocal = LocalContext.current
                            Button(
                                onClick = {
                                    fbError = null
                                    val checkedName = fbName.trim()
                                    val checkedEmail = fbEmail.trim()
                                    val checkedSubject = fbSubject.trim()
                                    val checkedMessage = fbMessage.trim()

                                    if (checkedName.length < 3) {
                                        fbError = "Full Name must be at least 3 characters."
                                    } else if (checkedEmail.isBlank()) {
                                        fbError = "Email Address is required."
                                    } else if (!checkedEmail.contains("@") || !checkedEmail.contains(".")) {
                                        fbError = "Please enter a valid email format."
                                    } else if (checkedSubject.isBlank()) {
                                        fbError = "Subject is required."
                                    } else if (checkedSubject.length > 100) {
                                        fbError = "Subject cannot exceed 100 characters."
                                    } else if (checkedMessage.length < 20) {
                                        fbError = "Feedback message must be at least 20 characters."
                                    } else if (checkedMessage.length > 2000) {
                                        fbError = "Feedback message cannot exceed 2000 characters."
                                    } else {
                                        // Initialize / update ticket ID
                                        if (activeTicketId.isBlank()) {
                                            val prefs = contextLocal.getSharedPreferences("ayvaaan_support", Context.MODE_PRIVATE)
                                            val count = prefs.getInt("ticket_id_counter", 1001)
                                            activeTicketId = "AYV-$count"
                                            prefs.edit().putInt("ticket_id_counter", count + 1).apply()
                                        }
                                        screenPhase = 1 // Proceed to Preview Receipt Screen!
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = PitchBlack),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("submit_feedback_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SUBMIT TICKET DETAILS", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                    }

                    1 -> {
                        // PHASE 1: EMAIL TICKET PREVIEW RECEIPT SCREEN
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(top = 20.dp, bottom = 120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "VERIFY TICKET SPECIFICATION",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Cybernetic ticket visual docket
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C14)),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("🎵 AYVAAAN SUPPORT TICKET", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(CyberPink.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("PRIORITY: NORMAL", color = CyberPink, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Param layout metrics
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Ticket ID:", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(activeTicketId, color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Category:", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(fbCategory, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Subject:", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(fbSubject, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Submitted By:", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(fbName, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text("Feedback Preview Message", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(fbMessage, color = TextLight, fontSize = 12.sp, lineHeight = 17.sp)
                                    }

                                    if (fbScreenshotUri != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = CyberPink, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Screenshot Attachment Loaded", color = CyberPink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "This ticket will be sent directly to:",
                                        color = TextDim,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "arlo.myn@proton.me",
                                        color = CyberCyan,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Send Deck Trigger Keys
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { screenPhase = 0 },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
                                ) {
                                    Text("EDIT TICKET", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                val contextLocal = LocalContext.current
                                Button(
                                    onClick = {
                                        // Generate and Format specified Email Body Template
                                        val timestampStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                                        val screenshotPresent = if (fbScreenshotUri != null) "YES" else "NO"
                                        val generatedBody = "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                                "🎵 AYVAAAN SUPPORT TICKET\n" +
                                                "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                                "Ticket ID:\n" +
                                                "$activeTicketId\n\n" +
                                                "Date:\n" +
                                                "$timestampStr\n" +
                                                "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                                "USER DETAILS\n" +
                                                "Name:\n" +
                                                "${fbName.trim()}\n\n" +
                                                "Email:\n" +
                                                "${fbEmail.trim()}\n" +
                                                "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                                "DEVICE INFORMATION\n" +
                                                "Device:\n" +
                                                "${fbDevice.trim()}\n\n" +
                                                "Android:\n" +
                                                "${fbAndroid.trim()}\n\n" +
                                                "App Version:\n" +
                                                "$fbAppVersion\n" +
                                                "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                                "CATEGORY\n" +
                                                "$fbCategory\n" +
                                                "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                                "SUBJECT\n" +
                                                "${fbSubject.trim()}\n" +
                                                "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                                "MESSAGE\n" +
                                                "${fbMessage.trim()}\n" +
                                                "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                                "ATTACHMENTS\n" +
                                                "Screenshot Attached:\n" +
                                                "$screenshotPresent\n" +
                                                "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                                "Generated by Ayvaaan Music Player\n" +
                                                "Arlo Labs Co. © 2026\n" +
                                                "Support:\n" +
                                                "arlo.myn@proton.me\n" +
                                                "━━━━━━━━━━━━━━━━━━━━━━━━━━"

                                        val finalBody = buildEmailBody()
                                        val emailSubjectStr = "[AYVAAAN SUPPORT] Ticket #$activeTicketId | $fbCategory | ${fbSubject.trim()}"

                                        try {
                                            val intent = if (fbScreenshotUri != null) {
                                                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "image/*"
                                                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("arlo.myn@proton.me"))
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, emailSubjectStr)
                                                    putExtra(android.content.Intent.EXTRA_TEXT, finalBody)
                                                    putExtra(android.content.Intent.EXTRA_STREAM, fbScreenshotUri)
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                     selector = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                                         data = Uri.parse("mailto:")
                                                     }
                                                }
                                            } else {
                                                android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("mailto:")
                                                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("arlo.myn@proton.me"))
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, emailSubjectStr)
                                                    putExtra(android.content.Intent.EXTRA_TEXT, finalBody)
                                                }
                                            }

                                            contextLocal.startActivity(android.content.Intent.createChooser(intent, "Send Ticket via"))
                                            screenPhase = 2 // Move to Success State Screen
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            screenPhase = 3 // Call Mail client error redirect fallback
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = PitchBlack),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(50.dp)
                                        .testTag("send_ticket_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SEND TICKET", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // PHASE 2: TICKET GENERATED SUCCESS FLOW RECEIPT
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(top = 40.dp, bottom = 120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(CyberCyan.copy(alpha = 0.1f), CircleShape)
                                    .border(2.dp, CyberCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Success",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(44.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Support Ticket Prepared",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextLight,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ticket ID: $activeTicketId",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Support ticket prepared successfully. Please press Send in your mail application to deliver the ticket.",
                                color = TextDim,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 16.dp).testTag("support_prepared_text")
                            )

                            Spacer(modifier = Modifier.height(40.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        // Complete action, clear state variables, closes launcher support
                                        fbName = ""
                                        fbEmail = ""
                                        fbSubject = ""
                                        fbMessage = ""
                                        fbScreenshotUri = null
                                        fbIncludeDiagnostics = false
                                        activeTicketId = ""
                                        fbError = null
                                        screenPhase = 0
                                        showFeedbackScreen = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = PitchBlack),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Text("DONE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        // Edit ticket goes back to Phase 0 editor form
                                        screenPhase = 0
                                    },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
                                ) {
                                    Text("EDIT TICKET", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }

                    3 -> {
                        // PHASE 3: MAIL APPLICATION FALLBACK COMPONENT
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(top = 40.dp, bottom = 120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFF331114), CircleShape)
                                    .border(2.dp, Color(0xFFEF4444), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MailOutline,
                                    contentDescription = "Alert",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "No email application found on this device.",
                                color = TextLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Please copy our support address and your generated ticket details to send them using your webmail provider.",
                                color = TextDim,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            val clipboardLocal = LocalContext.current
                            var addressCopied by remember { mutableStateOf(false) }
                            var detailsCopied by remember { mutableStateOf(false) }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val clipboard = clipboardLocal.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Support Email", "arlo.myn@proton.me")
                                        clipboard.setPrimaryClip(clip)
                                        addressCopied = true
                                        detailsCopied = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = PitchBlack),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("copy_email_address_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (addressCopied) "EMAIL ADDRESS COPIED!" else "Copy Email Address", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        val clipboard = clipboardLocal.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Ticket Details", buildEmailBody())
                                        clipboard.setPrimaryClip(clip)
                                        detailsCopied = true
                                        addressCopied = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberPink, contentColor = PitchBlack),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("copy_ticket_details_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (detailsCopied) "TICKET DETAILS COPIED!" else "Copy Ticket Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(30.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { screenPhase = 0 },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(50.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
                                ) {
                                    Text("BACK TO FORM", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        // Reset state and exit support deck
                                        fbName = ""
                                        fbEmail = ""
                                        fbSubject = ""
                                        fbMessage = ""
                                        fbScreenshotUri = null
                                        fbIncludeDiagnostics = false
                                        activeTicketId = ""
                                        fbError = null
                                        screenPhase = 0
                                        showFeedbackScreen = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = TextLight),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                ) {
                                    Text("CLOSE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// Helper functions for dynamic exact Storage metrics computing
fun getStorageUsage(context: Context): String {
    try {
        var bytes = 0L
        bytes += getDirSize(context.filesDir)
        bytes += getDirSize(context.cacheDir)
        context.externalCacheDir?.let { bytes += getDirSize(it) }
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format("%.2f MB", mb)
    } catch (e: Exception) {
        return "0.00 MB"
    }
}

fun getDirSize(dir: java.io.File): Long {
    var size = 0L
    val files = dir.listFiles() ?: return 0L
    for (f in files) {
        if (f.isDirectory) {
            size += getDirSize(f)
        } else {
            size += f.length()
        }
    }
    return size
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextDim,
        letterSpacing = 1.5.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 22.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13131D)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        content()
    }
}

fun Modifier.maxHeightIn(max: androidx.compose.ui.unit.Dp): Modifier {
    return this.heightIn(max = max)
}

@Composable
fun PosterDiagnosticsDialog(
    viewModel: com.example.ui.viewmodel.MusicViewModel,
    onDismiss: () -> Unit
) {
    val stats by viewModel.diagnosticsStats.collectAsState()
    val logs by viewModel.diagnosticsLogs.collectAsState()

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F14))
                .padding(top = 40.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF13131D))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Poster Diagnostics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextLight)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Status Card
                        SettingsCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Artwork Statistics", color = CyberCyan, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Built-In Posters Loaded: ${stats.postersLoaded}", color = TextLight, fontSize = 13.sp)
                                Text("Poster Categories: ${stats.categories}", color = TextDim, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Songs Assigned: ${stats.songsAssigned}", color = TextLight, fontSize = 13.sp)
                                Text("Albums Assigned: ${stats.albumsAssigned}", color = TextDim, fontSize = 13.sp)
                                Text("Artists Assigned: ${stats.artistsAssigned}", color = TextDim, fontSize = 13.sp)
                                Text("Playlists Assigned: ${stats.playlistsAssigned}", color = TextDim, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Unassigned Songs: ${stats.unassignedSongs}", color = if (stats.unassignedSongs > 0) androidx.compose.ui.graphics.Color(0xFFFFB74D) else TextDim, fontSize = 13.sp)
                                Text("Cache Size: ${stats.cacheSize}", color = TextDim, fontSize = 13.sp)
                                Text("Last Assignment Run: ${stats.lastAssignmentRun}", color = TextDim, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Action Buttons
                        SettingsCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Actions", color = CyberCyan, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        viewModel.logDiagnostic("Manually triggered rebuild...")
                                        viewModel.rebuildArtworkAssignments()
                                        viewModel.refreshPosterDiagnostics()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Rebuild All Assignments", color = CyberCyan)
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        viewModel.logDiagnostic("Clearing artwork cache...")
                                        viewModel.clearArtworkCache { msg ->
                                            viewModel.logDiagnostic(msg)
                                            viewModel.refreshPosterDiagnostics()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13131D)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Clear Artwork Cache", color = TextLight)
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.logDiagnostic("Validating database...")
                                        viewModel.assignArtworkToSongs() // Force verify/assign missing
                                        viewModel.refreshPosterDiagnostics()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13131D)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Validate Poster Database", color = TextLight)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Logs area
                        SettingsCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Diagnostics Logs", color = CyberCyan, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .background(Color(0xFF0F0F14).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFF1E1E24), RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    if (logs.isEmpty()) {
                                        Text("No logs available.", color = TextDim, fontSize = 11.sp)
                                    } else {
                                        val revLogs = logs.reversed()
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            items(revLogs.size) { i ->
                                                Text(revLogs[i], color = TextLight, fontSize = 10.sp, 
                                                     fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                     modifier = Modifier.padding(bottom = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
