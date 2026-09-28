package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.Song

class MediaPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "nova_media_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.player.ACTION_START"
        const val ACTION_UPDATE = "com.example.player.ACTION_UPDATE"
        const val ACTION_STOP = "com.example.player.ACTION_STOP"

        var activeInstance: MediaPlaybackService? = null

        fun startService(context: Context) {
            try {
                val intent = Intent(context, MediaPlaybackService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun updateNotification(context: Context) {
            try {
                val intent = Intent(context, MediaPlaybackService::class.java).apply {
                    action = ACTION_UPDATE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                activeInstance?.let { svc ->
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.notify(NOTIFICATION_ID, svc.buildMediaNotification())
                }
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, MediaPlaybackService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        createNotificationChannel()
    }

    override fun onDestroy() {
        activeInstance = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, ACTION_UPDATE, null -> {
                val notification = buildMediaNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(NOTIFICATION_ID, notification)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background music and video playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildMediaNotification(): Notification {
        val audioManager = AudioPlayerManager.activeInstance
        val videoManager = NovaPlayerManager.activeInstance

        val song = audioManager?.currentSong?.value
        val isAudioPlaying = audioManager?.isPlaying?.value ?: false

        val video = videoManager?.currentVideo?.value
        val isVideoPlaying = videoManager?.isPlaying?.value ?: false

        val isAudioActive = isAudioPlaying || (song != null && !isVideoPlaying)
        val isVideoActive = !isAudioActive && (isVideoPlaying || video != null)

        val title = if (isVideoActive) (video?.title ?: "Playing Video") else (song?.title ?: "Playing Music")
        val subtitle = if (isVideoActive) (video?.folderName ?: "Nova Video Player") else (song?.artist ?: "Nova Player")
        val isPlaying = if (isVideoActive) isVideoPlaying else isAudioPlaying

        // Open app intent
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action intents
        val prevPendingIntent = createActionPendingIntent(if (isVideoActive) "VIDEO_PREV" else "ACTION_PREV")
        val playPausePendingIntent = createActionPendingIntent(
            if (isVideoActive) {
                if (isVideoPlaying) "VIDEO_PAUSE" else "VIDEO_PLAY"
            } else {
                if (isAudioPlaying) "ACTION_PAUSE" else "ACTION_PLAY"
            }
        )
        val nextPendingIntent = createActionPendingIntent(if (isVideoActive) "VIDEO_NEXT" else "ACTION_NEXT")
        val stopPendingIntent = createActionPendingIntent(if (isVideoActive) "VIDEO_STOP" else "ACTION_STOP")

        val albumArtUri = if (song != null) {
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
        } else null

        val bitmap: Bitmap? = try {
            if (albumArtUri != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = android.graphics.ImageDecoder.createSource(contentResolver, albumArtUri)
                    android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(contentResolver, albumArtUri)
                }
            } else if (isVideoActive && video != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        contentResolver.loadThumbnail(Uri.parse(video.uri), android.util.Size(512, 512), null)
                    } catch (_: Exception) {
                        @Suppress("DEPRECATION")
                        android.media.ThumbnailUtils.createVideoThumbnail(
                            video.path,
                            android.provider.MediaStore.Images.Thumbnails.MINI_KIND
                        )
                    }
                } else {
                    @Suppress("DEPRECATION")
                    android.media.ThumbnailUtils.createVideoThumbnail(
                        video.path,
                        android.provider.MediaStore.Images.Thumbnails.MINI_KIND
                    )
                }
            } else null
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_icon)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                prevPendingIntent
            )
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                nextPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Close",
                stopPendingIntent
            )

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
        }

        // Apply MediaStyle
        val mediaSession = if (isVideoActive) videoManager?.mediaSession else audioManager?.mediaSession
        if (mediaSession != null) {
            val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
            builder.setStyle(mediaStyle)
        }

        return builder.build()
    }

    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MediaActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
