package com.example.data.model

import android.content.Context
import android.net.Uri

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val filePath: String,
    val folder: String,
    val artworkUri: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val dateAdded: Long = 0L
) {
    val durationFormatted: String
        get() = formatDuration(duration)

    companion object {
        fun formatDuration(durationMs: Long): String {
            val totalSecs = durationMs / 1000
            val mins = totalSecs / 60
            val secs = totalSecs % 60
            return String.format("%02d:%02d", mins, secs)
        }

        fun getDeterministicFallbackUrl(title: String, artist: String): String {
            val seed = (title + artist).hashCode()
            val index = java.lang.Math.abs(seed) % 5
            return when (index) {
                0 -> "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=80" // Purple abstract fluid
                1 -> "https://images.unsplash.com/photo-1614741118887-7a4ee193a5fa?w=500&auto=format&fit=crop&q=80" // Neon wave
                2 -> "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=500&auto=format&fit=crop&q=80" // Dark ambient
                3 -> "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=500&auto=format&fit=crop&q=80" // Black fluid geometric
                else -> "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&auto=format&fit=crop&q=80" // Cyberpunk tech grid
            }
        }

        fun getSongArtwork(context: Context, song: Song, customArtworksMap: Map<String, String>, poolImages: List<String>): String? {
            // 1. User Custom Artwork
            val manualArt = customArtworksMap[song.id]
            if (!manualArt.isNullOrEmpty()) {
                return manualArt
            }

            // 2. Embedded Album Art (Just return the URI, let Coil handle missing streams lazily)
            if (!song.filePath.startsWith("virtual_track_") && !song.artworkUri.isNullOrEmpty() && song.artworkUri.startsWith("content://")) {
                return song.artworkUri
            } else if (!song.artworkUri.isNullOrEmpty() && (song.artworkUri.startsWith("http://") || song.artworkUri.startsWith("https://"))) {
                return song.artworkUri
            }

            // 3. Uploaded Artwork Pool
            if (poolImages.isNotEmpty()) {
                val best = selectBestArtwork(song, poolImages)
                if (best != null) return best
            }

            // 4/5. Default Fallback Artwork
            return getDeterministicFallbackUrl(song.title, song.artist)
        }

        fun detectSongGenre(song: Song): String {
            val text = "${song.title} ${song.artist} ${song.album} ${song.filePath} ${song.folder}".lowercase()
            return when {
                text.contains("telugu") || text.contains("tollywood") || text.contains("sid sriram") || text.contains("dsp") -> "Telugu"
                text.contains("hindi") || text.contains("bollywood") || text.contains("arijit") || text.contains("shreya") -> "Hindi"
                text.contains("tamil") || text.contains("kollywood") || text.contains("ar rahman") || text.contains("anirudh") -> "Tamil"
                text.contains("devotional") || text.contains("bhakti") || text.contains("stotra") || text.contains("shiva") || text.contains("temple") || text.contains("krishna") || text.contains("hanuman") || text.contains("sai baba") -> "Devotional"
                text.contains("classical") || text.contains("carnatic") || text.contains("tabla") -> "Classical"
                text.contains("folk") || text.contains("janapada") || text.contains("desi") -> "Folk"
                text.contains("rock") || text.contains("pink floyd") || text.contains("linkin") || text.contains("metallica") -> "Rock"
                text.contains("pop") || text.contains("swift") || text.contains("sheeran") || text.contains("bieber") || text.contains("dido") -> "Pop"
                text.contains("cyber") || text.contains("synth") || text.contains("neon") || text.contains("original") -> "Synth & Electronic"
                else -> "English"
            }
        }

        fun selectBestArtwork(song: Song, poolImages: List<String>): String? {
            if (poolImages.isEmpty()) return null

            val titleLower = song.title.lowercase()
            val albumLower = song.album.lowercase()
            val artistLower = song.artist.lowercase()
            val folderLower = song.folder.lowercase()
            val fileLower = song.filePath.substringAfterLast("/").lowercase()
            val genreLower = detectSongGenre(song).lowercase()

            val combinedMetadata = "$titleLower $albumLower $artistLower $folderLower $fileLower $genreLower"

            var bestImage: String? = null
            var bestScore = -1

            for (path in poolImages) {
                val file = java.io.File(path)
                val rawName = file.nameWithoutExtension.lowercase()
                
                // Score starts at 0 for each image Candidate
                var score = 0

                // 1. Check Specific Rules and Keyword Matches (High weight = 100)
                
                // Hi Nanna
                val matchesHiNannaSong = titleLower.contains("hi nanna") || albumLower.contains("hi nanna") || fileLower.contains("hi nanna") || fileLower.contains("hi_nanna")
                val matchesHiNannaArt = rawName.contains("hi nanna") || rawName.contains("hi_nanna")
                if (matchesHiNannaSong && matchesHiNannaArt) {
                    score += 100
                }

                // Krishna
                val matchesKrishnaSong = titleLower.contains("krishna") || titleLower.contains("govinda") || titleLower.contains("hare krishna") || titleLower.contains("radha") ||
                                         albumLower.contains("krishna") || fileLower.contains("krishna") || fileLower.contains("govinda")
                val matchesKrishnaArt = rawName.contains("krishna") || rawName.contains("govinda") || rawName.contains("radha")
                if (matchesKrishnaSong && matchesKrishnaArt) {
                    score += 100
                }

                // Hanuman
                val matchesHanumanSong = titleLower.contains("hanuman") || titleLower.contains("anjaneya") || titleLower.contains("bajrang") ||
                                         albumLower.contains("hanuman") || fileLower.contains("hanuman") || fileLower.contains("anjaneya")
                val matchesHanumanArt = rawName.contains("hanuman") || rawName.contains("anjaneya") || rawName.contains("bajrang")
                if (matchesHanumanSong && matchesHanumanArt) {
                    score += 100
                }

                // Sai Baba
                val matchesSaiSong = titleLower.contains("sai baba") || titleLower.contains("shirdi") || titleLower.contains("sai") ||
                                     albumLower.contains("sai baba") || fileLower.contains("sai") || fileLower.contains("shirdi")
                val matchesSaiArt = rawName.contains("sai baba") || rawName.contains("sai_baba") || rawName.contains("shirdi") || rawName.contains("sai")
                if (matchesSaiSong && matchesSaiArt) {
                    score += 100
                }

                // 2. Generic Keyword overlapping (Medium weight)
                // Remove generic timestamp/prefix: e.g. "img_123456789_1234"
                val cleanedName = rawName.replace(Regex("img_\\d+_\\d+"), "")
                val tokens = cleanedName.split(Regex("[^a-zA-Z0-9]")).filter { 
                    it.length >= 3 && it != "img" && it != "png" && it != "jpg" && it != "jpeg" 
                }

                for (token in tokens) {
                    if (combinedMetadata.contains(token)) {
                        score += 50
                        
                        // Stronger match if the token specifically appears in title, album, artist
                        if (titleLower.contains(token)) score += 30
                        if (albumLower.contains(token)) score += 20
                        if (artistLower.contains(token)) score += 20
                        if (fileLower.contains(token)) score += 10
                        if (folderLower.contains(token)) score += 10
                    }
                }

                if (score > bestScore) {
                    bestScore = score
                    bestImage = path
                }
            }

            // Return first or highest score image if any positive score exists.
            if (bestScore > 0 && bestImage != null) {
                return bestImage
            }

            // Default fallback if no fuzzy match score > 0 (use deterministic hash modulo check)
            val songIndex = (song.id.hashCode() and Int.MAX_VALUE) % poolImages.size
            return poolImages[songIndex]
        }
    }
}
