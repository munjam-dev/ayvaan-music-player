package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["addedAt"])]
)
data class FavoriteEntity(
    @PrimaryKey val songPath: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "play_history",
    indices = [
        Index(value = ["songPath"]),
        Index(value = ["timestamp"])
    ]
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songPath: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "Playlist",
    indices = [Index(value = ["createdAt"])]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "playlistId")
    val id: Int = 0,
    
    @ColumnInfo(name = "playlistName")
    val name: String,
    
    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "coverImage")
    val coverImage: String? = null
)

@Entity(
    tableName = "PlaylistSongs",
    primaryKeys = ["playlistId", "songId"],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["songId"])
    ]
)
data class PlaylistSongEntity(
    val playlistId: Int,
    
    @ColumnInfo(name = "songId")
    val songPath: String,
    
    @ColumnInfo(name = "position")
    val position: Int = 0,
    
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_artworks")
data class CustomArtworkEntity(
    @PrimaryKey val songId: String,
    val artworkPath: String,
    val assignedAt: Long = System.currentTimeMillis(),
    val sourceType: String = "pool"
)

@Entity(tableName = "artwork_pool")
data class ArtworkPoolEntity(
    @PrimaryKey(autoGenerate = true) val artworkId: Int = 0,
    val artworkPath: String,
    val artworkCategory: String,
    val artworkPriority: Int,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "song_artwork_map")
data class SongArtworkMapEntity(
    @PrimaryKey val songId: String,
    val artworkId: Int,
    val sourceType: String, // "embedded", "defaultPoster", "customPoster"
    val assignedAt: Long = System.currentTimeMillis()
)


