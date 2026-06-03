package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class MostPlayedItem(
    val songPath: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val playCount: Int
)

@Dao
interface MusicDao {

    // --- FAVORITES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songPath = :songPath")
    suspend fun deleteFavorite(songPath: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songPath = :songPath)")
    suspend fun isFavoriteState(songPath: String): Boolean

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavoritesFlow(): Flow<List<FavoriteEntity>>


    // --- PLAY HISTORY ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayHistory(history: PlayHistoryEntity)

    @Query("SELECT * FROM play_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistoryFlow(): Flow<List<PlayHistoryEntity>>

    @Query("""
        SELECT songPath, title, artist, album, duration, COUNT(songPath) as playCount 
        FROM play_history 
        GROUP BY songPath 
        ORDER BY playCount DESC 
        LIMIT 30
    """)
    fun getMostPlayedFlow(): Flow<List<MostPlayedItem>>

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()


    // --- PLAYLISTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM Playlist WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Int)

    @Query("SELECT * FROM Playlist ORDER BY createdAt DESC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM Playlist WHERE playlistId = :playlistId")
    suspend fun getPlaylistById(playlistId: Int): PlaylistEntity?


    // --- PLAYLIST SONGS ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSong(playlistSong: PlaylistSongEntity)

    @Query("DELETE FROM PlaylistSongs WHERE playlistId = :playlistId AND songId = :songPath")
    suspend fun deletePlaylistSong(playlistId: Int, songPath: String)

    @Query("DELETE FROM play_history WHERE songPath = :songPath")
    suspend fun deletePlayHistoryByPath(songPath: String)

    @Query("DELETE FROM PlaylistSongs WHERE songId = :songPath")
    suspend fun deletePlaylistSongsByPath(songPath: String)

    @Query("SELECT * FROM PlaylistSongs WHERE songId = :songPath")
    suspend fun getPlaylistSongsByPathStatic(songPath: String): List<PlaylistSongEntity>

    @Query("SELECT songId FROM PlaylistSongs WHERE playlistId = :playlistId ORDER BY position ASC, addedAt ASC")
    fun getPlaylistSongPathsFlow(playlistId: Int): Flow<List<String>>

    @Query("DELETE FROM PlaylistSongs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Int)


    // --- CUSTOM ARTWORKS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomArtwork(customArtwork: CustomArtworkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomArtworks(customArtworks: List<CustomArtworkEntity>)

    @Query("DELETE FROM custom_artworks WHERE songId = :songId")
    suspend fun deleteCustomArtwork(songId: String)

    @Query("DELETE FROM custom_artworks")
    suspend fun deleteAllCustomArtworks()

    @Query("SELECT artworkPath FROM custom_artworks WHERE songId = :songId")
    suspend fun getCustomArtworkPath(songId: String): String?

    @Query("SELECT * FROM custom_artworks")
    fun getAllCustomArtworksFlow(): Flow<List<CustomArtworkEntity>>

    // --- ARTWORK POOL AND SONG ARTWORK MAPS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtworkPoolItem(item: ArtworkPoolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtworkPoolItems(items: List<ArtworkPoolEntity>)

    @Query("SELECT * FROM artwork_pool ORDER BY artworkPriority ASC, dateAdded DESC")
    fun getAllArtworkPoolFlow(): Flow<List<ArtworkPoolEntity>>

    @Query("SELECT * FROM artwork_pool ORDER BY artworkPriority ASC, dateAdded DESC")
    suspend fun getAllArtworkPoolStatic(): List<ArtworkPoolEntity>

    @Query("DELETE FROM artwork_pool")
    suspend fun clearArtworkPool()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongArtworkMap(map: SongArtworkMapEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongArtworkMaps(maps: List<SongArtworkMapEntity>)

    @Query("DELETE FROM song_artwork_map WHERE songId = :songId")
    suspend fun deleteSongArtworkMap(songId: String)

    @Query("DELETE FROM song_artwork_map")
    suspend fun deleteAllSongArtworkMaps()

    @Query("SELECT * FROM song_artwork_map")
    fun getAllSongArtworkMapsFlow(): Flow<List<SongArtworkMapEntity>>

    @Query("SELECT * FROM song_artwork_map")
    suspend fun getAllSongArtworkMapsStatic(): List<SongArtworkMapEntity>

    @Query("SELECT * FROM Playlist")
    suspend fun getAllPlaylistsStatic(): List<PlaylistEntity>

    @Query("SELECT * FROM PlaylistSongs")
    suspend fun getAllPlaylistSongsStatic(): List<PlaylistSongEntity>

    @Query("SELECT * FROM PlaylistSongs ORDER BY position ASC, addedAt ASC")
    fun getAllPlaylistSongsFlow(): Flow<List<PlaylistSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistsStatic(playlists: List<PlaylistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongsStatic(songs: List<PlaylistSongEntity>)
}
