package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.example.data.db.*
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {

    private val database = MusicDatabase.getDatabase(context)
    private val dao = database.musicDao()

    // Managed scope for repository operations (avoids coroutine leaks)
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Dynamic state of scanned songs
    private val _allScannedSongs = MutableStateFlow<List<Song>>(emptyList())
    val allScannedSongs: StateFlow<List<Song>> = _allScannedSongs.asStateFlow()

    // Room inputs and custom lists mapping to Song objects
    val favoritePathsFlow: Flow<List<String>> = dao.getAllFavoritesFlow().map { list ->
        list.map { it.songPath }
    }

    val playHistoryFlow: Flow<List<PlayHistoryEntity>> = dao.getRecentHistoryFlow()
    val mostPlayedFlow: Flow<List<MostPlayedItem>> = dao.getMostPlayedFlow()
    val playlistsFlow: Flow<List<PlaylistEntity>> = dao.getAllPlaylistsFlow()
    val allPlaylistSongsFlow: Flow<List<PlaylistSongEntity>> = dao.getAllPlaylistSongsFlow()

    init {
        // Initial scan of songs
        scanSongs()
    }

    private var scanJob: kotlinx.coroutines.Job? = null

    fun scanSongs() {
        // Cancel any in-progress scan to avoid stacking up
        scanJob?.cancel()
        scanJob = repositoryScope.launch {
            val songsList = mutableListOf<Song>()
            val contentResolver = context.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val prefs = context.getSharedPreferences("ayvaan_settings_prefs", Context.MODE_PRIVATE)
            val ignoreShortAudioSec = prefs.getInt("ignore_short_audio_sec", 10)
            val hiddenFolders = prefs.getStringSet("hidden_folders", emptySet()) ?: emptySet()

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            try {
                contentResolver.query(uri, projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol).toString()
                        val title = cursor.getString(titleCol) ?: "Unknown Track"
                        val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                        val album = cursor.getString(albumCol) ?: "Unknown Album"
                        val duration = cursor.getLong(durationCol)
                        val filePath = cursor.getString(dataCol) ?: ""
                        val dateAdded = cursor.getLong(dateAddedCol)

                        // Support MP3, WAV, FLAC, AAC, M4A
                        val lowerPath = filePath.lowercase()
                        val isSupportedFormat = lowerPath.endsWith(".mp3") ||
                                lowerPath.endsWith(".wav") ||
                                lowerPath.endsWith(".flac") ||
                                lowerPath.endsWith(".aac") ||
                                lowerPath.endsWith(".m4a")

                        if (filePath.isNotEmpty() && isSupportedFormat) {
                            val folder = try {
                                File(filePath).parentFile?.name ?: "Local Files"
                            } catch (e: Exception) {
                                "Local Files"
                            }

                            val durationSec = duration / 1000
                            val isHidden = hiddenFolders.contains(folder)
                            val meetsDuration = durationSec >= ignoreShortAudioSec

                            if (!isHidden && meetsDuration) {
                                songsList.add(
                                    Song(
                                        id = id,
                                        title = title,
                                        artist = artist,
                                        album = album,
                                        duration = duration,
                                        filePath = filePath,
                                        folder = folder,
                                        artworkUri = "content://media/external/audio/albumart/$id",
                                        dateAdded = dateAdded
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Setup scanned songs library list directly, ensuring Ayvaan originals are prepended
            _allScannedSongs.value = getVirtualCyberpunkSongs() + songsList
        }
    }

    suspend fun getPlaylistsStatic(): List<PlaylistEntity> = dao.getAllPlaylistsStatic()
    suspend fun getPlaylistSongsStatic(): List<PlaylistSongEntity> = dao.getAllPlaylistSongsStatic()
    
    suspend fun restorePlaylistsData(playlists: List<PlaylistEntity>, songs: List<PlaylistSongEntity>) = withContext(Dispatchers.IO) {
        dao.insertPlaylistsStatic(playlists)
        dao.insertPlaylistSongsStatic(songs)
    }

    // --- FAVORITES ACTION ---
    suspend fun toggleFavorite(songPath: String) = withContext(Dispatchers.IO) {
        val alreadyFav = dao.isFavoriteState(songPath)
        if (alreadyFav) {
            dao.deleteFavorite(songPath)
        } else {
            dao.insertFavorite(FavoriteEntity(songPath))
        }
    }

    suspend fun isFavorite(songPath: String): Boolean = withContext(Dispatchers.IO) {
        dao.isFavoriteState(songPath)
    }

    // --- PLAY HISTORY ACTION ---
    suspend fun addSongToHistory(song: Song) = withContext(Dispatchers.IO) {
        val lastEntity = PlayHistoryEntity(
            songPath = song.filePath,
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration
        )
        dao.insertPlayHistory(lastEntity)
    }

    // --- PLAYLIST ACTIONS ---
    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        dao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlistId: Int) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
        dao.clearPlaylistSongs(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Int, songPath: String) = withContext(Dispatchers.IO) {
        dao.insertPlaylistSong(PlaylistSongEntity(playlistId, songPath))
    }

    suspend fun deleteSongFromPlaylist(playlistId: Int, songPath: String) = withContext(Dispatchers.IO) {
        dao.deletePlaylistSong(playlistId, songPath)
    }

    suspend fun renamePlaylist(playlistId: Int, name: String) = withContext(Dispatchers.IO) {
        val playlist = dao.getPlaylistById(playlistId)
        if (playlist != null) {
            dao.insertPlaylist(playlist.copy(name = name))
        }
    }

    suspend fun clearPlaylistSongs(playlistId: Int) = withContext(Dispatchers.IO) {
        dao.clearPlaylistSongs(playlistId)
    }

    fun getSongsInPlaylistFlow(playlistId: Int): Flow<List<Song>> {
        return combine(
            dao.getPlaylistSongPathsFlow(playlistId),
            _allScannedSongs
        ) { paths, allSongs ->
            paths.mapNotNull { path ->
                allSongs.find { it.filePath == path }
            }
        }
    }

    // Sample cinematic cyberpunk tracks
    private fun getVirtualCyberpunkSongs(): List<Song> {
        return emptyList()
    }

    // --- CUSTOM ARTWORKS ACTIONS ---
    val customArtworksFlow: Flow<List<CustomArtworkEntity>> = dao.getAllCustomArtworksFlow()

    suspend fun saveCustomArtwork(songId: String, artworkPath: String) = withContext(Dispatchers.IO) {
        dao.insertCustomArtwork(CustomArtworkEntity(songId, artworkPath))
    }

    suspend fun saveCustomArtworks(artworks: List<CustomArtworkEntity>) = withContext(Dispatchers.IO) {
        dao.insertCustomArtworks(artworks)
    }

    suspend fun deleteCustomArtwork(songId: String) = withContext(Dispatchers.IO) {
        dao.deleteCustomArtwork(songId)
    }

    suspend fun deleteAllCustomArtworks() = withContext(Dispatchers.IO) {
        dao.deleteAllCustomArtworks()
    }

    // --- ARTWORK POOL ACTIONS ---
    val artworkPoolFlow: Flow<List<ArtworkPoolEntity>> = dao.getAllArtworkPoolFlow()

    suspend fun getAllArtworkPoolStatic(): List<ArtworkPoolEntity> = withContext(Dispatchers.IO) {
        dao.getAllArtworkPoolStatic()
    }

    suspend fun saveArtworkPoolItem(item: ArtworkPoolEntity) = withContext(Dispatchers.IO) {
        dao.insertArtworkPoolItem(item)
    }

    suspend fun saveArtworkPoolItems(items: List<ArtworkPoolEntity>) = withContext(Dispatchers.IO) {
        dao.insertArtworkPoolItems(items)
    }

    suspend fun clearArtworkPool() = withContext(Dispatchers.IO) {
        dao.clearArtworkPool()
    }

    // --- SONG ARTWORK MAP ACTIONS ---
    val songArtworkMapsFlow: Flow<List<SongArtworkMapEntity>> = dao.getAllSongArtworkMapsFlow()

    suspend fun getAllSongArtworkMapsStatic(): List<SongArtworkMapEntity> = withContext(Dispatchers.IO) {
        dao.getAllSongArtworkMapsStatic()
    }

    suspend fun saveSongArtworkMap(map: SongArtworkMapEntity) = withContext(Dispatchers.IO) {
        dao.insertSongArtworkMap(map)
    }

    suspend fun saveSongArtworkMaps(maps: List<SongArtworkMapEntity>) = withContext(Dispatchers.IO) {
        dao.insertSongArtworkMaps(maps)
    }

    suspend fun deleteSongArtworkMap(songId: String) = withContext(Dispatchers.IO) {
        dao.deleteSongArtworkMap(songId)
    }

    suspend fun deleteAllSongArtworkMaps() = withContext(Dispatchers.IO) {
        dao.deleteAllSongArtworkMaps()
    }

    suspend fun getPlaylistSongsForSong(songPath: String): List<PlaylistSongEntity> = withContext(Dispatchers.IO) {
        dao.getPlaylistSongsByPathStatic(songPath)
    }

    suspend fun insertPlaylistSong(playlistSong: PlaylistSongEntity) = withContext(Dispatchers.IO) {
        dao.insertPlaylistSong(playlistSong)
    }

    suspend fun removeSongFromDatabase(songPath: String, songId: String) = withContext(Dispatchers.IO) {
        dao.deleteFavorite(songPath)
        dao.deletePlayHistoryByPath(songPath)
        dao.deletePlaylistSongsByPath(songPath)
        dao.deleteCustomArtwork(songId)
    }

    fun deleteSongFromScannedList(song: Song) {
        _allScannedSongs.value = _allScannedSongs.value.filter { it.id != song.id }
    }

    fun addSongToScannedList(song: Song) {
        val current = _allScannedSongs.value.toMutableList()
        if (current.none { it.id == song.id }) {
            current.add(song)
            _allScannedSongs.value = current
        }
    }
}
