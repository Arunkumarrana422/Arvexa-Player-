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
import androidx.media3.exoplayer.SeekParameters
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
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ArvexaPlayer:VideoWakeLock")
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

    private val _backgroundAudioEnabled = MutableStateFlow(false)
    val backgroundAudioEnabled: StateFlow<Boolean> = _backgroundAudioEnabled.asStateFlow()

    fun setBackgroundAudioEnabled(enabled: Boolean) {
        _backgroundAudioEnabled.value = enabled
        if (enabled) {
            if (_isPlaying.value && _currentVideo.value != null) {
                mediaSession.isActive = true
                MediaPlaybackService.startService(context)
                updateVideoNotification()
            }
        } else {
            dismissVideoNotification()
            if (!isAppInForeground && _isPlaying.value) {
                pause()
            }
        }
    }

    init {
        scope.launch {
            try {
                SettingsRepository(context).settingsFlow.collect { settings ->
                    val wasEnabled = _backgroundAudioEnabled.value
                    _backgroundAudioEnabled.value = settings.backgroundAudioEnabled
                    if (!settings.backgroundAudioEnabled) {
                        dismissVideoNotification()
                        if (!isAppInForeground && _isPlaying.value) {
                            pause()
                        }
                    } else if (!wasEnabled && _isPlaying.value && _currentVideo.value != null) {
                        mediaSession.isActive = true
                        MediaPlaybackService.startService(context)
                        updateVideoNotification()
                    }
                }
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
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        }

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000,
                50000,
                2500,
                5000
            )
            .build()

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        return ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setHandleAudioBecomingNoisy(true)
            .setSeekParameters(SeekParameters.EXACT)
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
    var playerViewBounds: android.graphics.Rect? = null

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

    private var isAudioFallbackAttempted = false
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var onProgressUpdate: ((video: Video, pos: Long, dur: Long) -> Unit)? = null

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    val mediaSession = MediaSessionCompat(context, "ArvexaVideoSession").apply {
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

    private val videoAspectPrefs by lazy {
        context.getSharedPreferences("video_aspect_ratios", Context.MODE_PRIVATE)
    }

    fun playVideo(video: Video, playlist: List<Video> = emptyList(), startPositionMs: Long = 0L) {
        onVideoStarted?.invoke()
        _errorMessage.value = null
        _currentVideo.value = video
        isAudioFallbackAttempted = false
        try {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            )
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .build()
        } catch (_: Exception) {}

        val savedModeName = videoAspectPrefs.getString("aspect_${video.id}", null)
        val savedMode = savedModeName?.let {
            try { AspectRatioMode.valueOf(it) } catch (_: Exception) { null }
        } ?: AspectRatioMode.FIT
        _aspectRatioMode.value = savedMode
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
        if (_backgroundAudioEnabled.value) {
            mediaSession.isActive = true
            MediaPlaybackService.startService(context)
            updateVideoNotification()
        } else {
            mediaSession.isActive = false
            dismissVideoNotification()
        }
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
        if (!isAppInForeground && !_backgroundAudioEnabled.value) {
            return
        }

        AudioPlayerManager.activeInstance?.let {
            if (it.isPlaying.value) {
                it.pause()
            }
            it.mediaSession.isActive = false
        }

        acquireWakeLock()
        exoPlayer.play()
        _isPlaying.value = true
        if (_backgroundAudioEnabled.value) {
            mediaSession.isActive = true
            MediaPlaybackService.startService(context)
            updateVideoNotification()
        } else {
            mediaSession.isActive = false
            dismissVideoNotification()
        }
    }

    fun pause() {
        flushProgress()
        exoPlayer.pause()
        _isPlaying.value = false
        releaseWakeLock()
        if (_backgroundAudioEnabled.value) {
            updateVideoNotification()
        } else {
            dismissVideoNotification()
        }
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
        dismissVideoNotification()
    }

    fun dismissVideoNotification() {
        try {
            mediaSession.isActive = false
            val audioManager = AudioPlayerManager.activeInstance
            if (audioManager?.isPlaying?.value == true || audioManager?.currentSong?.value != null) {
                MediaPlaybackService.updateNotification(context)
            } else {
                MediaPlaybackService.stopService(context)
            }
        } catch (_: Exception) {}
    }

    fun updateVideoNotification() {
        if (!_backgroundAudioEnabled.value) {
            dismissVideoNotification()
            return
        }
        val video = _currentVideo.value ?: run {
            dismissVideoNotification()
            return
        }
        val isPlaying = _isPlaying.value
        mediaSession.isActive = true

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
        setAspectRatio(nextMode)
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
        _currentVideo.value?.let { video ->
            try {
                videoAspectPrefs.edit().putString("aspect_${video.id}", mode.name).apply()
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
        val tracks = exoPlayer.currentTracks
        var targetOverride: TrackSelectionOverride? = null

        // 1. First attempt: match by stored indices if valid
        if (track.groupIndex in 0 until tracks.groups.size && track.trackIndex >= 0) {
            val group = tracks.groups[track.groupIndex]
            if (group.type == C.TRACK_TYPE_AUDIO && track.trackIndex < group.length) {
                targetOverride = TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
            }
        }

        // 2. Second attempt: match by exact trackId or format.id
        if (targetOverride == null) {
            outer@ for (groupIndex in 0 until tracks.groups.size) {
                val trackGroup = tracks.groups[groupIndex]
                if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until trackGroup.length) {
                        val currentTrackId = "audio_${groupIndex}_$i"
                        val format = trackGroup.getTrackFormat(i)
                        if (currentTrackId == track.id || (format.id != null && format.id == track.id)) {
                            targetOverride = TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                            break@outer
                        }
                    }
                }
            }
        }

        isAudioFallbackAttempted = false
        if (targetOverride != null) {
            try {
                trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                )
            } catch (_: Exception) {}
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .setOverrideForType(targetOverride)
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .build()
        }

        // Immediately update UI state so the clicked track shows checkmark instantly
        _availableAudioTracks.value = _availableAudioTracks.value.map {
            it.copy(isSelected = (it.id == track.id))
        }

        // Clear any previous audio codec error banner
        _errorMessage.value = null

        // If player stopped due to previous audio codec failure or error, recover playback
        if (exoPlayer.playerError != null || exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.playWhenReady = true
        if (!exoPlayer.isPlaying) {
            try { exoPlayer.play() } catch (_: Exception) {}
        }
        _isPlaying.value = true
    }

    fun selectSubtitleTrack(subtitle: SubtitleTrack?) {
        val updatedSubList = _availableSubtitleTracks.value.map { track ->
            if (subtitle == null) {
                track.copy(isSelected = false)
            } else {
                track.copy(isSelected = (track.id == subtitle.id))
            }
        }
        _availableSubtitleTracks.value = updatedSubList

        if (subtitle == null) {
            // Disable subtitles
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            )
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _externalCues.value = emptyList()
            _activeSubtitleText.value = null
        } else if (subtitle.isExternal) {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            )
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            )
            val tracks = exoPlayer.currentTracks
            var targetOverride: TrackSelectionOverride? = null

            if (subtitle.groupIndex in 0 until tracks.groups.size && subtitle.trackIndex >= 0) {
                val group = tracks.groups[subtitle.groupIndex]
                if (group.type == C.TRACK_TYPE_TEXT && subtitle.trackIndex < group.length) {
                    targetOverride = TrackSelectionOverride(group.mediaTrackGroup, subtitle.trackIndex)
                }
            }

            if (targetOverride == null) {
                outer@ for (groupIndex in 0 until tracks.groups.size) {
                    val trackGroup = tracks.groups[groupIndex]
                    if (trackGroup.type == C.TRACK_TYPE_TEXT) {
                        for (i in 0 until trackGroup.length) {
                            val trackId = "sub_${groupIndex}_$i"
                            val format = trackGroup.getTrackFormat(i)
                            if (trackId == subtitle.id || (format.id != null && format.id == subtitle.id)) {
                                targetOverride = TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                break@outer
                            }
                        }
                    }
                }
            }

            if (targetOverride != null) {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setOverrideForType(targetOverride)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .build()
            }
            _externalCues.value = emptyList()
        }
        exoPlayer.playWhenReady = true
        if (!exoPlayer.isPlaying) {
            try { exoPlayer.play() } catch (_: Exception) {}
        }
        _isPlaying.value = true
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
                    val trackId = "audio_${groupIndex}_$i"
                    val isSelected = group.isTrackSelected(i)

                    val langDisplay = try {
                        if (!format.language.isNullOrEmpty() && format.language != "und") {
                            java.util.Locale(format.language!!).getDisplayLanguage(java.util.Locale.ENGLISH)
                        } else null
                    } catch (_: Exception) { null }

                    val baseName = format.label ?: langDisplay ?: "Audio Track ${audioList.size + 1}"
                    val fullLabel = if (!format.sampleMimeType.isNullOrEmpty()) "$baseName (${format.sampleMimeType})" else baseName

                    audioList.add(
                        AudioTrack(
                            id = trackId,
                            label = fullLabel,
                            language = format.language ?: "und",
                            isSelected = isSelected,
                            groupIndex = groupIndex,
                            trackIndex = i
                        )
                    )
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val trackId = "sub_${groupIndex}_$i"
                    val isSelected = group.isTrackSelected(i)

                    val langDisplay = try {
                        if (!format.language.isNullOrEmpty() && format.language != "und") {
                            java.util.Locale(format.language!!).getDisplayLanguage(java.util.Locale.ENGLISH)
                        } else null
                    } catch (_: Exception) { null }

                    val baseName = format.label ?: langDisplay ?: "Subtitle ${subList.size + 1}"

                    subList.add(
                        SubtitleTrack(
                            id = trackId,
                            language = format.language ?: "und",
                            label = baseName,
                            isSelected = isSelected,
                            groupIndex = groupIndex,
                            trackIndex = i
                        )
                    )
                }
            }
        }

        // Preserve user's selection if ExoPlayer is momentarily reporting no audio track selected
        val currentSelectedAudioId = _availableAudioTracks.value.firstOrNull { it.isSelected }?.id
        if (currentSelectedAudioId != null && audioList.none { it.isSelected }) {
            val idx = audioList.indexOfFirst { it.id == currentSelectedAudioId }
            if (idx != -1) {
                audioList[idx] = audioList[idx].copy(isSelected = true)
            }
        }

        _availableAudioTracks.value = audioList

        // Preserve external subtitles
        val externalSubs = _availableSubtitleTracks.value.filter { it.isExternal }
        _availableSubtitleTracks.value = subList + externalSubs
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

        private fun isAudioError(error: PlaybackException): Boolean {
            val fullText = buildString {
                append(error.message ?: "")
                append(" ")
                append(error.localizedMessage ?: "")
                append(" ")
                error.cause?.let { cause ->
                    append(cause.javaClass.name)
                    append(" ")
                    append(cause.message ?: "")
                }
            }.lowercase()

            if (fullText.contains("audio") ||
                fullText.contains("audiotrack") ||
                fullText.contains("audiosink") ||
                fullText.contains("mediacodecaudiorenderer") ||
                fullText.contains("eac3") ||
                fullText.contains("ac-3") ||
                fullText.contains("ac3") ||
                fullText.contains("dts") ||
                fullText.contains("truehd") ||
                fullText.contains("m4a") ||
                fullText.contains("aac")
            ) {
                return true
            }

            return when (error.errorCode) {
                PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
                PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> true
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                    fullText.contains("audio") || fullText.contains("index=1")
                }
                else -> false
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _isBuffering.value = false

            if (isAudioError(error) && !isAudioFallbackAttempted) {
                isAudioFallbackAttempted = true
                val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)

                // Disable audio so video continues playing smoothly without audio
                try {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    )
                } catch (_: Exception) {}

                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()

                _errorMessage.value = null

                // Show toast message as requested: "Not supported" / "Audio not supported"
                scope.launch(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Audio not supported. Playing video without audio.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

                // Prepare and resume video playback
                exoPlayer.prepare()
                if (currentPos > 0) {
                    exoPlayer.seekTo(currentPos)
                }
                exoPlayer.playWhenReady = true
                try { exoPlayer.play() } catch (_: Exception) {}
                _isPlaying.value = true
                return
            }

            // Non-audio errors: show Toast instead of blocking red card
            val message = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Network error: Unable to connect to server."
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                    "Unsupported or corrupted video file."
                else -> error.localizedMessage ?: "Playback error: ${error.errorCodeName}"
            }
            scope.launch(Dispatchers.Main) {
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            }
            _errorMessage.value = null
        }
    }

    fun retryPlayback() {
        _errorMessage.value = null
        exoPlayer.prepare()
        exoPlayer.play()
    }

    private var equalizer: android.media.audiofx.Equalizer? = null
    private var bassBoost: android.media.audiofx.BassBoost? = null
    private var virtualizer: android.media.audiofx.Virtualizer? = null
    private var activeAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private val _isEqualizerEnabled = MutableStateFlow(false)
    val isEqualizerEnabled: StateFlow<Boolean> = _isEqualizerEnabled.asStateFlow()
    private val _selectedPreset = MutableStateFlow("Normal")
    val selectedPreset: StateFlow<String> = _selectedPreset.asStateFlow()

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

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        try {
            equalizer?.release()
            equalizer = null
            _exoPlayer?.removeListener(playerListener)
            _exoPlayer?.release()
        } catch (_: Exception) {}
        _exoPlayer = null
    }
}
