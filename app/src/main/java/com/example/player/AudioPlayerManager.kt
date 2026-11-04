package com.example.player

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.app.NotificationCompat
import android.os.PowerManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import android.widget.RemoteViews
import com.example.domain.model.AudioRepeatMode
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.Song
import com.example.domain.model.Video
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayerManager(private val context: Context) {
    var isAppInForeground = true
    companion object {
        var activeInstance: AudioPlayerManager? = null
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ArvexaPlayer:AudioWakeLock")
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    val exoPlayer: ExoPlayer by lazy {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setEnableDecoderFallback(true)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(AudioRepeatMode.OFF)
    val repeatMode: StateFlow<AudioRepeatMode> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _isFullPlayerOpen = MutableStateFlow(false)
    val isFullPlayerOpen: StateFlow<Boolean> = _isFullPlayerOpen.asStateFlow()

    private val _selectedPreset = MutableStateFlow("Balanced")
    val selectedPreset: StateFlow<String> = _selectedPreset.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val equalizerPresets = listOf(
        EqualizerPreset("flat", "Balanced", 0f, 0f, 0f),
        EqualizerPreset("bass_boost", "Bass Boost", 6f, 0f, -2f),
        EqualizerPreset("vocal", "Vocal Booster", -2f, 5f, 2f),
        EqualizerPreset("rock", "Rock & Metal", 4f, -1f, 5f),
        EqualizerPreset("pop", "Pop & Beats", 3f, 2f, 3f),
        EqualizerPreset("electronic", "Electronic / EDM", 5f, 1f, 4f),
        EqualizerPreset("classical", "Acoustic / Classical", 2f, 1f, 3f),
        EqualizerPreset("treble", "Treble Boost", -2f, 1f, 6f)
    )

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    val mediaSession = MediaSessionCompat(context, "ArvexaAudioSession").apply {
        isActive = true
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { play() }
            override fun onPause() { pause() }
            override fun onSkipToNext() { nextSong() }
            override fun onSkipToPrevious() { previousSong() }
            override fun onSeekTo(pos: Long) { seekTo(pos) }
        })
    }

    // Callback to stop video playback when audio starts
    var onAudioStarted: (() -> Unit)? = null
    var onToggleFavorite: ((Song) -> Unit)? = null

    fun toggleFavorite(song: Song) {
        onToggleFavorite?.invoke(song)
        val updatedSong = song.copy(isFavorite = !song.isFavorite)
        if (_currentSong.value?.id == song.id) {
            _currentSong.value = updatedSong
        }
        _queue.value = _queue.value.map { if (it.id == song.id) updatedSong else it }
    }

    fun playNext() {
        nextSong()
    }

    init {
        activeInstance = this
        createNotificationChannel()
        startProgressTracker()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "media_playback_channel",
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music and audio playback"
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateMediaNotification() {
        val song = _currentSong.value ?: return
        val isPlaying = _isPlaying.value

        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_REWIND or
                PlaybackStateCompat.ACTION_STOP

        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, _currentPositionMs.value, _playbackSpeed.value)
                .setActions(actions)
                .build()
        )

        val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
        val rawBitmap = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, albumArtUri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, albumArtUri)
            }
        } catch (_: Exception) {
            null
        }

        val bitmap: Bitmap? = if (rawBitmap != null) {
            try {
                Bitmap.createScaledBitmap(rawBitmap, 128, 128, true)
            } catch (_: Exception) {
                null
            }
        } else null

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.durationMs)

        if (bitmap != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
        }
        mediaSession.setMetadata(metadataBuilder.build())

        MediaPlaybackService.updateNotification(context)
    }

    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(context, MediaActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.playbackState == Player.STATE_BUFFERING) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)

                    _currentPositionMs.value = pos
                    if (dur > 0L) {
                        _durationMs.value = dur
                    }
                    _bufferedPositionMs.value = buf
                }
                delay(200)
            }
        }
    }

    fun playSong(song: Song, playlist: List<Song> = emptyList(), startIndex: Int = -1) {
        onAudioStarted?.invoke()
        _errorMessage.value = null

        val currentId = _currentSong.value?.id
        if (currentId == song.id && exoPlayer.playbackState != Player.STATE_IDLE) {
            val actualQueue = if (playlist.isNotEmpty()) playlist else _queue.value
            if (actualQueue.isNotEmpty()) {
                _queue.value = actualQueue
                val index = if (startIndex >= 0 && startIndex < actualQueue.size) {
                    startIndex
                } else {
                    actualQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(_queueIndex.value)
                }
                _queueIndex.value = index
            }
            if (!exoPlayer.isPlaying) {
                exoPlayer.play()
                _isPlaying.value = true
                updateMediaNotification()
            }
            return
        }

        _currentSong.value = song

        val actualQueue = if (playlist.isNotEmpty()) playlist else listOf(song)
        _queue.value = actualQueue
        val index = if (startIndex >= 0 && startIndex < actualQueue.size) {
            startIndex
        } else {
            actualQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        }
        _queueIndex.value = index

        try {
            NovaPlayerManager.activeInstance?.let {
                if (it.isPlaying.value) {
                    it.pause()
                }
                it.mediaSession.isActive = false
            }
            mediaSession.isActive = true

            acquireWakeLock()
            val mediaItem = MediaItem.fromUri(Uri.parse(song.uri))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            _isPlaying.value = true
            _durationMs.value = song.durationMs
            _currentPositionMs.value = 0L
            MediaPlaybackService.startService(context)
            updateMediaNotification()
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load track: ${e.localizedMessage}"
            releaseWakeLock()
        }
    }

    fun play() {
        onAudioStarted?.invoke()
        NovaPlayerManager.activeInstance?.let {
            if (it.isPlaying.value) {
                it.pause()
            }
            it.mediaSession.isActive = false
        }
        mediaSession.isActive = true

        acquireWakeLock()
        exoPlayer.play()
        _isPlaying.value = true
        MediaPlaybackService.startService(context)
        updateMediaNotification()
    }

    fun pause() {
        exoPlayer.pause()
        _isPlaying.value = false
        releaseWakeLock()
        updateMediaNotification()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val bounded = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        exoPlayer.seekTo(bounded)
        _currentPositionMs.value = bounded
    }

    fun seekBy(deltaMs: Long) {
        val target = (_currentPositionMs.value + deltaMs).coerceIn(0L, _durationMs.value.coerceAtLeast(1L))
        seekTo(target)
    }

    fun nextSong() {
        val q = _queue.value
        if (q.isNotEmpty()) {
            val nextIdx = (_queueIndex.value + 1) % q.size
            _queueIndex.value = nextIdx
            playSong(q[nextIdx], q, nextIdx)
        }
    }

    fun previousSong() {
        val q = _queue.value
        if (q.isNotEmpty()) {
            // If current position is > 3 seconds, replay current song first
            if (_currentPositionMs.value > 3000L) {
                seekTo(0L)
            } else {
                val prevIdx = if (_queueIndex.value - 1 < 0) q.size - 1 else _queueIndex.value - 1
                _queueIndex.value = prevIdx
                playSong(q[prevIdx], q, prevIdx)
            }
        }
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        exoPlayer.shuffleModeEnabled = next
    }

    fun cycleRepeatMode() {
        val next = when (_repeatMode.value) {
            AudioRepeatMode.OFF -> AudioRepeatMode.ALL
            AudioRepeatMode.ALL -> AudioRepeatMode.ONE
            AudioRepeatMode.ONE -> AudioRepeatMode.OFF
        }
        _repeatMode.value = next
        exoPlayer.repeatMode = when (next) {
            AudioRepeatMode.OFF -> Player.REPEAT_MODE_OFF
            AudioRepeatMode.ALL -> Player.REPEAT_MODE_ALL
            AudioRepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.setPlaybackSpeed(speed)
    }

    private var equalizer: android.media.audiofx.Equalizer? = null
    private var bassBoost: android.media.audiofx.BassBoost? = null
    private var virtualizer: android.media.audiofx.Virtualizer? = null
    private var activeAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private val _isEqualizerEnabled = MutableStateFlow(false)
    val isEqualizerEnabled: StateFlow<Boolean> = _isEqualizerEnabled.asStateFlow()

    private val _bandLevels = MutableStateFlow(listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f))
    val bandLevels: StateFlow<List<Float>> = _bandLevels.asStateFlow()

    private val _customBandLevels = MutableStateFlow(listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f))
    val customBandLevels: StateFlow<List<Float>> = _customBandLevels.asStateFlow()

    private val _bassBoostLevel = MutableStateFlow(0.5f)
    val bassBoostLevel: StateFlow<Float> = _bassBoostLevel.asStateFlow()

    private val _virtualizerLevel = MutableStateFlow(0.5f)
    val virtualizerLevel: StateFlow<Float> = _virtualizerLevel.asStateFlow()

    fun setEqualizerEnabled(enabled: Boolean) {
        _isEqualizerEnabled.value = enabled
        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (enabled) {
                if (audioSessionId != android.media.audiofx.AudioEffect.ERROR_BAD_VALUE && audioSessionId != 0) {
                    rebindAudioEffects(audioSessionId)
                } else {
                    rebindAudioEffects(0)
                }
            } else {
                equalizer?.enabled = false
                bassBoost?.enabled = false
                virtualizer?.enabled = false
            }
        } catch (_: Exception) {}
    }

    private fun rebindAudioEffects(sessionId: Int) {
        if (sessionId == activeAudioSessionId && equalizer != null && _isEqualizerEnabled.value) {
            applyCustomEqualizer()
            applyBassBoost(_bassBoostLevel.value)
            applyVirtualizer(_virtualizerLevel.value)
            return
        }
        
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        activeAudioSessionId = sessionId

        if (_isEqualizerEnabled.value && sessionId != android.media.audiofx.AudioEffect.ERROR_BAD_VALUE) {
            try {
                equalizer = android.media.audiofx.Equalizer(0, sessionId).apply { enabled = true }
                bassBoost = android.media.audiofx.BassBoost(0, sessionId).apply { enabled = _bassBoostLevel.value > 0f }
                virtualizer = android.media.audiofx.Virtualizer(0, sessionId).apply { enabled = _virtualizerLevel.value > 0f }

                applyCustomEqualizer()
                applyBassBoost(_bassBoostLevel.value)
                applyVirtualizer(_virtualizerLevel.value)
            } catch (_: Exception) {}
        }
    }

    fun setPreset(name: String) {
        _selectedPreset.value = name
        when (name) {
            "Custom" -> {
                _bandLevels.value = _customBandLevels.value
            }
            "Normal", "Flat", "Balanced" -> { _bandLevels.value = listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f); _bassBoostLevel.value = 0.0f; _virtualizerLevel.value = 0.0f }
            "Pop", "Pop & Beats" -> { _bandLevels.value = listOf(0.58f, 0.65f, 0.52f, 0.64f, 0.72f); _bassBoostLevel.value = 0.30f; _virtualizerLevel.value = 0.20f }
            "Rock", "Rock & Metal" -> { _bandLevels.value = listOf(0.74f, 0.62f, 0.46f, 0.68f, 0.78f); _bassBoostLevel.value = 0.40f; _virtualizerLevel.value = 0.25f }
            "Jazz", "Acoustic / Classical" -> { _bandLevels.value = listOf(0.58f, 0.54f, 0.48f, 0.62f, 0.70f); _bassBoostLevel.value = 0.20f; _virtualizerLevel.value = 0.30f }
            "Bass Boost", "Super Bass" -> { _bandLevels.value = listOf(0.85f, 0.72f, 0.50f, 0.48f, 0.52f); _bassBoostLevel.value = 0.60f; _virtualizerLevel.value = 0.15f }
            "Vocal", "Vocal Booster" -> { _bandLevels.value = listOf(0.35f, 0.45f, 0.85f, 0.78f, 0.55f); _bassBoostLevel.value = 0.10f; _virtualizerLevel.value = 0.15f }
            "Heavy Metal", "Electronic / EDM" -> { _bandLevels.value = listOf(0.82f, 0.70f, 0.45f, 0.65f, 0.82f); _bassBoostLevel.value = 0.50f; _virtualizerLevel.value = 0.35f }
            "Treble Boost" -> { _bandLevels.value = listOf(0.32f, 0.40f, 0.55f, 0.78f, 0.90f); _bassBoostLevel.value = 0.0f; _virtualizerLevel.value = 0.10f }
        }
        if (_isEqualizerEnabled.value) {
            applyCustomEqualizer()
            applyBassBoost(_bassBoostLevel.value)
            applyVirtualizer(_virtualizerLevel.value)
        }
    }

    fun setBandLevel(bandIndex: Int, value: Float) {
        _selectedPreset.value = "Custom"
        val currentBands = _bandLevels.value.toMutableList()
        if (bandIndex in 0..4) {
            currentBands[bandIndex] = value
            _bandLevels.value = currentBands
            _customBandLevels.value = currentBands
        }
        if (_isEqualizerEnabled.value) {
            applyCustomEqualizer()
        }
    }

    fun setBassBoost(value: Float) {
        _bassBoostLevel.value = value
        if (_isEqualizerEnabled.value) {
            applyBassBoost(value)
        }
    }

    fun setVirtualizer(value: Float) {
        _virtualizerLevel.value = value
        if (_isEqualizerEnabled.value) {
            applyVirtualizer(value)
        }
    }

    private fun interpolateBandLevel(bandLevels: List<Float>, normPos: Float): Float {
        if (bandLevels.isEmpty()) return 0.5f
        if (bandLevels.size == 1) return bandLevels[0]
        val scaledIndex = normPos * (bandLevels.size - 1)
        val lowerIdx = scaledIndex.toInt().coerceIn(0, bandLevels.size - 1)
        val upperIdx = (lowerIdx + 1).coerceIn(0, bandLevels.size - 1)
        val fraction = scaledIndex - lowerIdx
        return bandLevels[lowerIdx] * (1f - fraction) + bandLevels[upperIdx] * fraction
    }

    private fun applyCustomEqualizer() {
        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (audioSessionId != android.media.audiofx.AudioEffect.ERROR_BAD_VALUE) {
                if (equalizer == null && _isEqualizerEnabled.value) {
                    rebindAudioEffects(audioSessionId)
                }
                equalizer?.enabled = _isEqualizerEnabled.value
                val numBands = equalizer?.numberOfBands ?: 0
                if (numBands > 0 && _isEqualizerEnabled.value) {
                    val minLevel = equalizer?.bandLevelRange?.get(0) ?: -1500
                    val maxLevel = equalizer?.bandLevelRange?.get(1) ?: 1500
                    val bandLevels = _bandLevels.value

                    // Smooth boost headroom (65% boost, 80% cut) to give warm acoustic response without clipping
                    val maxBoost = (maxLevel * 0.65f)
                    val maxCut = (minLevel * 0.80f)

                    for (i in 0 until numBands) {
                        val normPos = if (numBands > 1) i.toFloat() / (numBands - 1) else 0.5f
                        val floatVal = interpolateBandLevel(bandLevels, normPos)
                        val millibels = if (floatVal >= 0.5f) {
                            (0 + maxBoost * ((floatVal - 0.5f) * 2f)).toInt().toShort()
                        } else {
                            (maxCut + (0 - maxCut) * (floatVal * 2f)).toInt().toShort()
                        }
                        equalizer?.setBandLevel(i.toShort(), millibels.coerceIn(minLevel, maxLevel))
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun applyBassBoost(value: Float) {
        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (audioSessionId != android.media.audiofx.AudioEffect.ERROR_BAD_VALUE) {
                if (bassBoost == null && _isEqualizerEnabled.value) {
                    rebindAudioEffects(audioSessionId)
                }
                bassBoost?.enabled = _isEqualizerEnabled.value && value > 0f
                if (bassBoost?.strengthSupported == true) {
                    // Safe strength range (max 650/1000) for deep rich punch without volume distortion
                    val strength = (value * 650).toInt().toShort().coerceIn(0, 650)
                    bassBoost?.setStrength(strength)
                }
            }
        } catch (_: Exception) {}
    }

    private fun applyVirtualizer(value: Float) {
        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (audioSessionId != android.media.audiofx.AudioEffect.ERROR_BAD_VALUE) {
                if (virtualizer == null && _isEqualizerEnabled.value) {
                    rebindAudioEffects(audioSessionId)
                }
                virtualizer?.enabled = _isEqualizerEnabled.value && value > 0f
                if (virtualizer?.strengthSupported == true) {
                    val strength = (value * 750).toInt().toShort().coerceIn(0, 750)
                    virtualizer?.setStrength(strength)
                }
            }
        } catch (_: Exception) {}
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes != null && minutes > 0) {
            sleepTimerJob = scope.launch {
                var remainingSec = minutes * 60
                while (remainingSec > 0) {
                    delay(1000)
                    remainingSec--
                    _sleepTimerMinutes.value = (remainingSec / 60) + 1
                }
                pause()
                _sleepTimerMinutes.value = null
            }
        }
    }

    fun openFullPlayer() {
        _isFullPlayerOpen.value = true
    }

    fun closeFullPlayer() {
        _isFullPlayerOpen.value = false
    }

    fun stopAndDismiss() {
        releaseWakeLock()
        try {
            mediaSession.isActive = false
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        } catch (_: Exception) {}
        _currentSong.value = null
        _isPlaying.value = false
        _isFullPlayerOpen.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        if (NovaPlayerManager.activeInstance?.isPlaying?.value != true) {
            MediaPlaybackService.stopService(context)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
            if (playbackState == Player.STATE_READY) {
                _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                updateMediaNotification()
            }
            if (playbackState == Player.STATE_ENDED) {
                when (_repeatMode.value) {
                    AudioRepeatMode.ONE -> {
                        seekTo(0L)
                        play()
                    }
                    AudioRepeatMode.ALL -> {
                        nextSong()
                    }
                    AudioRepeatMode.OFF -> {
                        val q = _queue.value
                        if (_queueIndex.value < q.size - 1) {
                            nextSong()
                        } else {
                            _isPlaying.value = false
                            seekTo(0L)
                            updateMediaNotification()
                        }
                    }
                }
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                rebindAudioEffects(audioSessionId)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying && _isEqualizerEnabled.value && equalizer == null) {
                rebindAudioEffects(exoPlayer.audioSessionId)
            }
            updateMediaNotification()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateMediaNotification()
        }

        override fun onPlayerError(error: PlaybackException) {
            _errorMessage.value = "Audio error: ${error.localizedMessage ?: error.errorCodeName}"
            _isBuffering.value = false
        }
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        try {
            equalizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
