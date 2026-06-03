package com.example.player

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

enum class RepeatMode {
    NONE, ONE, ALL
}

enum class SleepTimerMode {
    OFF, MINUTES_10, MINUTES_20, MINUTES_30, MINUTES_60, END_OF_SONG, END_OF_PLAYLIST
}

class MusicPlayerManager private constructor(
    private val context: Context,
    private val repository: MusicRepository
) {
    companion object {
        @Volatile
        private var INSTANCE: MusicPlayerManager? = null

        fun getInstance(context: Context): MusicPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val appContext = context.applicationContext
                    val repo = MusicRepository(appContext)
                    val instance = MusicPlayerManager(appContext, repo)
                    INSTANCE = instance
                    instance
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: android.media.AudioFocusRequest? = null

    // Pause on headphone unplug
    private val noisyReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: android.content.Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }
    private var noisyReceiverRegistered = false

    private fun registerNoisyReceiver() {
        if (!noisyReceiverRegistered) {
            try {
                context.registerReceiver(
                    noisyReceiver,
                    android.content.IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                )
                noisyReceiverRegistered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun unregisterNoisyReceiver() {
        if (noisyReceiverRegistered) {
            try {
                context.unregisterReceiver(noisyReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            noisyReceiverRegistered = false
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                resume()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val focusRequestObj = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                focusRequest = focusRequestObj
                audioManager.requestAudioFocus(focusRequestObj) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }.also { granted ->
            if (granted) registerNoisyReceiver()
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                focusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        unregisterNoisyReceiver()
    }

    private fun triggerServiceStart() {
        try {
            val intent = android.content.Intent(context, MusicService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Player instances
    private var mediaPlayer: MediaPlayer? = null
    private var synthThread: Job? = null
    private var synthTrack: AudioTrack? = null

    // Sleep Timer
    private val _sleepTimeRemaining = MutableStateFlow(0L)
    val sleepTimeRemaining: StateFlow<Long> = _sleepTimeRemaining.asStateFlow()
    private val _sleepTimerMode = MutableStateFlow(SleepTimerMode.OFF)
    val sleepTimerMode: StateFlow<SleepTimerMode> = _sleepTimerMode.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Player States
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _playbackDuration = MutableStateFlow(0L)
    val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    // Dynamic wave values for UX UI Visualizer
    private val _visualizerAmplitudes = MutableStateFlow(FloatArray(24) { 0.1f })
    val visualizerAmplitudes: StateFlow<FloatArray> = _visualizerAmplitudes.asStateFlow()

    private var progressTrackingJob: Job? = null
    private var visualizerJob: Job? = null

    init {
        // Start visualizer and progress jobs immediately
        startProgressTracker()
        startVisualizerGenerator()
    }

    fun setQueue(songs: List<Song>) {
        _currentQueue.value = songs
    }

    fun playNext(song: Song) {
        val currentList = _currentQueue.value.toMutableList()
        val playingSong = _currentSong.value
        if (playingSong != null) {
            currentList.removeAll { it.filePath == song.filePath }
            val currentIndex = currentList.indexOfFirst { it.filePath == playingSong.filePath }
            if (currentIndex != -1) {
                currentList.add(currentIndex + 1, song)
            } else {
                currentList.add(0, song)
            }
        } else {
            if (currentList.none { it.filePath == song.filePath }) {
                currentList.add(0, song)
            }
        }
        _currentQueue.value = currentList
    }

    fun removeSongFromQueue(song: Song) {
        val currentList = _currentQueue.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == song.id }
        if (index != -1) {
            currentList.removeAt(index)
            _currentQueue.value = currentList
        }
    }

    fun handleDeletedSong(song: Song) {
        val queue = _currentQueue.value.toMutableList()
        val currentIndex = queue.indexOfFirst { it.id == song.id }
        
        if (currentIndex != -1) {
            queue.removeAt(currentIndex)
            _currentQueue.value = queue
        }

        // Try to play next available song
        if (queue.isNotEmpty() && currentIndex != -1) {
            val nextIndex = if (currentIndex < queue.size) currentIndex else 0
            playSong(queue[nextIndex])
        } else {
            // No next song exists. Stop playback, close player gracefully.
            stopPlayback()
            _currentSong.value = null
            _isPlaying.value = false
        }
    }

    fun playLast(song: Song) {
        val currentList = _currentQueue.value.toMutableList()
        currentList.removeAll { it.filePath == song.filePath }
        currentList.add(song)
        _currentQueue.value = currentList
    }

    fun playSimilarSongs(referenceSong: Song, poolOfSongs: List<Song>) {
        val romanticKeywords = listOf("romantic", "romance", "love", "feel", "heart", "dil", "prema", "pranay", "valapu", "melody", "melodious", "sad", "breakup", "duet")
        val refGenre = Song.detectSongGenre(referenceSong)
        val refIsRomantic = romanticKeywords.any { referenceSong.title.lowercase().contains(it) || referenceSong.artist.lowercase().contains(it) }
        
        val scoredSongs = poolOfSongs
            .filter { it.filePath != referenceSong.filePath && !it.filePath.startsWith("virtual_track_") }
            .map { s ->
                var score = 0
                val sGenre = Song.detectSongGenre(s)
                
                // Genre match
                if (sGenre.equals(refGenre, ignoreCase = true)) {
                    score += 50
                }
                // Romantic category match
                val sIsRomantic = romanticKeywords.any { s.title.lowercase().contains(it) || s.artist.lowercase().contains(it) }
                if (refIsRomantic && sIsRomantic) {
                    score += 50
                }
                // Artist match
                if (s.artist.equals(referenceSong.artist, ignoreCase = true) && s.artist != "Unknown Artist") {
                    score += 40
                }
                // Album match
                if (s.album.equals(referenceSong.album, ignoreCase = true) && s.album != "Unknown Album") {
                    score += 30
                }
                // Folder match
                if (s.folder.equals(referenceSong.folder, ignoreCase = true)) {
                    score += 25
                }
                s to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }

        val playQueue = mutableListOf<Song>()
        playQueue.add(referenceSong)
        playQueue.addAll(scoredSongs)
        
        _currentQueue.value = playQueue
    }

    fun updateSongArtworkInPlayer(songId: String, newUri: String?) {
        val current = _currentSong.value
        if (current != null && current.id == songId) {
            _currentSong.value = current.copy(artworkUri = newUri)
        }
        _currentQueue.value = _currentQueue.value.map {
            if (it.id == songId) {
                it.copy(artworkUri = newUri)
            } else {
                it
            }
        }
    }

    fun playSong(song: Song) {
        // Stop current playbacks first
        stopPlayback()

        _currentSong.value = song
        _playbackDuration.value = song.duration
        _playbackPosition.value = 0L

        // Record to history
        scope.launch {
            repository.addSongToHistory(song)
        }

        if (song.filePath.startsWith("virtual_track_")) {
            // It is a premium synth Cyberpunk track! Play via synthesized sound
            startSynthesizer(song)
        } else {
            // Check file existence
            val file = java.io.File(song.filePath)
            if (!file.exists()) {
                scope.launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "File not found: ${song.title}", android.widget.Toast.LENGTH_LONG).show()
                }
                _isPlaying.value = false
                return
            }

            // It is a physical file track scanned from user media storage
            scope.launch {
                try {
                    if (requestAudioFocus()) {
                        val mp = withContext(Dispatchers.IO) {
                            MediaPlayer().apply {
                                setWakeMode(context, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                                setAudioAttributes(
                                    android.media.AudioAttributes.Builder()
                                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                        .build()
                                )
                                // Use file path directly for local files to avoid URI parsing issues on some devices
                                setDataSource(song.filePath)
                                prepare()
                            }
                        }
                        mediaPlayer = mp
                        applyPreferences(mp)
                        mp.start()
                        mp.setOnCompletionListener {
                            handleSongCompletion()
                        }
                        _isPlaying.value = true
                        setupGaplessPlaybackIfNeeded(mp)
                    } else {
                        _isPlaying.value = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Error playing: auto progress or notify user
                    _isPlaying.value = false
                }
            }
        }
        triggerServiceStart()
    }

    private var equalizer: android.media.audiofx.Equalizer? = null
    private var bassBoost: android.media.audiofx.BassBoost? = null
    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null

    private var gaplessEnabled = false
    private var crossfadeSeconds = 0

    fun applyPreferences(mp: MediaPlayer) {
        val prefs = context.getSharedPreferences("ayvaan_settings_prefs", Context.MODE_PRIVATE)
        try {
            val audioSessionId = mp.audioSessionId
            
            // 1. Equalizer Section
            val eqEnabled = prefs.getBoolean("equalizer_enabled", true)
            equalizer?.release()
            equalizer = null
            if (eqEnabled) {
                try {
                    equalizer = android.media.audiofx.Equalizer(0, audioSessionId).apply {
                        enabled = true
                        val preset = prefs.getString("equalizer_preset", "Cyberpunk") ?: "Cyberpunk"
                        if (preset == "Custom") {
                            val numBands = numberOfBands.toInt()
                            for (i in 0 until numBands) {
                                val bandLevel = prefs.getFloat("equalizer_band_$i", 50f)
                                val min = bandLevelRange[0]
                                val max = bandLevelRange[1]
                                val target = (min + (bandLevel / 100f) * (max - min)).toInt().toShort()
                                setBandLevel(i.toShort(), target)
                            }
                        } else {
                            val bands = when (preset) {
                                "Cyberpunk" -> floatArrayOf(80f, 65f, 75f, 85f, 90f)
                                "Bass Booster" -> floatArrayOf(95f, 80f, 50f, 40f, 30f)
                                "Treble Booster" -> floatArrayOf(25f, 40f, 60f, 85f, 95f)
                                "Cinematic" -> floatArrayOf(75f, 60f, 55f, 70f, 80f)
                                "Vocal" -> floatArrayOf(40f, 55f, 85f, 80f, 60f)
                                else -> floatArrayOf(50f, 50f, 50f, 50f, 50f) // Flat
                            }
                            val numBands = numberOfBands.toInt()
                            for (i in 0 until numBands.coerceAtMost(bands.size)) {
                                val min = bandLevelRange[0]
                                val max = bandLevelRange[1]
                                val target = (min + (bands[i] / 100f) * (max - min)).toInt().toShort()
                                setBandLevel(i.toShort(), target)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Bass Boost Section
            val bassVal = prefs.getFloat("bass_boost", 80f)
            bassBoost?.release()
            bassBoost = null
            if (bassVal > 0f) {
                try {
                    bassBoost = android.media.audiofx.BassBoost(0, audioSessionId).apply {
                        enabled = true
                        setStrength((bassVal * 10).toInt().coerceIn(0, 1000).toShort())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. Loudness Enhancer Section
            val loudnessEnabled = prefs.getBoolean("loudness_enhancement", false)
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            if (loudnessEnabled) {
                try {
                    loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(audioSessionId).apply {
                        enabled = true
                        setTargetGain(1000)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 4. Audio Balance Section
            val balance = prefs.getFloat("audio_balance", 0f)
            val leftVol = if (balance > 0f) 1.0f - balance else 1.0f
            val rightVol = if (balance < 0f) 1.0f + balance else 1.0f
            mp.setVolume(leftVol, rightVol)

            // 5. Playback Speed Section
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val speed = prefs.getFloat("playback_speed", 1.0f)
                if (speed != 1.0f) {
                    mp.playbackParams = mp.playbackParams.setSpeed(speed)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyPreferencesDirectly() {
        val mp = mediaPlayer ?: return
        applyPreferences(mp)
    }

    fun applyBalanceDirectly() {
        val mp = mediaPlayer ?: return
        val prefs = context.getSharedPreferences("ayvaan_settings_prefs", Context.MODE_PRIVATE)
        val balance = prefs.getFloat("audio_balance", 0f)
        val leftVol = if (balance > 0f) 1.0f - balance else 1.0f
        val rightVol = if (balance < 0f) 1.0f + balance else 1.0f
        try {
            mp.setVolume(leftVol, rightVol)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applySpeedDirectly() {
        val mp = mediaPlayer ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val prefs = context.getSharedPreferences("ayvaan_settings_prefs", Context.MODE_PRIVATE)
            val speed = prefs.getFloat("playback_speed", 1.0f)
            try {
                mp.playbackParams = mp.playbackParams.setSpeed(speed)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setGapless(enabled: Boolean) {
        gaplessEnabled = enabled
        mediaPlayer?.let { setupGaplessPlaybackIfNeeded(it) }
    }

    fun setCrossfade(seconds: Int) {
        crossfadeSeconds = seconds
    }

    fun notifyNotificationSettingsChanged() {
        try {
            val intent = android.content.Intent(context, MusicService::class.java).apply {
                action = "com.example.ACTION_NOTIFICATION_SETTINGS_CHANGED"
            }
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupGaplessPlaybackIfNeeded(mp: MediaPlayer) {
        val prefs = context.getSharedPreferences("ayvaan_settings_prefs", Context.MODE_PRIVATE)
        val isGapless = prefs.getBoolean("gapless_playback", false)
        if (!isGapless) return

        try {
            val queue = _currentQueue.value
            val current = _currentSong.value ?: return
            var index = queue.indexOfFirst { it.filePath == current.filePath }
            if (index == -1) return
            
            val nextIndex = if (_shuffleMode.value) {
                (queue.indices).random()
            } else {
                index + 1
            }
            if (nextIndex < queue.size) {
                val nextSong = queue[nextIndex]
                if (!nextSong.filePath.startsWith("virtual_track_")) {
                    val nextMp = MediaPlayer().apply {
                        setWakeMode(context, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                        setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        // Use file path directly for local files to avoid URI parsing issues on some devices
                        setDataSource(nextSong.filePath)
                        prepare()
                        applyPreferences(this)
                    }
                    mp.setNextMediaPlayer(nextMp)
                    mp.setOnCompletionListener {
                        try {
                            _currentSong.value = nextSong
                            mediaPlayer = nextMp
                            nextMp.setOnCompletionListener {
                                handleSongCompletion()
                            }
                            setupGaplessPlaybackIfNeeded(nextMp)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        if (_isPlaying.value) {
            _isPlaying.value = false
            mediaPlayer?.pause()
            // Stop simulator thread
            stopSynthStream()
            abandonAudioFocus()
        }
    }

    fun resume() {
        val song = _currentSong.value ?: return
        if (!_isPlaying.value) {
            if (requestAudioFocus()) {
                _isPlaying.value = true
                if (song.filePath.startsWith("virtual_track_")) {
                    startSynthesizer(song, resumeFromPosition = _playbackPosition.value)
                } else {
                    mediaPlayer?.start()
                }
                triggerServiceStart()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        _playbackPosition.value = positionMs
        val song = _currentSong.value
        if (song != null && !song.filePath.startsWith("virtual_track_")) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    mediaPlayer?.seekTo(positionMs, MediaPlayer.SEEK_CLOSEST)
                } else {
                    @Suppress("DEPRECATION")
                    mediaPlayer?.seekTo(positionMs.toInt())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            if (_currentSong.value != null) {
                resume()
            } else {
                // If queue has items, play first item
                val queue = _currentQueue.value
                if (queue.isNotEmpty()) {
                    playSong(queue.first())
                }
            }
        }
    }

    fun next() {
        val queue = _currentQueue.value
        val current = _currentSong.value ?: return
        if (queue.isEmpty()) return

        var index = queue.indexOfFirst { it.filePath == current.filePath }
        if (index == -1) {
            playSong(queue.first())
            return
        }

        if (_shuffleMode.value) {
            val nextIndex = (queue.indices).random()
            playSong(queue[nextIndex])
        } else {
            index++
            if (index < queue.size) {
                playSong(queue[index])
            } else {
                if (_repeatMode.value == RepeatMode.ALL) {
                    playSong(queue.first())
                } else {
                    stopPlayback()
                }
            }
        }
    }

    fun previous() {
        val queue = _currentQueue.value
        val current = _currentSong.value ?: return
        if (queue.isEmpty()) return

        var index = queue.indexOfFirst { it.filePath == current.filePath }
        if (index == -1) {
            playSong(queue.first())
            return
        }

        if (_playbackPosition.value > 3000) {
            seekTo(0)
            return
        }

        index--
        if (index >= 0) {
            playSong(queue[index])
        } else {
            if (_repeatMode.value == RepeatMode.ALL) {
                playSong(queue.last())
            } else {
                seekTo(0)
            }
        }
    }

    fun toggleShuffle() {
        _shuffleMode.value = !_shuffleMode.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.NONE -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.NONE
        }
    }

    fun setSleepTimer(minutes: Int) {
        val mode = when (minutes) {
            10 -> SleepTimerMode.MINUTES_10
            20 -> SleepTimerMode.MINUTES_20
            30 -> SleepTimerMode.MINUTES_30
            60 -> SleepTimerMode.MINUTES_60
            else -> SleepTimerMode.OFF
        }
        setSleepTimerMode(mode)
    }

    fun setSleepTimerMode(mode: SleepTimerMode) {
        sleepTimerJob?.cancel()
        _sleepTimerMode.value = mode
        _sleepTimeRemaining.value = 0L

        when (mode) {
            SleepTimerMode.OFF -> {
                // Stopped
            }
            SleepTimerMode.MINUTES_10 -> startSleepTimerCountdown(10 * 60 * 1000L)
            SleepTimerMode.MINUTES_20 -> startSleepTimerCountdown(20 * 60 * 1000L)
            SleepTimerMode.MINUTES_30 -> startSleepTimerCountdown(30 * 60 * 1000L)
            SleepTimerMode.MINUTES_60 -> startSleepTimerCountdown(60 * 60 * 1000L)
            SleepTimerMode.END_OF_SONG -> {
                // Done in handleSongCompletion
            }
            SleepTimerMode.END_OF_PLAYLIST -> {
                // Done in handleSongCompletion
            }
        }
    }

    private fun startSleepTimerCountdown(totalMs: Long) {
        _sleepTimeRemaining.value = totalMs
        sleepTimerJob = scope.launch(Dispatchers.Main) {
            var remaining = totalMs
            val step = 1000L
            while (remaining > 0) {
                delay(step)
                remaining -= step
                val currentRemaining = remaining.coerceAtLeast(0L)
                _sleepTimeRemaining.value = currentRemaining
                if (currentRemaining <= 0) {
                    fadeOutAndStop()
                    _sleepTimerMode.value = SleepTimerMode.OFF
                    break
                }
            }
        }
    }

    private fun fadeOutAndStop() {
        scope.launch(Dispatchers.Main) {
            try {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    var vol = 1.0f
                    while (vol > 0f) {
                        vol -= 0.1f
                        val coercedVol = vol.coerceAtLeast(0f)
                        mp.setVolume(coercedVol, coercedVol)
                        delay(200)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pause()
                try {
                    mediaPlayer?.setVolume(1.0f, 1.0f)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun handleSongCompletion() {
        scope.launch {
            if (_sleepTimerMode.value == SleepTimerMode.END_OF_SONG) {
                _sleepTimerMode.value = SleepTimerMode.OFF
                fadeOutAndStop()
                return@launch
            }

            if (_sleepTimerMode.value == SleepTimerMode.END_OF_PLAYLIST) {
                val currentQueueValue = _currentQueue.value
                val playingSong = _currentSong.value
                if (playingSong != null && currentQueueValue.isNotEmpty()) {
                    val isLast = currentQueueValue.lastOrNull()?.filePath == playingSong.filePath
                    if (isLast) {
                        _sleepTimerMode.value = SleepTimerMode.OFF
                        fadeOutAndStop()
                        return@launch
                    }
                }
            }

            when (_repeatMode.value) {
                RepeatMode.ONE -> {
                    _currentSong.value?.let { playSong(it) }
                }
                else -> {
                    next()
                }
            }
        }
    }

    private fun stopPlayback() {
        _isPlaying.value = false
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        stopSynthStream()
    }

    // --- CYBERPUNK PREMIUM THEME SOUND CHORD SYNTHESIS (PCM AUDIO STREAM) ---
    private fun startSynthesizer(song: Song, resumeFromPosition: Long = 0L) {
        stopSynthStream()
        if (requestAudioFocus()) {
            _isPlaying.value = true
            _playbackPosition.value = resumeFromPosition

            synthThread = scope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            var track: AudioTrack? = null
            
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                if (bufferSize > 0) {
                    track = AudioTrack.Builder()
                        .setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setSampleRate(sampleRate)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                    synthTrack = track
                    track.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                track = null
                synthTrack = null
            }

            // High-fidelity synthwave ambient melodies
            // Let's create an elegant chord loop in key of G minor / G# minor (highly cinematic-futuristic)
            val baseNotes = when (song.id) {
                "virtual_1" -> doubleArrayOf(261.63, 329.63, 392.00, 493.88) // C maj 7 vibe
                "virtual_2" -> doubleArrayOf(220.00, 261.63, 329.63, 392.00) // A min 7 vibe
                "virtual_3" -> doubleArrayOf(207.65, 246.94, 311.13, 415.30) // G# min
                "virtual_4" -> doubleArrayOf(196.00, 233.08, 293.66, 349.23) // G min 7
                else -> doubleArrayOf(174.61, 220.00, 261.63, 329.63) // F maj 7
            }

            val buffer = ShortArray(1024)
            var currentIdx = 0L
            val chunkDurationMs = (1024 * 1000L) / sampleRate // Approx 23ms
            var lastChunkTime = System.currentTimeMillis()

            while (isActive && _isPlaying.value) {
                // Synthesizing peaceful synth waves
                // Harmonizing notes cycling over time
                val noteDurationSamples = sampleRate * 1.5 // 1.5 seconds per note change
                val currentNotePeriod = (currentIdx / noteDurationSamples).toInt()
                val activeFreq = baseNotes[currentNotePeriod % baseNotes.size]

                for (i in buffer.indices) {
                    val angle = 2.0 * Math.PI * activeFreq * (currentIdx + i) / sampleRate
                    // Warm atmospheric pulse + organic subharmonic
                    val sample = (0.35 * sin(angle) + 0.15 * sin(angle * 0.5)) * Short.MAX_VALUE
                    buffer[i] = sample.toInt().toShort()
                }

                var written = 0
                if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                    try {
                        written = track.write(buffer, 0, buffer.size)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (written <= 0) {
                    // Safe software timing simulation when hardware is missing/restricted, or system audio is busy
                    delay(chunkDurationMs)
                } else {
                    // Even if we successfully wrote to hardware, enforce real-time pacing 
                    // to prevent ultra-fast CPU-burning loops in emulators/environments with non-blocking audio devices
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastChunkTime
                    val remainingSleep = chunkDurationMs - elapsed
                    if (remainingSleep > 0) {
                        delay(remainingSleep)
                    } else {
                        yield()
                    }
                }
                lastChunkTime = System.currentTimeMillis()
                
                currentIdx += buffer.size

                // Emulate visual progress
                val msAdded = (buffer.size.toDouble() / sampleRate * 1000).toLong()
                _playbackPosition.value = (_playbackPosition.value + msAdded).coerceAtMost(_playbackDuration.value)

                if (_playbackPosition.value >= _playbackDuration.value) {
                    withContext(Dispatchers.Main) {
                        handleSongCompletion()
                    }
                    break
                }
            }

            try {
                track?.stop()
                track?.release()
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    } else {
        _isPlaying.value = false
    }
}

    private fun stopSynthStream() {
        synthThread?.cancel()
        synthThread = null
        try {
            synthTrack?.stop()
            synthTrack?.release()
        } catch (e: Exception) {
            // Ignored
        }
        synthTrack = null
    }

    // Progress updater loop
    private fun startProgressTracker() {
        progressTrackingJob = scope.launch {
            while (isActive) {
                val song = _currentSong.value
                val playing = _isPlaying.value
                if (playing && song != null && !song.filePath.startsWith("virtual_track_")) {
                    mediaPlayer?.let { mp ->
                        try {
                            _playbackPosition.value = mp.currentPosition.toLong()
                        } catch (e: Exception) {
                            // Safe error suppression
                        }
                    }
                }
                delay(250)
            }
        }
    }

    // Synthesized Visualizer waveforms looking futuristic
    private fun startVisualizerGenerator() {
        visualizerJob = scope.launch {
            while (isActive) {
                if (_isPlaying.value) {
                    // Update visualizer heights dynamically in a fluid wave form!
                    val centerFreq = (System.currentTimeMillis() / 150.0)
                    val generatedFloats = FloatArray(24) { i ->
                        val waveOffset = sin(centerFreq + i * 0.5).toFloat()
                        val noise = (0.1f + 0.8f * (waveOffset + 1f) / 2f) * (0.4f + 0.6f * (Math.random().toFloat()))
                        noise.coerceIn(0.1f, 1.0f)
                    }
                    _visualizerAmplitudes.value = generatedFloats
                } else {
                    // Slow idle breath pulse effect
                    val basePulse = (0.12f + 0.05f * sin(System.currentTimeMillis() / 800.0).toFloat())
                    val idleFloats = FloatArray(24) { basePulse }
                    if (!idleFloats.contentEquals(_visualizerAmplitudes.value)) {
                        _visualizerAmplitudes.value = idleFloats
                    }
                }
                delay(80)
            }
        }
    }

    fun release() {
        sleepTimerJob?.cancel()
        stopPlayback()
        scope.cancel()
    }
}
