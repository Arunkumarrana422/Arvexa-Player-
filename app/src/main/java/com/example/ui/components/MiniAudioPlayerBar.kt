package com.example.ui.components

import android.content.ContentUris
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.Song
import com.example.domain.model.Video
import com.example.player.AudioPlayerManager
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.theme.nightGlassBorder

@Composable
fun MiniAudioPlayerBar(
    audioPlayerManager: AudioPlayerManager,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by audioPlayerManager.currentSong.collectAsState()
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    val currentPositionMs by audioPlayerManager.currentPositionMs.collectAsState()
    val durationMs by audioPlayerManager.durationMs.collectAsState()

    MiniAudioPlayerContent(
        song = currentSong,
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        onExpandFullPlayer = onExpand,
        onTogglePlayPause = { audioPlayerManager.togglePlayPause() },
        onNext = { audioPlayerManager.nextSong() },
        onPrevious = { audioPlayerManager.previousSong() },
        onClose = { audioPlayerManager.stopAndDismiss() },
        modifier = modifier
    )
}

@Composable
fun MiniAudioPlayerContent(
    song: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onExpandFullPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = song != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn() + scaleIn(initialScale = 0.92f),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier
    ) {
        if (song != null) {
            val progressFraction = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

            Card(
                onClick = onExpandFullPlayer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("mini_audio_player_bar"),
                shape = RoundedCornerShape(16.dp),
                border = nightGlassBorder(intensity = 1.2f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val albumArtUri = remember(song.albumId) {
                            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), song.albumId)
                        }

                        // Album Art Thumbnail (Matching video 68x46 landscape box)
                        Box(
                            modifier = Modifier
                                .size(width = 68.dp, height = 46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(NovaPrimary, NovaSecondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = albumArtUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0x55000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    NowPlayingEqualizer(
                                        modifier = Modifier.size(18.dp),
                                        barColor = Color.White
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Title & Artist / Time Status (Matching video style)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${Video.formatDuration(currentPositionMs)} / ${Video.formatDuration(durationMs.takeIf { it > 0 } ?: song.durationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NovaAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "• ${song.artist}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Previous Track
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Play/Pause Action
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("mini_audio_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Next Track
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Close Action
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("mini_audio_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Bottom progress indicator
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = NovaAccent,
                        trackColor = Color(0x33FFFFFF)
                    )
                }
            }
        }
    }
}
