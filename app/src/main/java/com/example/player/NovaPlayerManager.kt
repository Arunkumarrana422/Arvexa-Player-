package com.example.player

import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.app.NotificationCompat
import android.media.AudioManager
import android.net.Uri
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.os.PowerManager
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.domain.model.AspectRatioMode
import com.example.domain.model.AudioTrack
import com.example.domain.model.DecoderMode
import com.example.domain.model.SubtitleTrack
import com.example.domain.model.Video
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NovaPlayerManager(private val context: Context) {
    var isAppInForeground = true
    companion object {
        var activeInstance: NovaPlayerManager? = null
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NovaPlayer:VideoWakeLock")
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

    init {
        scope.launch {
            try {
                val settings = SettingsRepository(context).settingsFlow.first()
                _aspectRatioMode.value = settings.defaultAspectRatio
            } catch (_: Exception) {}
        }
    }

    private val trackSelector = DefaultTrackSelector(context)
    
    private val _decoderMode = MutableStateFlow(DecoderMode.HW_PLUS)
    val decoderMode: StateFlow<DecoderMode> = _decoderMode.asStateFlow()

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer
        get() {
            if (_exoPlayer == null) {
                _exoPlayer = buildExoPlayer(_decoderMode.value)
            }
            return _exoPlayer!!
        }

    private fun buildExoPlayer(mode: DecoderMode): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
            when (mode) {
                DecoderMode.HW -> {
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                }
                DecoderMode.HW_PLUS, DecoderMode.SW -> {
                    setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                }
            }
        }

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                addListener(playerListener)
            }
    }

    fun setDecoderMode(mode: DecoderMode) {
        if (_decoderMode.value == mode) return
        _decoderMode.value = mode
        try {
            if (_exoPlayer != null && !_exoPlayer!!.isPlaying) {
                _exoPlayer?.play()
            }
        } catch (_: Exception) {}
    }

    var onVideoStarted: (() -> Unit)? = null

    private val _currentVideo = MutableStateFlow<Video?>(null)
    val currentVideo: StateFlow<Video?> = _currentVideo.asStateFlow()

    private val _queue = MutableStateFlow<List<Video>>(emptyList())
    val queue: StateFlow<List<Video>> = _queue.asStateFlow()

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

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _volumeFraction = MutableStateFlow(0.8f)
    val volumeFraction: StateFlow<Float> = _volumeFraction.asStateFlow()

    private val _brightnessFraction = MutableStateFlow(0.7f)
    val brightnessFraction: StateFlow<Float> = _brightnessFraction.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _isRepeatOne = MutableStateFlow(false)
    val isRepeatOne: StateFlow<Boolean> = _isRepeatOne.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _availableAudioTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val availableAudioTracks: StateFlow<List<AudioTrack>> = _availableAudioTracks.asStateFlow()

    private val _availableSubtitleTracks = MutableStateFlow<List<SubtitleTrack>>(emptyList())
    val availableSubtitleTracks: StateFlow<List<SubtitleTrack>> = _availableSubtitleTracks.asStateFlow()

    private val _activeSubtitleText = MutableStateFlow<String?>(null)
    val activeSubtitleText: StateFlow<String?> = _activeSubtitleText.asStateFlow()

    private val _subtitleDelayMs = MutableStateFlow(0L)
    val subtitleDelayMs: StateFlow<Long> = _subtitleDelayMs.asStateFlow()

    private val _externalCues = MutableStateFlow<List<SubtitleCue>>(emptyList())

    private val _isMiniPlayerActive = MutableStateFlow(false)
    val isMiniPlayerActive: StateFlow<Boolean> = _isMiniPlayerActive.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var onProgressUpdate: ((video: Video, pos: Long, dur: Long) -> Unit)? = null

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    val mediaSession = MediaSessionCompat(context, "NovaVideoSession").apply {
        isActive = true
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { play() }
            override fun onPause() { pause() }
            override fun onSkipToNext() { nextVideo() }
            override fun onSkipToPrevious() { previousVideo() }
            override fun onSeekTo(pos: Long) { seekTo(pos) }
            override fun onFastForward() { seekBy(10000L) }
            override fun onRewind() { seekBy(-10000L) }
            override fun onStop() { stopAndDismiss() }
        })
    }

    init {
        activeInstance = this
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (maxVol > 0) {
            _volumeFraction.value = curVol.toFloat() / maxVol.toFloat()
        }
        startProgressTracker()
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
                    _durationMs.value = dur
                    _bufferedPositionMs.value = buf

                    _currentVideo.value?.let { video ->
                        onProgressUpdate?.invoke(video, pos, dur)
                    }

                    // Update external subtitle cues if any
                    val cues = _externalCues.value
                    if (cues.isNotEmpty()) {
                        _activeSubtitleText.value = SubtitleParser.getActiveCue(cues, pos, _subtitleDelayMs.value)
                    }
                }
                delay(200)
            }
        }
    }

    private fun getVideoThumbnail(video: Video): Bitmap? {
        return try {
            val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    context.contentResolver.loadThumbnail(Uri.parse(video.uri), android.util.Size(128, 128), null)
                } catch (_: Exception) {
                    @Suppress("DEPRECATION")
                    android.media.ThumbnailUtils.createVideoThumbnail(
                        video.path,
                        android.provider.MediaStore.Images.Thumbnails.MICRO_KIND
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                android.media.ThumbnailUtils.createVideoThumbnail(
                    video.path,
                    android.provider.MediaStore.Images.Thumbnails.MICRO_KIND
                )
            }
            if (raw != null) {
                Bitmap.createScaledBitmap(raw, 128, 128, true)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun playVideo(video: Video, playlist: List<Video> = emptyList(), startPositionMs: Long = 0L) {
        onVideoStarted?.invoke()
        _errorMessage.value = null
        _currentVideo.value = video
        val actualList = if (playlist.isNotEmpty()) playlist else listOf(video)
        _queue.value = actualList
        val idx = actualList.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        _queueIndex.value = idx

        val resumePos = if (startPositionMs > 0) startPositionMs else video.lastPositionMs
        _currentPositionMs.value = resumePos
        _durationMs.value = video.durationMs

        val mediaItem = MediaItem.fromUri(Uri.parse(video.uri))
        if (resumePos > 0) {
            exoPlayer.setMediaItem(mediaItem, resumePos)
        } else {
            exoPlayer.setMediaItem(mediaItem)
        }
        exoPlayer.prepare()
        if (resumePos > 0) {
            exoPlayer.seekTo(resumePos)
        }

        AudioPlayerManager.activeInstance?.let {
            if (it.isPlaying.value) {
                it.pause()
            }
            it.mediaSession.isActive = false
        }
        mediaSession.isActive = true

        acquireWakeLock()
        exoPlayer.playWhenReady = true
        _isPlaying.value = true
        _isMiniPlayerActive.value = false
        onProgressUpdate?.invoke(video, if (resumePos > 0) resumePos else 1L, video.durationMs)
        MediaPlaybackService.startService(context)
        updateVideoNotification()
    }

    fun flushProgress() {
        _currentVideo.value?.let { video ->
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            if (pos > 0L) {
                onProgressUpdate?.invoke(video, pos, dur)
            }
        }
    }

    fun play() {
        AudioPlayerManager.activeInstance?.let {
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
        updateVideoNotification()
    }

    fun pause() {
        flushProgress()
        exoPlayer.pause()
        _isPlaying.value = false
        releaseWakeLock()
        updateVideoNotification()
    }

    fun stopAndDismiss() {
        flushProgress()
        releaseWakeLock()
        try {
            mediaSession.isActive = false
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        } catch (_: Exception) {}
        _currentVideo.value = null
        _isPlaying.value = false
        _isMiniPlayerActive.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        if (AudioPlayerManager.activeInstance?.isPlaying?.value != true) {
            MediaPlaybackService.stopService(context)
        }
    }

    fun updateVideoNotification() {
        val video = _currentVideo.value ?: return
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

        val thumbBitmap = getVideoThumbnail(video)
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, video.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, video.folderName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, video.folderName)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, video.title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, video.folderName)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, video.durationMs)

        if (thumbBitmap != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, thumbBitmap)
        }
        mediaSession.setMetadata(metadataBuilder.build())

        MediaPlaybackService.updateNotification(context)
    }

    private fun createVideoActionPendingIntent(action: String): PendingIntent {
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

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.setPlaybackSpeed(speed)
    }

    fun setVolume(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        _volumeFraction.value = clamped
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVol = (clamped * maxVol).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
    }

    fun adjustVolumeBy(deltaFraction: Float) {
        setVolume(_volumeFraction.value + deltaFraction)
    }

    fun setBrightness(fraction: Float, activity: Activity? = null) {
        val clamped = fraction.coerceIn(0.01f, 1.0f)
        _brightnessFraction.value = clamped
        scope.launch {
            try {
                SettingsRepository(context).setVideoBrightness(clamped)
            } catch (_: Exception) {}
        }
        activity?.let {
            val lp = it.window.attributes
            lp.screenBrightness = clamped
            it.window.attributes = lp
        }
    }

    fun restoreSystemBrightness(activity: Activity?) {
        activity?.let {
            val lp = it.window.attributes
            lp.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            it.window.attributes = lp
        }
    }

    fun applyPlayerBrightness(activity: Activity?) {
        activity?.let {
            val lp = it.window.attributes
            lp.screenBrightness = _brightnessFraction.value.coerceIn(0.01f, 1.0f)
            it.window.attributes = lp
        }
    }

    fun adjustBrightnessBy(deltaFraction: Float, activity: Activity? = null) {
        setBrightness(_brightnessFraction.value + deltaFraction, activity)
    }

    fun setLocked(locked: Boolean) {
        _isLocked.value = locked
    }

    fun cycleAspectRatio() {
        val modes = AspectRatioMode.values()
        val currentIdx = modes.indexOf(_aspectRatioMode.value)
        val nextMode = modes[(currentIdx + 1) % modes.size]
        _aspectRatioMode.value = nextMode
        scope.launch {
            try {
                SettingsRepository(context).setDefaultAspectRatio(nextMode)
            } catch (_: Exception) {}
        }
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
        scope.launch {
            try {
                SettingsRepository(context).setDefaultAspectRatio(mode)
            } catch (_: Exception) {}
        }
    }

    fun nextVideo() {
        val q = _queue.value
        if (q.isNotEmpty()) {
            val nextIdx = (_queueIndex.value + 1) % q.size
            _queueIndex.value = nextIdx
            playVideo(q[nextIdx], q, 0L)
        }
    }

    fun previousVideo() {
        val q = _queue.value
        if (q.isNotEmpty()) {
            val prevIdx = if (_queueIndex.value - 1 < 0) q.size - 1 else _queueIndex.value - 1
            _queueIndex.value = prevIdx
            playVideo(q[prevIdx], q, 0L)
        }
    }

    fun toggleRepeat() {
        val next = !_isRepeatOne.value
        _isRepeatOne.value = next
        exoPlayer.repeatMode = if (next) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        exoPlayer.shuffleModeEnabled = next
    }

    fun selectAudioTrack(track: AudioTrack) {
        // If track is a multi-channel format like DDP / EAC3 / AC3 / DTS and mode is HW, auto-switch to HW+ for extension decoding support
        if ((track.label.contains("DDP", true) || track.label.contains("AC3", true) || track.label.contains("EAC3", true) || track.label.contains("DTS", true)) && _decoderMode.value == DecoderMode.HW) {
            setDecoderMode(DecoderMode.HW_PLUS)
        }

        val tracks = exoPlayer.currentTracks
        for (groupIndex in 0 until tracks.groups.size) {
            val trackGroup = tracks.groups[groupIndex]
            if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until trackGroup.length) {
                    val trackId = "audio_${groupIndex}_$i"
                    val format = trackGroup.getTrackFormat(i)
                    if (trackId == track.id || format.id == track.id || (format.language == track.language && track.id.contains("_$i"))) {
                        val override = TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(override)
                            .build()
                        break
                    }
                }
            }
        }
        try {
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (_: Exception) {}
        updateAvailableTracks(exoPlayer.currentTracks)
    }

    fun selectSubtitleTrack(subtitle: SubtitleTrack?) {
        if (subtitle == null) {
            // Disable subtitles
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            )
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .build()
            _externalCues.value = emptyList()
            _activeSubtitleText.value = null
        } else if (subtitle.isExternal) {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            )
        } else {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            )
            val tracks = exoPlayer.currentTracks
            for (groupIndex in 0 until tracks.groups.size) {
                val trackGroup = tracks.groups[groupIndex]
                if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                    for (i in 0 until trackGroup.length) {
                        val trackId = "sub_${groupIndex}_$i"
                        val format = trackGroup.getTrackFormat(i)
                        if (trackId == subtitle.id || format.id == subtitle.id || (format.language == subtitle.language && subtitle.id.contains("_$i"))) {
                            val override = TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(override)
                                .build()
                            break
                        }
                    }
                }
            }
        }
        try {
            exoPlayer.prepare()
        } catch (_: Exception) {}
        updateAvailableTracks(exoPlayer.currentTracks)
    }

    suspend fun loadExternalSubtitle(uri: Uri) {
        val cues = SubtitleParser.parseFromUri(context, uri)
        _externalCues.value = cues
        val extTrack = SubtitleTrack(
            id = "ext_${System.currentTimeMillis()}",
            language = "External",
            label = "External Subtitle (${cues.size} lines)",
            uri = uri.toString(),
            isExternal = true,
            isSelected = true
        )
        _availableSubtitleTracks.value = _availableSubtitleTracks.value + extTrack
    }

    fun adjustSubtitleDelay(deltaMs: Long) {
        _subtitleDelayMs.value += deltaMs
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

    fun setMiniPlayer(active: Boolean) {
        _isMiniPlayerActive.value = active
    }

    private fun updateAvailableTracks(tracks: Tracks) {
        val audioList = mutableListOf<AudioTrack>()
        val subList = mutableListOf<SubtitleTrack>()

        for (groupIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[groupIndex]
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val label = format.label ?: format.language ?: "Audio Track ${audioList.size + 1}"
                    val trackId = "audio_${groupIndex}_$i"
                    audioList.add(
                        AudioTrack(
                            id = trackId,
                            label = "$label (${format.sampleMimeType ?: "Audio"})",
                            language = format.language ?: "und",
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val label = format.label ?: format.language ?: "Subtitle ${subList.size + 1}"
                    val trackId = "sub_${groupIndex}_$i"
                    subList.add(
                        SubtitleTrack(
                            id = trackId,
                            language = format.language ?: "und",
                            label = "$label (${format.language ?: "Text"})",
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
            }
        }

        _availableAudioTracks.value = audioList
        _availableSubtitleTracks.value = subList
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = (playbackState == Player.STATE_BUFFERING)
            if (playbackState == Player.STATE_READY) {
                _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                updateVideoNotification()
            }
            if (playbackState == Player.STATE_ENDED) {
                _isPlaying.value = false
                if (!_isRepeatOne.value) {
                    nextVideo()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            updateVideoNotification()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateVideoNotification()
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateAvailableTracks(tracks)
        }

        override fun onCues(cueGroup: CueGroup) {
            val text = cueGroup.cues.mapNotNull { it.text }.joinToString("\n")
            if (_externalCues.value.isEmpty()) {
                _activeSubtitleText.value = if (text.isNotBlank()) text else null
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
                // Software fallback is active and playback will continue normally. Ignore error banner.
                return
            }
            val message = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Network error: Unable to connect to streaming server. Check your connection."
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                    "Unsupported or corrupted video stream format."
                else -> error.localizedMessage ?: "Playback error occurred: ${error.errorCodeName}"
            }
            _errorMessage.value = message
            _isBuffering.value = false
        }
    }

    fun retryPlayback() {
        _errorMessage.value = null
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        try {
            _exoPlayer?.removeListener(playerListener)
            _exoPlayer?.release()
        } catch (_: Exception) {}
        _exoPlayer = null
    }
}
