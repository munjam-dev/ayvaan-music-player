package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.R
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.InputStream
import java.net.URL

class MusicService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var playerManager: MusicPlayerManager
    private lateinit var repository: MusicRepository
    private var mediaSession: MediaSessionCompat? = null

    private var currentSongJob: Job? = null
    private var isPlayingJob: Job? = null
    private var favoriteStatusJob: Job? = null

    private var loadedArtworkBitmap: Bitmap? = null
    private var currentArtworkUrl: String? = null

    companion object {
        const val CHANNEL_ID = "ayvan_premium_music_channel"
        const val NOTIFICATION_ID = 2026528

        const val ACTION_PLAY_PAUSE = "com.example.player.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.player.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.player.ACTION_PREVIOUS"
        const val ACTION_FAVORITE_TOGGLE = "com.example.player.ACTION_FAVORITE_TOGGLE"
        const val ACTION_STOP = "com.example.player.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        playerManager = MusicPlayerManager.getInstance(this)
        repository = MusicRepository(this)

        createNotificationChannel()
        setupMediaSession()
        startDefaultForeground()
        observePlaybackState()
    }

    private fun startDefaultForeground() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val flagImmutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, flagImmutable
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ayvan_notification)
            .setContentTitle("Ayvaan Premium Audio")
            .setContentText("Music engine active")
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(MediaStyle().setMediaSession(mediaSession?.sessionToken))
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            handleAction(action)
        }
        return START_STICKY
    }

    private fun handleAction(action: String) {
        when (action) {
            ACTION_PLAY_PAUSE -> playerManager.togglePlayPause()
            ACTION_NEXT -> playerManager.next()
            ACTION_PREVIOUS -> playerManager.previous()
            ACTION_FAVORITE_TOGGLE -> {
                playerManager.currentSong.value?.let { song ->
                    serviceScope.launch {
                        repository.toggleFavorite(song.filePath)
                        // Trigger a notification update to refresh the favorite heart icon
                        updateNotification(playerManager.currentSong.value, playerManager.isPlaying.value)
                    }
                }
            }
            ACTION_STOP -> {
                playerManager.pause()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Ayvaan Premium Audio Playback"
            val descriptionText = "Displays audio playback controls for the Ayvaan music engine."
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "AyvanMediaSession").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playerManager.resume()
                }

                override fun onPause() {
                    playerManager.pause()
                }

                override fun onSkipToNext() {
                    playerManager.next()
                }

                override fun onSkipToPrevious() {
                    playerManager.previous()
                }

                override fun onStop() {
                    handleAction(ACTION_STOP)
                }
            })

            isActive = true
        }
    }

    private fun observePlaybackState() {
        // Collect current song updates
        currentSongJob = serviceScope.launch {
            playerManager.currentSong.collectLatest { song ->
                if (song != null) {
                    updateMediaSessionMetadata(song)
                    loadArtwork(song)
                } else {
                    updateNotification(null, false)
                }
            }
        }

        // Collect show/hide playing updates
        isPlayingJob = serviceScope.launch {
            playerManager.isPlaying.collectLatest { playing ->
                updateMediaSessionPlaybackState(playing, playerManager.playbackPosition.value)
                updateNotification(playerManager.currentSong.value, playing)
            }
        }

        // Collect favorite updates continuously to update favorite heart icon reactively
        favoriteStatusJob = serviceScope.launch {
            repository.favoritePathsFlow.collectLatest { _ ->
                // Refresh notification state whenever favorites lists in repository databases update
                updateNotification(playerManager.currentSong.value, playerManager.isPlaying.value)
            }
        }
    }

    private fun updateMediaSessionMetadata(song: Song) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)

        val artworkBitmap = loadedArtworkBitmap
        if (artworkBitmap != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artworkBitmap)
        }
        
        mediaSession?.setMetadata(metadataBuilder.build())
    }

    private fun updateMediaSessionPlaybackState(isPlaying: Boolean, position: Long) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO

        val stateBuilder = PlaybackStateCompat.Builder()
            .setState(state, position, 1.0f)
            .setActions(actions)

        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private fun loadArtwork(song: Song) {
        val uri = song.artworkUri
        if (uri == currentArtworkUrl) {
            // Already loaded or loading this song's artwork
            return
        }
        currentArtworkUrl = uri

        serviceScope.launch(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            if (!uri.isNullOrEmpty()) {
                try {
                    // Pre-populate with standard Coil loader
                    val loader = ImageLoader(this@MusicService)
                    val request = ImageRequest.Builder(this@MusicService)
                        .data(uri)
                        .allowHardware(false) // Must be software bitmap to show in notifications
                        .build()
                    val result = loader.execute(request)
                    bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (bitmap == null) {
                try {
                    val fallbackUrl = Song.getDeterministicFallbackUrl(song.title, song.artist)
                    val loader = ImageLoader(this@MusicService)
                    val request = ImageRequest.Builder(this@MusicService)
                        .data(fallbackUrl)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                loadedArtworkBitmap = bitmap
                updateMediaSessionMetadata(song)
                updateNotification(song, playerManager.isPlaying.value)
            }
        }
    }

    private fun updateNotification(song: Song?, isPlaying: Boolean) {
        if (song == null) {
            return
        }

        serviceScope.launch {
            val prefs = getSharedPreferences("ayvaan_settings_prefs", Context.MODE_PRIVATE)
            val showNotificationControlsSetting = prefs.getBoolean("show_notification_controls", true)
            if (!showNotificationControlsSetting) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                return@launch
            }

            val lockScreenControlsSetting = prefs.getBoolean("lock_screen_controls", true)
            val visibility = if (lockScreenControlsSetting) {
                NotificationCompat.VISIBILITY_PUBLIC
            } else {
                NotificationCompat.VISIBILITY_SECRET
            }

            // Retrieve whether the current track is a favorite from repository source of truth
            val isFav = repository.isFavorite(song.filePath)

            // Setup Intents for tapping on widgets
            val openAppIntent = Intent(this@MusicService, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            
            // Build modern immutable PendingIntents
            val flagImmutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }

            val openAppPendingIntent = PendingIntent.getActivity(
                this@MusicService, 0, openAppIntent, flagImmutable
            )

            // Control Actions PendingIntents
            val prevPendingIntent = PendingIntent.getService(
                this@MusicService, 1, Intent(this@MusicService, MusicService::class.java).apply { action = ACTION_PREVIOUS }, flagImmutable
            )
            val playPausePendingIntent = PendingIntent.getService(
                this@MusicService, 2, Intent(this@MusicService, MusicService::class.java).apply { action = ACTION_PLAY_PAUSE }, flagImmutable
            )
            val nextPendingIntent = PendingIntent.getService(
                this@MusicService, 3, Intent(this@MusicService, MusicService::class.java).apply { action = ACTION_NEXT }, flagImmutable
            )
            val favPendingIntent = PendingIntent.getService(
                this@MusicService, 4, Intent(this@MusicService, MusicService::class.java).apply { action = ACTION_FAVORITE_TOGGLE }, flagImmutable
            )
            val closePendingIntent = PendingIntent.getService(
                this@MusicService, 5, Intent(this@MusicService, MusicService::class.java).apply { action = ACTION_STOP }, flagImmutable
            )

            // Dynamic Icons
            val playPauseIcon = if (isPlaying) R.drawable.ic_notification_pause else R.drawable.ic_notification_play
            val favIcon = if (isFav) R.drawable.ic_notification_fav else R.drawable.ic_notification_fav_border

            // Build dynamic default large icon if no Unsplash URL or physical art was resolved
            val largeArt = loadedArtworkBitmap ?: BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

            // Construct standard Android MediaStyle notification
            val notificationBuilder = NotificationCompat.Builder(this@MusicService, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_ayvan_notification) // High-contrast crisp vector small status icon
                .setLargeIcon(largeArt)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setSubText(song.album)
                .setContentIntent(openAppPendingIntent)
                .setVisibility(visibility)
                .setOngoing(isPlaying)
                // Add actions in normal music standard positioning
                .addAction(favIcon, "Favorite", favPendingIntent)
                .addAction(R.drawable.ic_notification_prev, "Previous", prevPendingIntent)
                .addAction(playPauseIcon, "Play/Pause", playPausePendingIntent)
                .addAction(R.drawable.ic_notification_next, "Next", nextPendingIntent)
                .addAction(R.drawable.ic_notification_close, "Close", closePendingIntent)
                // Map MediaStyle parameters
                .setStyle(
                    MediaStyle()
                        .setMediaSession(mediaSession?.sessionToken)
                        // Show Favorite, Previous, Play, Next in collapsed state (0, 1, 2, 3 indexes)
                        .setShowActionsInCompactView(1, 2, 3)
                )

            val notification = notificationBuilder.build()

            if (isPlaying) {
                // Call startForeground to prevent systems from killing our process
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e: Exception) {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            } else {
                // Set ongoing to false to allow swipe dismissing
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(false)
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keeps playback playing smoothly when swipe dismissing app from Overview (recent apps) screen
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        currentSongJob?.cancel()
        isPlayingJob?.cancel()
        favoriteStatusJob?.cancel()
        serviceScope.cancel()
        
        mediaSession?.run {
            isActive = false
            release()
        }
        super.onDestroy()
    }
}
