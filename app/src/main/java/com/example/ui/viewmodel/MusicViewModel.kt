package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.data.db.PlayHistoryEntity
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistSongEntity
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import com.example.player.MusicPlayerManager
import com.example.player.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    val playerManager = MusicPlayerManager.getInstance(application)

    // Profiles Picture persistence
    private val sharedPrefs = application.getSharedPreferences("ayvaan_prefs", Context.MODE_PRIVATE)
    private val _profilePicUri = MutableStateFlow<String?>(sharedPrefs.getString("profile_pic_uri", null))
    val profilePicUri: StateFlow<String?> = _profilePicUri.asStateFlow()

    fun updateProfilePicUri(uriString: String?) {
        _profilePicUri.value = uriString
        sharedPrefs.edit().putString("profile_pic_uri", uriString).apply()
    }

    // Artwork Pool Persistence & Operations
    private val _artworkPoolFiles = MutableStateFlow<List<String>>(emptyList())
    val artworkPoolFiles: StateFlow<List<String>> = _artworkPoolFiles.asStateFlow()

    fun loadArtworkPool() {
        val poolDir = File(getApplication<Application>().filesDir, "artwork_pool")
        val images = poolDir.listFiles()?.map { it.absolutePath }?.sorted() ?: emptyList()
        _artworkPoolFiles.value = images
    }

    fun uploadToArtworkPool(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val poolDir = File(context.filesDir, "artwork_pool")
                if (!poolDir.exists()) {
                    poolDir.mkdirs()
                }
                
                uris.forEach { uri ->
                    var originalName = "uploaded_art"
                    if (uri.scheme == "content") {
                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                        try {
                            if (cursor != null && cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    val name = cursor.getString(nameIndex)
                                    if (!name.isNullOrBlank()) {
                                        originalName = name
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // ignore
                        } finally {
                            cursor?.close()
                        }
                    } else {
                        val path = uri.path
                        if (path != null) {
                            originalName = path.substringAfterLast("/")
                        }
                    }
                    val nameWithoutExt = originalName.substringBeforeLast(".")
                    val sanitized = nameWithoutExt.replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()

                    val inputStream = context.contentResolver.openInputStream(uri)
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (originalBitmap != null) {
                        val width = originalBitmap.width
                        val height = originalBitmap.height
                        val squareSize = minOf(width, height)
                        val x = (width - squareSize) / 2
                        val y = (height - squareSize) / 2
                        val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, squareSize, squareSize)

                        val timestamp = System.currentTimeMillis()
                        val randomSuffix = (1000..9999).random()
                        val destFile = File(poolDir, "${sanitized}_${timestamp}_${randomSuffix}.png")
                        val outStream = java.io.FileOutputStream(destFile)
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 90, outStream)
                        outStream.flush()
                        outStream.close()
                    }
                }
                loadArtworkPool()
                // Yield to allow state flow to emit new pool files
                kotlinx.coroutines.delay(100)
                assignArtworkToSongs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteFromArtworkPool(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
                loadArtworkPool()
                // Auto-refresh the current song's playing artwork
                refreshPlayerArtworkFromNewSchema()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // System Settings Setup
    private val settingsPrefs = application.getSharedPreferences("ayvaan_settings_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        try { settingsPrefs.getString("theme_mode", "Dark Mode") ?: "Dark Mode" } catch (e: Exception) { "Dark Mode" }
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColors = MutableStateFlow(
        try { settingsPrefs.getBoolean("dynamic_colors", false) } catch (e: Exception) { false }
    )
    val dynamicColors: StateFlow<Boolean> = _dynamicColors.asStateFlow()

    private val _accentColor = MutableStateFlow(
        try { settingsPrefs.getString("accent_color", "Cyber Accent") ?: "Cyber Accent" } catch (e: Exception) { "Cyber Accent" }
    )
    val accentColor: StateFlow<String> = _accentColor.asStateFlow()

    private val _customAccentHex = MutableStateFlow(
        try { settingsPrefs.getString("custom_accent_hex", "#8A2BE2") ?: "#8A2BE2" } catch (e: Exception) { "#8A2BE2" }
    )
    val customAccentHex: StateFlow<String> = _customAccentHex.asStateFlow()

    private val _crossfade = MutableStateFlow(try { settingsPrefs.getInt("crossfade_sec", 0) } catch (e: Exception) { 0 })
    val crossfade: StateFlow<Int> = _crossfade.asStateFlow()

    private val _gapless = MutableStateFlow(try { settingsPrefs.getBoolean("gapless_playback", false) } catch (e: Exception) { false })
    val gapless: StateFlow<Boolean> = _gapless.asStateFlow()

    private val _resumeOnLaunch = MutableStateFlow(try { settingsPrefs.getBoolean("resume_on_launch", false) } catch (e: Exception) { false })
    val resumeOnLaunch: StateFlow<Boolean> = _resumeOnLaunch.asStateFlow()

    private val _trebleBoost = MutableStateFlow(try { settingsPrefs.getFloat("treble_boost", 50f) } catch (e: Exception) { 50f })
    val trebleBoost: StateFlow<Float> = _trebleBoost.asStateFlow()

    private val _loudnessEnhancement = MutableStateFlow(try { settingsPrefs.getBoolean("loudness_enhancement", false) } catch (e: Exception) { false })
    val loudnessEnhancement: StateFlow<Boolean> = _loudnessEnhancement.asStateFlow()

    private val _audioBalance = MutableStateFlow(try { settingsPrefs.getFloat("audio_balance", 0f) } catch (e: Exception) { 0f })
    val audioBalance: StateFlow<Float> = _audioBalance.asStateFlow()

    private val _autoDownloadArtwork = MutableStateFlow(try { settingsPrefs.getBoolean("auto_download_artwork", false) } catch (e: Exception) { false })
    val autoDownloadArtwork: StateFlow<Boolean> = _autoDownloadArtwork.asStateFlow()

    private val _showNotificationControls = MutableStateFlow(try { settingsPrefs.getBoolean("show_notification_controls", true) } catch (e: Exception) { true })
    val showNotificationControls: StateFlow<Boolean> = _showNotificationControls.asStateFlow()

    private val _lockScreenControls = MutableStateFlow(try { settingsPrefs.getBoolean("lock_screen_controls", true) } catch (e: Exception) { true })
    val lockScreenControls: StateFlow<Boolean> = _lockScreenControls.asStateFlow()

    private val _ignoreShortAudio = MutableStateFlow(try { settingsPrefs.getInt("ignore_short_audio_sec", 10) } catch (e: Exception) { 10 })
    val ignoreShortAudio: StateFlow<Int> = _ignoreShortAudio.asStateFlow()

    private val _hiddenFolders = MutableStateFlow(try { settingsPrefs.getStringSet("hidden_folders", emptySet()) ?: emptySet() } catch (e: Exception) { emptySet() })
    val hiddenFolders: StateFlow<Set<String>> = _hiddenFolders.asStateFlow()

    // --- DELETION STATE MANAGEMENT ---
    private val _enablePermanentDeletion = MutableStateFlow(
        try { settingsPrefs.getBoolean("enable_permanent_deletion", true) } catch (e: Exception) { true }
    )
    val enablePermanentDeletion: StateFlow<Boolean> = _enablePermanentDeletion.asStateFlow()

    fun setEnablePermanentDeletion(enabled: Boolean) {
        _enablePermanentDeletion.value = enabled
        settingsPrefs.edit().putBoolean("enable_permanent_deletion", enabled).apply()
    }

    private val _activeDeleteRequestSong = MutableStateFlow<Song?>(null)
    val activeDeleteRequestSong: StateFlow<Song?> = _activeDeleteRequestSong.asStateFlow()

    fun requestDeleteSong(song: Song) {
        _activeDeleteRequestSong.value = song
    }

    fun dismissDeleteRequest() {
        _activeDeleteRequestSong.value = null
    }

    data class UndoDeleteState(
        val song: Song,
        val isFavorite: Boolean,
        val playlistAssignments: List<PlaylistSongEntity>,
        val backupFile: File?,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _pendingUndoState = MutableStateFlow<UndoDeleteState?>(null)
    val pendingUndoState: StateFlow<UndoDeleteState?> = _pendingUndoState.asStateFlow()

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            val permanent = _enablePermanentDeletion.value
            var backupFile: File? = null

            if (permanent) {
                // Rename physical file to a temporary backup
                try {
                    val originalFile = File(song.filePath)
                    if (originalFile.exists()) {
                        val tempBackup = File(getApplication<Application>().cacheDir, originalFile.name + ".bak")
                        if (tempBackup.exists()) tempBackup.delete()
                        if (originalFile.renameTo(tempBackup)) {
                            backupFile = tempBackup
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Delete linked LRC file if it exists
                try {
                    val origLrc = File(song.filePath.substringBeforeLast(".") + ".lrc")
                    if (origLrc.exists()) {
                        val tempLrcBackup = File(getApplication<Application>().cacheDir, origLrc.name + ".bak")
                        if (tempLrcBackup.exists()) tempLrcBackup.delete()
                        origLrc.renameTo(tempLrcBackup)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Record DB relationships to prepare for possible Undo
            val favoritesStateValue = favoritePaths.value.contains(song.filePath)
            
            // Get all playlist assignments
            val playlistSongsStatic = repository.getPlaylistSongsForSong(song.filePath)

            // Database cleanups
            repository.removeSongFromDatabase(song.filePath, song.id)

            // Remove from repository in-memory state so library refreshes instantly
            repository.deleteSongFromScannedList(song)

            // Handle Now Playing Protection:
            // If currently playing song is deleted
            val curSong = playerManager.currentSong.value
            if (curSong != null && curSong.id == song.id) {
                playerManager.handleDeletedSong(song)
            } else {
                // Just remove from player queue if it's cached in queue
                playerManager.removeSongFromQueue(song)
            }

            // Setup Undo state
            val undoState = UndoDeleteState(
                song = song,
                isFavorite = favoritesStateValue,
                playlistAssignments = playlistSongsStatic,
                backupFile = backupFile,
                timestamp = System.currentTimeMillis()
            )
            _pendingUndoState.value = undoState

            // Wait 5 seconds, then finalize deletion if Undo isn't clicked
            delay(5000)
            if (_pendingUndoState.value == undoState) {
                // Finalize physical file deletion
                if (permanent && backupFile != null && backupFile.exists()) {
                    backupFile.delete()
                }
                // Also finalize linked lrc backup deletion
                try {
                    val tempLrcBackup = File(getApplication<Application>().cacheDir, File(song.filePath).nameWithoutExtension + ".lrc.bak")
                    if (tempLrcBackup.exists()) {
                        tempLrcBackup.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                _pendingUndoState.value = null
            }
        }
    }

    fun performUndoDelete() {
        val undoState = _pendingUndoState.value ?: return
        _pendingUndoState.value = null

        viewModelScope.launch {
            // Restore physical file (if permanent)
            if (_enablePermanentDeletion.value && undoState.backupFile != null && undoState.backupFile.exists()) {
                try {
                    val originalFile = File(undoState.song.filePath)
                    undoState.backupFile.renameTo(originalFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Restore linked LRC file if backup exists
                try {
                    val tempLrcBackup = File(getApplication<Application>().cacheDir, File(undoState.song.filePath).nameWithoutExtension + ".lrc.bak")
                    if (tempLrcBackup.exists()) {
                        val originalLrc = File(undoState.song.filePath.substringBeforeLast(".") + ".lrc")
                        tempLrcBackup.renameTo(originalLrc)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Restore DB relationships
            if (undoState.isFavorite) {
                repository.toggleFavorite(undoState.song.filePath)
            }

            // Restore queue / playlists
            undoState.playlistAssignments.forEach { playlistSong ->
                repository.insertPlaylistSong(playlistSong)
            }

            // Restore to repository's in-memory list
            repository.addSongToScannedList(undoState.song)
        }
    }

    // Dynamic brand color derived from artwork
    private val _artworkDominantColor = MutableStateFlow<Color?>(null)
    val artworkDominantColor: StateFlow<Color?> = _artworkDominantColor.asStateFlow()

    // Thread-safe Search Cache to prevent full list scans during typing
    private val searchCache = java.util.concurrent.ConcurrentHashMap<String, List<Song>>()
    private var lastBaseSongsRef: List<Song>? = null

    // Indexed Search Query with fast debouncing
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    val debouncedSearchQuery: Flow<String> = _searchQuery
        .debounce(300)
        .distinctUntilChanged()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Scanned songs list
    val allSongs: StateFlow<List<Song>> = repository.allScannedSongs

    // Favorites Path List
    val favoritePaths: StateFlow<List<String>> = repository.favoritePathsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Flow of Custom Artworks mappings from DB
    val customArtworks: Flow<List<com.example.data.db.CustomArtworkEntity>> = repository.customArtworksFlow

    // Redesigned: Map song Favorite, Custom Artwork, Assigned Poster states dynamically from new tables
    val songsWithFavoritesState: StateFlow<List<Song>> = combine(
        allSongs,
        favoritePaths,
        repository.customArtworksFlow,
        repository.artworkPoolFlow,
        repository.songArtworkMapsFlow
    ) { songs, favPaths, customArtworks, pool, artworkMaps ->
        val customMap = customArtworks.associate { it.songId to it.artworkPath }
        val poolMap = pool.associate { it.artworkId to it.artworkPath }
        val assignmentMap = artworkMaps.associate { it.songId to it }
        
        songs.map { song ->
            // 1. Check User Custom Artwork First
            var resolvedArt = customMap[song.id]
            
            if (resolvedArt.isNullOrEmpty()) {
                val assigned = assignmentMap[song.id]
                if (assigned != null) {
                    if (assigned.sourceType == "embedded") {
                        resolvedArt = song.artworkUri
                    } else {
                        resolvedArt = poolMap[assigned.artworkId]
                    }
                }
                
                // If it's still null, fallback to deterministic pool images or fallback URL
                if (resolvedArt.isNullOrEmpty()) {
                    if (pool.isNotEmpty()) {
                        val bestPoster = selectBestArtworkFromPool(song, pool)
                        resolvedArt = bestPoster?.artworkPath
                    }
                    if (resolvedArt.isNullOrEmpty()) {
                        resolvedArt = Song.getDeterministicFallbackUrl(song.title, song.artist)
                    }
                }
            }
            
            song.copy(
                isFavorite = favPaths.contains(song.filePath),
                artworkUri = resolvedArt
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectBestArtworkFromPool(song: Song, pool: List<com.example.data.db.ArtworkPoolEntity>): com.example.data.db.ArtworkPoolEntity? {
        if (pool.isEmpty()) return null
        
        val genre = Song.detectSongGenre(song).lowercase()
        // Try to match the poster category first
        val matchingCategoryPosters = pool.filter { it.artworkCategory.lowercase() == genre }
        
        if (matchingCategoryPosters.isNotEmpty()) {
            val index = (song.id.hashCode() and Int.MAX_VALUE) % matchingCategoryPosters.size
            return matchingCategoryPosters[index]
        }
        
        val index = (song.id.hashCode() and Int.MAX_VALUE) % pool.size
        return pool[index]
    }

    private suspend fun syncArtworkPoolFromAssets(): List<com.example.data.db.ArtworkPoolEntity> {
        var existingPool = repository.getAllArtworkPoolStatic()
        
        val assetManager = getApplication<Application>().assets
        val postersDir = "posters"
        var bundledPosters = emptyArray<String>()
        try {
            bundledPosters = assetManager.list(postersDir) ?: emptyArray()
        } catch (e: Exception) {
            android.util.Log.e("AyvaanArtwork", "Failed to list assets/posters", e)
        }

        val bundledPostersSet = bundledPosters.map { "file:///android_asset/$postersDir/$it" }.toSet()
        val staleAssetsFound = existingPool.any { 
            it.artworkPath.startsWith("file:///android_asset/$postersDir/") && !bundledPostersSet.contains(it.artworkPath) 
        }

        val hasUnsplash = existingPool.any { it.artworkPath.contains("unsplash.com") }
        if (hasUnsplash || staleAssetsFound) {
            repository.clearArtworkPool()
            repository.deleteAllSongArtworkMaps()
            existingPool = emptyList()
            android.util.Log.d("AyvaanArtwork", "Cleared stale or fallback assets from directory and database.")
        }
        
        val existingPaths = existingPool.map { it.artworkPath }.toSet()
        
        val newPosters = mutableListOf<com.example.data.db.ArtworkPoolEntity>()
        var validFilesFound = 0

        if (bundledPosters.isNotEmpty()) {
            bundledPosters.forEachIndexed { index, fileName ->
                if (fileName.endsWith(".png", ignoreCase = true) || 
                    fileName.endsWith(".jpg", ignoreCase = true) ||
                    fileName.endsWith(".jpeg", ignoreCase = true) ||
                    fileName.endsWith(".webp", ignoreCase = true)) {
                    
                    android.util.Log.d("AyvaanArtwork", "Poster Found: $fileName")
                    validFilesFound++
                    
                    val path = "file:///android_asset/$postersDir/$fileName"
                    if (!existingPaths.contains(path)) {
                        val category = when {
                            fileName.contains("devotional", ignoreCase = true) || fileName.contains("krishna", ignoreCase = true) -> "Devotional"
                            fileName.contains("synth", ignoreCase = true) -> "Synth & Electronic"
                            fileName.contains("rock", ignoreCase = true) -> "Rock"
                            fileName.contains("pop", ignoreCase = true) -> "Pop"
                            fileName.contains("hindi", ignoreCase = true) -> "Hindi"
                            fileName.contains("telugu", ignoreCase = true) -> "Telugu"
                            fileName.contains("tamil", ignoreCase = true) -> "Tamil"
                            fileName.contains("classical", ignoreCase = true) -> "Classical"
                            fileName.contains("folk", ignoreCase = true) -> "Folk"
                            fileName.contains("romantic", ignoreCase = true) -> "Romantic"
                            else -> "Pop" // Default fallback category
                        }
                        newPosters.add(
                            com.example.data.db.ArtworkPoolEntity(
                                artworkPath = path,
                                artworkCategory = category,
                                artworkPriority = existingPool.size + validFilesFound
                            )
                        )
                    }
                }
            }
        } 
        
        if (validFilesFound == 0 && existingPool.isEmpty()) {
            android.util.Log.d("AyvaanArtwork", "No bundled posters found. Falling back to Unsplash URLs.")
            newPosters.addAll(listOf(
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1545128485-c400e7702796?w=600&auto=format&fit=crop&q=80", artworkCategory = "Devotional", artworkPriority = 1),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1609137144813-7dbe24888126?w=600&auto=format&fit=crop&q=80", artworkCategory = "Devotional", artworkPriority = 2),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1561571994-3c61c554181a?w=600&auto=format&fit=crop&q=80", artworkCategory = "Devotional", artworkPriority = 3),
                        
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80", artworkCategory = "Synth & Electronic", artworkPriority = 1),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1614741118887-7a4ee193a5fa?w=600&auto=format&fit=crop&q=80", artworkCategory = "Synth & Electronic", artworkPriority = 2),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80", artworkCategory = "Synth & Electronic", artworkPriority = 3),
                        
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80", artworkCategory = "Rock", artworkPriority = 1),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?w=600&auto=format&fit=crop&q=80", artworkCategory = "Rock", artworkPriority = 2),
                        
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80", artworkCategory = "Pop", artworkPriority = 1),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80", artworkCategory = "Pop", artworkPriority = 2),
                        
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80", artworkCategory = "Hindi", artworkPriority = 1),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80", artworkCategory = "Hindi", artworkPriority = 2),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80", artworkCategory = "Hindi", artworkPriority = 3),
                        
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80", artworkCategory = "Telugu", artworkPriority = 1),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1511735111819-9a3f7709049c?w=600&auto=format&fit=crop&q=80", artworkCategory = "Telugu", artworkPriority = 2),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80", artworkCategory = "Tamil", artworkPriority = 1),
                        
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1552422535-c45813c61732?w=600&auto=format&fit=crop&q=80", artworkCategory = "Classical", artworkPriority = 1),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&auto=format&fit=crop&q=80", artworkCategory = "Folk", artworkPriority = 1),
                        
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=600&auto=format&fit=crop&q=80", artworkCategory = "English", artworkPriority = 1),
                        com.example.data.db.ArtworkPoolEntity(artworkPath = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=600&auto=format&fit=crop&q=80", artworkCategory = "English", artworkPriority = 2)
            ))
        }

        if (newPosters.isNotEmpty()) {
            repository.saveArtworkPoolItems(newPosters)
            android.util.Log.d("AyvaanArtwork", "Populated initial poster pool into DB.")
        }
        
        val fullPool = repository.getAllArtworkPoolStatic()
        android.util.Log.d("AyvaanArtwork", "Total Posters Loaded: ${fullPool.size}")
        return fullPool
    }

    fun seedArtworkPoolIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                syncArtworkPoolFromAssets()
                
                // Triggers artwork assignment after pool is ready
                assignArtworkToSongs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun assignArtworkToSongs(songsList: List<Song>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val pool = repository.getAllArtworkPoolStatic()
            if (pool.isEmpty()) {
                android.util.Log.e("AyvaanArtwork", "Built-in Posters Loaded: 0 - System Failure")
                return@launch
            } else {
                android.util.Log.d("AyvaanArtwork", "Built-in Posters Loaded: ${pool.size}")
            }
            val songs = songsList ?: repository.allScannedSongs.value
            val existingAssignments = repository.getAllSongArtworkMapsStatic().associateBy { it.songId }
            
            val toSave = mutableListOf<com.example.data.db.SongArtworkMapEntity>()
            val resolver = getApplication<Application>().contentResolver
            
            var assignedCount = 0
            songs.forEach { song ->
                val assigned = existingAssignments[song.id]
                if (assigned != null && (assigned.sourceType == "embedded" || assigned.artworkId != -1)) return@forEach
                
                var hasRealEmbedded = false
                if (!song.filePath.startsWith("virtual_track_") && !song.artworkUri.isNullOrEmpty() && song.artworkUri.startsWith("content://")) {
                    try {
                        val uri = android.net.Uri.parse(song.artworkUri)
                        val fd = resolver.openAssetFileDescriptor(uri, "r")
                        if (fd != null) {
                            hasRealEmbedded = true
                            fd.close()
                        }
                    } catch (e: Exception) {
                    }
                }
                
                if (hasRealEmbedded) {
                    toSave.add(com.example.data.db.SongArtworkMapEntity(
                        songId = song.id,
                        artworkId = -1,
                        sourceType = "embedded",
                        assignedAt = System.currentTimeMillis()
                    ))
                    assignedCount++
                    android.util.Log.d("AyvaanArtwork", "Assigned Embedded Artwork to song: ${song.title}")
                } else {
                    val bestPoster = selectBestArtworkFromPool(song, pool)
                    if (bestPoster != null) {
                        toSave.add(com.example.data.db.SongArtworkMapEntity(
                            songId = song.id,
                            artworkId = bestPoster.artworkId,
                            sourceType = "defaultPoster",
                            assignedAt = System.currentTimeMillis()
                        ))
                        assignedCount++
                        android.util.Log.d("AyvaanArtwork", "Assigned Built-in Poster to song: ${song.title}")
                    }
                }
            }
            
            if (toSave.isNotEmpty()) {
                repository.saveSongArtworkMaps(toSave)
                android.util.Log.d("AyvaanArtwork", "Saved $assignedCount new artwork assignments to database.")
            }
            
            val totalAssigned = existingAssignments.size + assignedCount
            val unassigned = songs.size - totalAssigned
            if (unassigned >= 0) {
                android.util.Log.d("AyvaanArtwork", "Songs Assigned: $totalAssigned")
                android.util.Log.d("AyvaanArtwork", "Unassigned Songs: $unassigned")
                android.util.Log.d("AyvaanArtwork", "Health Score: ${if (unassigned == 0) "100%" else "${(totalAssigned.toFloat() / songs.size * 100).toInt()}%"}")
            }

            refreshPlayerArtworkFromNewSchema()
        }
    }

    fun rebuildArtworkAssignments() {
        viewModelScope.launch(Dispatchers.IO) {
            val pool = syncArtworkPoolFromAssets()
            if (pool.isEmpty()) {
                android.util.Log.e("AyvaanArtwork", "Built-in Posters Loaded: 0 - System Failure")
                return@launch
            } else {
                android.util.Log.d("AyvaanArtwork", "Built-in Posters Loaded: ${pool.size}")
            }
            val songs = repository.allScannedSongs.value
            
            repository.deleteAllSongArtworkMaps()
            
            val toSave = mutableListOf<com.example.data.db.SongArtworkMapEntity>()
            val resolver = getApplication<Application>().contentResolver
            
            var assignedCount = 0
            songs.forEach { song ->
                var hasRealEmbedded = false
                if (!song.filePath.startsWith("virtual_track_") && !song.artworkUri.isNullOrEmpty() && song.artworkUri.startsWith("content://")) {
                    try {
                        val uri = android.net.Uri.parse(song.artworkUri)
                        val fd = resolver.openAssetFileDescriptor(uri, "r")
                        if (fd != null) {
                            hasRealEmbedded = true
                            fd.close()
                        }
                    } catch (e: Exception) {
                    }
                }
                
                if (hasRealEmbedded) {
                    toSave.add(com.example.data.db.SongArtworkMapEntity(
                        songId = song.id,
                        artworkId = -1,
                        sourceType = "embedded",
                        assignedAt = System.currentTimeMillis()
                    ))
                    assignedCount++
                } else {
                    val bestPoster = selectBestArtworkFromPool(song, pool)
                    if (bestPoster != null) {
                        toSave.add(com.example.data.db.SongArtworkMapEntity(
                            songId = song.id,
                            artworkId = bestPoster.artworkId,
                            sourceType = "defaultPoster",
                            assignedAt = System.currentTimeMillis()
                        ))
                        assignedCount++
                    }
                }
            }
            
            if (toSave.isNotEmpty()) {
                repository.saveSongArtworkMaps(toSave)
                android.util.Log.d("AyvaanArtwork", "Rebuilt assignments, total saved: $assignedCount")
            }
            
            android.util.Log.d("AyvaanArtwork", "Songs Assigned: $assignedCount")
            val unassigned = songs.size - assignedCount
            android.util.Log.d("AyvaanArtwork", "Unassigned Songs: $unassigned")
            android.util.Log.d("AyvaanArtwork", "Health Score: ${if (unassigned == 0) "100%" else "${(assignedCount.toFloat() / songs.size * 100).toInt()}%"}")
            
            refreshPlayerArtworkFromNewSchema()
        }
    }

    private suspend fun refreshPlayerArtworkFromNewSchema() {
        val current = playerManager.currentSong.value
        if (current != null) {
            val pool = repository.getAllArtworkPoolStatic()
            val artworkMaps = repository.getAllSongArtworkMapsStatic()
            val customArtworks = repository.customArtworksFlow.firstOrNull() ?: emptyList()
            
            val customMap = customArtworks.associate { it.songId to it.artworkPath }
            val poolMap = pool.associate { it.artworkId to it.artworkPath }
            val assignmentMap = artworkMaps.associate { it.songId to it }
            
            var resolvedArt = customMap[current.id]
            if (resolvedArt.isNullOrEmpty()) {
                val assigned = assignmentMap[current.id]
                if (assigned != null) {
                    if (assigned.sourceType == "embedded") {
                        resolvedArt = current.artworkUri
                    } else {
                        resolvedArt = poolMap[assigned.artworkId]
                    }
                }
                if (resolvedArt.isNullOrEmpty()) {
                    if (pool.isNotEmpty()) {
                        val bestPoster = selectBestArtworkFromPool(current, pool)
                        resolvedArt = bestPoster?.artworkPath
                    }
                    if (resolvedArt.isNullOrEmpty()) {
                        resolvedArt = Song.getDeterministicFallbackUrl(current.title, current.artist)
                    }
                }
            }
            playerManager.updateSongArtworkInPlayer(current.id, resolvedArt)
        }
    }

    // Filtered songs incorporating 300ms debounced query and optimized caching
    val filteredSongs: StateFlow<List<Song>> = combine(
        songsWithFavoritesState,
        debouncedSearchQuery
    ) { songs, query ->
        if (songs !== lastBaseSongsRef) {
            searchCache.clear()
            lastBaseSongsRef = songs
        }
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            songs
        } else {
            searchCache.getOrPut(trimmed) {
                songs.filter {
                    it.title.contains(trimmed, ignoreCase = true) ||
                    it.artist.contains(trimmed, ignoreCase = true) ||
                    it.album.contains(trimmed, ignoreCase = true)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player states exposed to Compose
    private val _isNowPlayingVisible = MutableStateFlow(false)
    val isNowPlayingVisible: StateFlow<Boolean> = _isNowPlayingVisible.asStateFlow()

    fun setNowPlayingVisible(visible: Boolean) {
        _isNowPlayingVisible.value = visible
    }

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val playbackPosition: StateFlow<Long> = playerManager.playbackPosition
    val playbackDuration: StateFlow<Long> = playerManager.playbackDuration
    val shuffleMode: StateFlow<Boolean> = playerManager.shuffleMode
    val repeatMode: StateFlow<RepeatMode> = playerManager.repeatMode
    val currentQueue: StateFlow<List<Song>> = playerManager.currentQueue
    val visualizerAmplitudes: StateFlow<FloatArray> = playerManager.visualizerAmplitudes
    val sleepTimeRemaining: StateFlow<Long> = playerManager.sleepTimeRemaining

    // Playback state of current song including favorite and custom artwork check
    val currentPlayingSongWithFavorite: StateFlow<Song?> = combine(
        currentSong,
        songsWithFavoritesState
    ) { song, stateSongs ->
        song?.let { s ->
            stateSongs.find { it.id == s.id } ?: s
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Playlists & History
    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlistsWithSongs: StateFlow<Map<Int, List<Song>>> = combine(
        repository.playlistsFlow,
        repository.allPlaylistSongsFlow,
        allSongs
    ) { playlistsList, relationList, songList ->
        playlistsList.associate { playlist ->
            val matchingRelationPaths = relationList
                .filter { it.playlistId == playlist.id }
                .map { it.songPath }
            val matchedSongs = matchingRelationPaths.mapNotNull { path ->
                songList.find { it.filePath == path }
            }
            playlist.id to matchedSongs
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _activeAddToPlaylistSong = MutableStateFlow<Song?>(null)
    val activeAddToPlaylistSong: StateFlow<Song?> = _activeAddToPlaylistSong.asStateFlow()

    fun showAddToPlaylistDialog(song: Song) {
        _activeAddToPlaylistSong.value = song
    }

    fun dismissAddToPlaylistDialog() {
        _activeAddToPlaylistSong.value = null
    }

    val recentHistory: StateFlow<List<PlayHistoryEntity>> = repository.playHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Permission state mapping
    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    fun updatePermissionStatus(granted: Boolean) {
        _permissionGranted.value = granted
        if (granted) {
            viewModelScope.launch {
                repository.scanSongs()
            }
        }
    }

    // Custom filtering tabs details
    val albumsFlow: StateFlow<List<String>> = songsWithFavoritesState.map { list ->
        list.map { it.album }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artistsFlow: StateFlow<List<String>> = songsWithFavoritesState.map { list ->
        list.map { it.artist }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foldersFlow: StateFlow<List<String>> = songsWithFavoritesState.map { list ->
        list.map { it.folder }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedSongs: StateFlow<List<Song>> = combine(
        songsWithFavoritesState,
        recentHistory
    ) { songs, history ->
        val historyPaths = history.sortedByDescending { it.timestamp }.map { it.songPath }.distinct()
        val songsMap = songs.associateBy { it.filePath }
        historyPaths.mapNotNull { songsMap[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val newlyAddedSongs: StateFlow<List<Song>> = songsWithFavoritesState.map { list ->
        list.sortedByDescending { it.dateAdded }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedSongs: StateFlow<List<Song>> = combine(
        songsWithFavoritesState,
        repository.mostPlayedFlow
    ) { songs, mostPlayedItems ->
        val mostPlayedMap = mostPlayedItems.associate { it.songPath to it.playCount }
        songs.filter { s -> mostPlayedMap.containsKey(s.filePath) }
            .map { s -> s.copy(playCount = mostPlayedMap[s.filePath] ?: 0) }
            .sortedByDescending { it.playCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = songsWithFavoritesState.map { list ->
        list.filter { it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Equalizer States
    private val _equalizerEnabled = MutableStateFlow(try { settingsPrefs.getBoolean("equalizer_enabled", true) } catch (e: Exception) { true })
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _bassBoost = MutableStateFlow(try { settingsPrefs.getFloat("bass_boost", 80f) } catch (e: Exception) { 80f })
    val bassBoost: StateFlow<Float> = _bassBoost.asStateFlow()

    private val _surroundSound = MutableStateFlow(try { settingsPrefs.getFloat("surround_sound", 65f) } catch (e: Exception) { 65f })
    val surroundSound: StateFlow<Float> = _surroundSound.asStateFlow()

    private val _equalizerBands = MutableStateFlow(floatArrayOf(
        try { settingsPrefs.getFloat("equalizer_band_0", 80f) } catch (e: Exception) { 80f },
        try { settingsPrefs.getFloat("equalizer_band_1", 65f) } catch (e: Exception) { 65f },
        try { settingsPrefs.getFloat("equalizer_band_2", 75f) } catch (e: Exception) { 75f },
        try { settingsPrefs.getFloat("equalizer_band_3", 85f) } catch (e: Exception) { 85f },
        try { settingsPrefs.getFloat("equalizer_band_4", 90f) } catch (e: Exception) { 90f }
    ))
    val equalizerBands: StateFlow<FloatArray> = _equalizerBands.asStateFlow()

    private val _equalizerPreset = MutableStateFlow(try { settingsPrefs.getString("equalizer_preset", "Cyberpunk") ?: "Cyberpunk" } catch (e: Exception) { "Cyberpunk" })
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()

    // Playback control wrappers
    fun selectAndPlay(song: Song) {
        playerManager.setQueue(allSongs.value)
        playerManager.playSong(song)
        _isNowPlayingVisible.value = true
    }

    fun selectAndPlayQueue(song: Song, queue: List<Song>) {
        playerManager.setQueue(queue)
        playerManager.playSong(song)
        _isNowPlayingVisible.value = true
    }

    fun playNext(song: Song) {
        playerManager.playNext(song)
    }

    fun playLast(song: Song) {
        playerManager.playLast(song)
    }

    fun playSimilarSongs(song: Song) {
        playerManager.playSimilarSongs(song, allSongs.value)
        _isNowPlayingVisible.value = true
    }

    fun playPause() {
        playerManager.togglePlayPause()
    }

    fun nextTrend() {
        playerManager.next()
    }

    fun previousTrend() {
        playerManager.previous()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.setSleepTimer(minutes)
    }

    val sleepTimerMode: StateFlow<com.example.player.SleepTimerMode> = playerManager.sleepTimerMode

    fun setSleepTimerMode(mode: com.example.player.SleepTimerMode) {
        playerManager.setSleepTimerMode(mode)
    }

    // Playback Speed State
    private val _playbackSpeed = MutableStateFlow(try { settingsPrefs.getFloat("playback_speed", 1.0f) } catch (e: Exception) { 1.0f })
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    init {
        loadArtworkPool()
        seedArtworkPoolIfEmpty()
        // Apply player manager settings
        playerManager.setGapless(_gapless.value)
        playerManager.setCrossfade(_crossfade.value)

        viewModelScope.launch {
            allSongs.collectLatest { songsList ->
                if (songsList.isNotEmpty()) {
                    assignArtworkToSongs(songsList)
                }
            }
        }

        viewModelScope.launch {
            playerManager.currentSong.collectLatest { song ->
                if (song != null) {
                    extractDominantColorFromArtwork(song)
                    if (_resumeOnLaunch.value) {
                        settingsPrefs.edit().putString("last_song_id", song.id).apply()
                    }
                }
            }
        }

        // Resume previous session on startup
        viewModelScope.launch {
            combine(allSongs, _resumeOnLaunch) { songsList, resume ->
                if (songsList.isNotEmpty()) {
                    if (resume && playerManager.currentSong.value == null) {
                        val lastSongId = try { settingsPrefs.getString("last_song_id", null) } catch (e: Exception) { null }
                        if (lastSongId != null) {
                            val lastSong = songsList.find { it.id == lastSongId }
                            if (lastSong != null) {
                                playerManager.playSong(lastSong)
                                playerManager.pause()
                                val lastPos = try { settingsPrefs.getLong("last_position", 0L) } catch (e: Exception) { 0L }
                                playerManager.seekTo(lastPos)
                            }
                        }
                    }
                    true // Scan completed (either fallback or real songs loaded), stop observing
                } else {
                    false // List still empty, continue observing
                }
            }.filter { it }.first()
        }

        // Store playback position periodically
        viewModelScope.launch {
            while (true) {
                delay(3000)
                if (playerManager.isPlaying.value && _resumeOnLaunch.value) {
                    settingsPrefs.edit().putLong("last_position", playerManager.playbackPosition.value).apply()
                }
            }
        }
    }

    private fun extractDominantColorFromArtwork(song: Song) {
        if (!_dynamicColors.value) {
            _artworkDominantColor.value = null
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val uriStr = song.artworkUri
                var bitmap: Bitmap? = null
                if (!uriStr.isNullOrEmpty()) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(Uri.parse(uriStr))
                        bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                    } catch (e: Exception) {
                        val loader = ImageLoader(context)
                        val request = ImageRequest.Builder(context)
                            .data(uriStr)
                            .allowHardware(false)
                            .build()
                        val result = loader.execute(request)
                        bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    }
                }

                if (bitmap == null) {
                    val fallback = Song.getDeterministicFallbackUrl(song.title, song.artist)
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(fallback)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                }

                if (bitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, 12, 12, false)
                    var r = 0L
                    var g = 0L
                    var b = 0L
                    val count = scaled.width * scaled.height
                    for (x in 0 until scaled.width) {
                        for (y in 0 until scaled.height) {
                            val color = scaled.getPixel(x, y)
                            r += (color shr 16) and 0xFF
                            g += (color shr 8) and 0xFF
                            b += color and 0xFF
                        }
                    }
                    scaled.recycle()
                    val col = Color(
                        red = (r / count).toInt().coerceIn(0, 255),
                        green = (g / count).toInt().coerceIn(0, 255),
                        blue = (b / count).toInt().coerceIn(0, 255)
                    )
                    withContext(Dispatchers.Main) {
                        _artworkDominantColor.value = col
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        settingsPrefs.edit().putString("theme_mode", mode).apply()
    }

    fun setDynamicColors(enabled: Boolean) {
        _dynamicColors.value = enabled
        settingsPrefs.edit().putBoolean("dynamic_colors", enabled).apply()
        val song = playerManager.currentSong.value
        if (song != null) {
            extractDominantColorFromArtwork(song)
        } else {
            _artworkDominantColor.value = null
        }
    }

    fun setAccentColor(colorStr: String) {
        _accentColor.value = colorStr
        settingsPrefs.edit().putString("accent_color", colorStr).apply()
    }

    fun setCustomAccentHex(hexStr: String) {
        _customAccentHex.value = hexStr
        settingsPrefs.edit().putString("custom_accent_hex", hexStr).apply()
    }

    fun setCrossfade(seconds: Int) {
        _crossfade.value = seconds
        settingsPrefs.edit().putInt("crossfade_sec", seconds).apply()
        playerManager.setCrossfade(seconds)
    }

    fun setGapless(enabled: Boolean) {
        _gapless.value = enabled
        settingsPrefs.edit().putBoolean("gapless_playback", enabled).apply()
        playerManager.setGapless(enabled)
    }

    fun setResumeOnLaunch(enabled: Boolean) {
        _resumeOnLaunch.value = enabled
        settingsPrefs.edit().putBoolean("resume_on_launch", enabled).apply()
    }

    fun setTrebleBoost(value: Float) {
        _trebleBoost.value = value
        settingsPrefs.edit().putFloat("treble_boost", value).apply()
        playerManager.applyPreferencesDirectly()
    }

    fun setLoudnessEnhancement(enabled: Boolean) {
        _loudnessEnhancement.value = enabled
        settingsPrefs.edit().putBoolean("loudness_enhancement", enabled).apply()
        playerManager.applyPreferencesDirectly()
    }

    fun setAudioBalance(value: Float) {
        _audioBalance.value = value
        settingsPrefs.edit().putFloat("audio_balance", value).apply()
        playerManager.applyBalanceDirectly()
    }

    fun setAutoDownloadArtwork(enabled: Boolean) {
        _autoDownloadArtwork.value = enabled
        settingsPrefs.edit().putBoolean("auto_download_artwork", enabled).apply()
    }

    fun setShowNotificationControls(enabled: Boolean) {
        _showNotificationControls.value = enabled
        settingsPrefs.edit().putBoolean("show_notification_controls", enabled).apply()
        playerManager.notifyNotificationSettingsChanged()
    }

    fun setLockScreenControls(enabled: Boolean) {
        _lockScreenControls.value = enabled
        settingsPrefs.edit().putBoolean("lock_screen_controls", enabled).apply()
        playerManager.notifyNotificationSettingsChanged()
    }

    fun setIgnoreShortAudio(seconds: Int) {
        _ignoreShortAudio.value = seconds
        settingsPrefs.edit().putInt("ignore_short_audio_sec", seconds).apply()
        viewModelScope.launch {
            repository.scanSongs()
        }
    }

    fun toggleHiddenFolder(folder: String) {
        val currentSet = _hiddenFolders.value.toMutableSet()
        if (currentSet.contains(folder)) {
            currentSet.remove(folder)
        } else {
            currentSet.add(folder)
        }
        _hiddenFolders.value = currentSet
        settingsPrefs.edit().putStringSet("hidden_folders", currentSet).apply()
        viewModelScope.launch {
            repository.scanSongs()
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            repository.scanSongs()
        }
    }

    // Equalizer Adjustments
    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        settingsPrefs.edit().putBoolean("equalizer_enabled", enabled).apply()
        playerManager.applyPreferencesDirectly()
    }

    fun setBassBoost(value: Float) {
        _bassBoost.value = value
        settingsPrefs.edit().putFloat("bass_boost", value).apply()
        playerManager.applyPreferencesDirectly()
    }

    fun setSurroundSound(value: Float) {
        _surroundSound.value = value
        settingsPrefs.edit().putFloat("surround_sound", value).apply()
    }

    fun setEqualizerBand(index: Int, value: Float) {
        val current = _equalizerBands.value.clone()
        if (index in current.indices) {
            current[index] = value
            _equalizerBands.value = current
            settingsPrefs.edit().putFloat("equalizer_band_$index", value).apply()
            _equalizerPreset.value = "Custom"
            settingsPrefs.edit().putString("equalizer_preset", "Custom").apply()
            playerManager.applyPreferencesDirectly()
        }
    }

    fun setEqualizerPreset(preset: String) {
        _equalizerPreset.value = preset
        settingsPrefs.edit().putString("equalizer_preset", preset).apply()
        when (preset) {
            "Cyberpunk" -> {
                _equalizerBands.value = floatArrayOf(80f, 65f, 75f, 85f, 90f)
                _bassBoost.value = 85f
                _surroundSound.value = 75f
            }
            "Bass Booster" -> {
                _equalizerBands.value = floatArrayOf(95f, 80f, 50f, 40f, 30f)
                _bassBoost.value = 100f
                _surroundSound.value = 40f
            }
            "Treble Booster" -> {
                _equalizerBands.value = floatArrayOf(25f, 40f, 60f, 85f, 95f)
                _bassBoost.value = 20f
                _surroundSound.value = 50f
            }
            "Cinematic" -> {
                _equalizerBands.value = floatArrayOf(75f, 60f, 55f, 70f, 80f)
                _bassBoost.value = 70f
                _surroundSound.value = 90f
            }
            "Vocal" -> {
                _equalizerBands.value = floatArrayOf(40f, 55f, 85f, 80f, 60f)
                _bassBoost.value = 30f
                _surroundSound.value = 30f
            }
            "Flat" -> {
                _equalizerBands.value = floatArrayOf(50f, 50f, 50f, 50f, 50f)
                _bassBoost.value = 0f
                _surroundSound.value = 0f
            }
        }
        for (i in _equalizerBands.value.indices) {
            settingsPrefs.edit().putFloat("equalizer_band_$i", _equalizerBands.value[i]).apply()
        }
        settingsPrefs.edit().putFloat("bass_boost", _bassBoost.value).apply()
        playerManager.applyPreferencesDirectly()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        settingsPrefs.edit().putFloat("playback_speed", speed).apply()
        playerManager.applySpeedDirectly()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.filePath)
        }
    }

    // Playlist CRUD
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "Playlist \"$name\" created instantly!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            val plName = repository.getPlaylistsStatic().find { it.id == playlistId }?.name ?: "Playlist"
            repository.deletePlaylist(playlistId)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "Playlist \"$plName\" deleted", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addSongToPlaylist(playlistId: Int, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song.filePath)
            val plName = repository.getPlaylistsStatic().find { it.id == playlistId }?.name ?: "Playlist"
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "Added to \"$plName\"", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: Int, song: Song) {
        viewModelScope.launch {
            repository.deleteSongFromPlaylist(playlistId, song.filePath)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "Removed from playlist", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun renamePlaylist(playlistId: Int, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(playlistId, newName)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "Renamed playlist to \"$newName\"", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearPlaylist(playlistId: Int) {
        viewModelScope.launch {
            repository.clearPlaylistSongs(playlistId)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(getApplication(), "Playlist cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun reorderPlaylistSongs(playlistId: Int, songList: List<Song>, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            if (fromIndex in songList.indices && toIndex in songList.indices) {
                val dbSongs = repository.getPlaylistSongsStatic()
                    .filter { it.playlistId == playlistId }
                    .sortedBy { it.addedAt }
                if (fromIndex in dbSongs.indices && toIndex in dbSongs.indices) {
                    val list = dbSongs.toMutableList()
                    val moved = list.removeAt(fromIndex)
                    list.add(toIndex, moved)
                    val now = System.currentTimeMillis()
                    val updated = list.mapIndexed { index, entity ->
                        entity.copy(addedAt = now + index * 1000)
                    }
                    repository.restorePlaylistsData(emptyList(), updated)
                }
            }
        }
    }

    fun getSongsInPlaylist(playlistId: Int): Flow<List<Song>> {
        return repository.getSongsInPlaylistFlow(playlistId)
    }

    fun playPlaylist(songs: List<Song>, song: Song) {
        playerManager.setQueue(songs)
        playerManager.playSong(song)
        _isNowPlayingVisible.value = true
    }

    fun shufflePlaylist(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            val shuffled = songs.shuffled()
            playerManager.setQueue(shuffled)
            playerManager.playSong(shuffled.first())
            _isNowPlayingVisible.value = true
        }
    }

    // Playlist backups and restores using local JSON format
    fun backupPlaylists(onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val playlistsList = repository.getPlaylistsStatic()
                val playlistSongsList = repository.getPlaylistSongsStatic()

                val responseObj = org.json.JSONObject()
                val playlistArr = org.json.JSONArray()
                for (pl in playlistsList) {
                    val plObj = org.json.JSONObject().apply {
                        put("id", pl.id)
                        put("name", pl.name)
                        put("createdAt", pl.createdAt)
                    }
                    playlistArr.put(plObj)
                }
                responseObj.put("playlists", playlistArr)

                val songsArr = org.json.JSONArray()
                for (s in playlistSongsList) {
                    val sObj = org.json.JSONObject().apply {
                        put("playlistId", s.playlistId)
                        put("songPath", s.songPath)
                        put("addedAt", s.addedAt)
                    }
                    songsArr.put(sObj)
                }
                responseObj.put("songs", songsArr)

                val backupDir = File(getApplication<Application>().cacheDir, "backups")
                if (!backupDir.exists()) backupDir.mkdirs()
                val backupFile = File(backupDir, "ayvan_playlists_backup.json")
                backupFile.writeText(responseObj.toString(4))

                withContext(Dispatchers.Main) {
                    onComplete("Backup successfully saved to cache: ${backupFile.name}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete("Backup error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun restorePlaylists(onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val backupFile = File(File(getApplication<Application>().cacheDir, "backups"), "ayvan_playlists_backup.json")
                if (!backupFile.exists()) {
                    withContext(Dispatchers.Main) {
                        onComplete("No backup found in cache. Create a backup first!")
                    }
                    return@launch
                }

                val jsonContent = backupFile.readText()
                val responseObj = org.json.JSONObject(jsonContent)
                
                val playlistsArr = responseObj.getJSONArray("playlists")
                val restoredPlaylists = mutableListOf<PlaylistEntity>()
                for (i in 0 until playlistsArr.length()) {
                    val plObj = playlistsArr.getJSONObject(i)
                    restoredPlaylists.add(
                        PlaylistEntity(
                            id = plObj.getInt("id"),
                            name = plObj.getString("name"),
                            createdAt = plObj.getLong("createdAt")
                        )
                    )
                }

                val songsArr = responseObj.getJSONArray("songs")
                val restoredSongs = mutableListOf<PlaylistSongEntity>()
                for (i in 0 until songsArr.length()) {
                    val sObj = songsArr.getJSONObject(i)
                    restoredSongs.add(
                        PlaylistSongEntity(
                            playlistId = sObj.getInt("playlistId"),
                            songPath = sObj.getString("songPath"),
                            addedAt = sObj.getLong("addedAt")
                        )
                    )
                }

                repository.restorePlaylistsData(restoredPlaylists, restoredSongs)

                withContext(Dispatchers.Main) {
                    onComplete("Restored ${restoredPlaylists.size} playlists from backup successfully!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete("Restore error: ${e.localizedMessage}")
                }
            }
        }
    }

    // Cache clearing and system optimization
    fun clearCache(onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cont = getApplication<Application>()
                cont.cacheDir.deleteRecursively()
                cont.externalCacheDir?.deleteRecursively()
                withContext(Dispatchers.Main) {
                    onComplete("App Cache cleared successfully!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete("Error clearing cache: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearArtworkCache(onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cont = getApplication<Application>()
                val coilLoader = ImageLoader(cont)
                coilLoader.diskCache?.clear()
                coilLoader.memoryCache?.clear()
                withContext(Dispatchers.Main) {
                    onComplete("Visual Artwork Cache cleared successfully!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete("Error clearing artwork: ${e.localizedMessage}")
                }
            }
        }
    }

    fun optimizeDatabase(onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.db.MusicDatabase.getDatabase(getApplication())
                db.openHelper.writableDatabase.execSQL("VACUUM")
                db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                withContext(Dispatchers.Main) {
                    onComplete("Database queries optimized and compressed successfully!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete("Error optimizing DB: ${e.localizedMessage}")
                }
            }
        }
    }

    // Custom artwork selection
    fun saveCustomArtwork(songId: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (originalBitmap != null) {
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val squareSize = minOf(width, height)
                    val x = (width - squareSize) / 2
                    val y = (height - squareSize) / 2
                    val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, squareSize, squareSize)

                    val artworkDir = File(context.filesDir, "custom_artwork")
                    if (!artworkDir.exists()) {
                        artworkDir.mkdirs()
                    }
                    val destFile = File(artworkDir, "art_${songId}.png")
                    val outStream = java.io.FileOutputStream(destFile)
                    croppedBitmap.compress(Bitmap.CompressFormat.PNG, 90, outStream)
                    outStream.flush()
                    outStream.close()

                    val localPath = destFile.absolutePath
                    repository.saveCustomArtwork(songId, localPath)
                    playerManager.updateSongArtworkInPlayer(songId, localPath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeCustomArtwork(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val artworkDir = File(context.filesDir, "custom_artwork")
                val destFile = File(artworkDir, "art_${songId}.png")
                if (destFile.exists()) {
                    destFile.delete()
                }

                repository.deleteCustomArtwork(songId)

                val originalSong = repository.allScannedSongs.value.find { it.id == songId }
                val originalArt = originalSong?.artworkUri
                playerManager.updateSongArtworkInPlayer(songId, originalArt)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Modern System Ringtone & Metadata Configurator
    fun setAsRingtone(song: Song, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (!android.provider.Settings.System.canWrite(context)) {
                onResult(false, "PERMISSION_REQUIRED")
                return@launch
            }

            try {
                if (song.filePath.startsWith("virtual_track_")) {
                    onResult(false, "Virtual demo tracks cannot be set as ringtones. Please download or scan actual local files!")
                    return@launch
                }

                val file = File(song.filePath)
                if (!file.exists()) {
                    onResult(false, "Source file not found on storage")
                    return@launch
                }

                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DATA, file.absolutePath)
                    put(android.provider.MediaStore.MediaColumns.TITLE, song.title)
                    put(android.provider.MediaStore.MediaColumns.SIZE, file.length())
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                    put(android.provider.MediaStore.Audio.Media.ARTIST, song.artist)
                    put(android.provider.MediaStore.Audio.Media.IS_RINGTONE, true)
                    put(android.provider.MediaStore.Audio.Media.IS_NOTIFICATION, false)
                    put(android.provider.MediaStore.Audio.Media.IS_ALARM, false)
                    put(android.provider.MediaStore.Audio.Media.IS_MUSIC, false)
                }

                val baseUri = android.provider.MediaStore.Audio.Media.getContentUriForPath(file.absolutePath)
                if (baseUri == null) {
                    onResult(false, "Failed to get MediaStore URI")
                    return@launch
                }

                val cursor = context.contentResolver.query(
                    baseUri,
                    arrayOf(android.provider.MediaStore.MediaColumns._ID),
                    "${android.provider.MediaStore.MediaColumns.DATA}=?",
                    arrayOf(file.absolutePath),
                    null
                )

                var ringtoneUri: Uri? = null
                if (cursor != null && cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns._ID))
                    ringtoneUri = android.content.ContentUris.withAppendedId(baseUri, id)
                    context.contentResolver.update(ringtoneUri, values, null, null)
                } else {
                    ringtoneUri = context.contentResolver.insert(baseUri, values)
                }
                cursor?.close()

                if (ringtoneUri != null) {
                    android.media.RingtoneManager.setActualDefaultRingtoneUri(
                        context,
                        android.media.RingtoneManager.TYPE_RINGTONE,
                        ringtoneUri
                    )
                    onResult(true, "Ringtone set successfully!")
                } else {
                    onResult(false, "Could not map file to system MediaStore")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Ringtone Error: ${e.localizedMessage}")
            }
        }
    }

    // Modern Secured Share Chooser Intent Syste, m
    fun shareSong(song: Song, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                if (song.filePath.startsWith("virtual_track_")) {
                    onResult(false, "Virtual tracks are stream-only and cannot be shared.")
                    return@launch
                }
                
                val file = File(song.filePath)
                if (!file.exists()) {
                    onResult(false, "File not found on storage")
                    return@launch
                }

                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    val authority = context.packageName + ".fileprovider"
                    val fileUri: Uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                    putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Sharing Audio Track")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Listen to '${song.title}' by ${song.artist}")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = android.content.Intent.createChooser(shareIntent, "Share Track via").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                onResult(true, "Opened")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Failed to share song: ${e.localizedMessage}")
            }
        }
    }

    // ==========================================
    // POSTER DIAGNOSTICS & LOGGING SYSTEM
    // ==========================================
    
    private val _diagnosticsStats = MutableStateFlow(DiagnosticsStats())
    val diagnosticsStats: StateFlow<DiagnosticsStats> = _diagnosticsStats.asStateFlow()
    
    private val _diagnosticsLogs = MutableStateFlow<List<String>>(emptyList())
    val diagnosticsLogs: StateFlow<List<String>> = _diagnosticsLogs.asStateFlow()

    fun logDiagnostic(message: String) {
        _diagnosticsLogs.update { (it + message).takeLast(100) }
        android.util.Log.d("AyvaanDiagnostics", message)
    }

    fun refreshPosterDiagnostics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pool = repository.getAllArtworkPoolStatic()
                val assignments = repository.getAllSongArtworkMapsStatic()
                val songs = repository.allScannedSongs.value
                val custom = repository.customArtworksFlow.firstOrNull() ?: emptyList()
                
                val context = getApplication<Application>()
                
                var sizeBytes = 0L
                val coilDir = File(context.cacheDir, "image_cache")
                if (coilDir.exists() && coilDir.isDirectory) {
                    sizeBytes = coilDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                }
                
                val cacheSizeStr = "${sizeBytes / (1024 * 1024)} MB"
                
                val assignedCount = assignments.filter { it.sourceType != "embedded" && it.artworkId != -1 }.size
                val unassignedCount = songs.size - assignments.size
                
                val lastAssignmentAt = assignments.maxOfOrNull { it.assignedAt } ?: 0L
                val dateStr = if (lastAssignmentAt > 0) {
                    java.text.SimpleDateFormat("MMM dd, yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastAssignmentAt))
                } else {
                    "Never"
                }
                
                _diagnosticsStats.value = DiagnosticsStats(
                    postersLoaded = pool.size,
                    categories = pool.map { it.artworkCategory }.distinct().size,
                    songsAssigned = assignedCount,
                    albumsAssigned = songs.filter { s -> assignments.any { it.songId == s.id } }.distinctBy { it.album }.size,
                    artistsAssigned = songs.filter { s -> assignments.any { it.songId == s.id } }.distinctBy { it.artist }.size,
                    playlistsAssigned = 0,
                    unassignedSongs = if (unassignedCount < 0) 0 else unassignedCount,
                    cacheSize = cacheSizeStr,
                    lastAssignmentRun = dateStr
                )
            } catch (e: Exception) {
                logDiagnostic("Error refreshing diagnostics: ${e.message}")
            }
        }
    }
}
