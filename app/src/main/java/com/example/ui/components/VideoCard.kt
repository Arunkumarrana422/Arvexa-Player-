package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.Video
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.nightGlassBorder
import com.example.ui.utils.shareFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val HalfWatchedColor: Color get() = NovaAccent // Dynamically uses selected theme color

@Composable
fun NowPlayingEqualizer(
    modifier: Modifier = Modifier,
    barColor: Color = NovaAccent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(13.dp * h1.coerceIn(0.2f, 1f))
                .background(barColor, RoundedCornerShape(1.5.dp))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(13.dp * h2.coerceIn(0.2f, 1f))
                .background(barColor, RoundedCornerShape(1.5.dp))
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(13.dp * h3.coerceIn(0.2f, 1f))
                .background(barColor, RoundedCornerShape(1.5.dp))
        )
    }
}

@Composable
fun NewTagBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFE53935), RoundedCornerShape(3.5.dp))
            .padding(horizontal = 4.5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "New",
            color = Color.White,
            style = TextStyle(
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Normal,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

@Composable
fun VideoOptionsMenuDialog(
    video: Video,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun animateAndDismiss(action: () -> Unit) {
        coroutineScope.launch {
            isVisible = false
            delay(160)
            onDismiss()
            action()
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Dialog(
        onDismissRequest = {
            coroutineScope.launch {
                isVisible = false
                delay(160)
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable {
                    coroutineScope.launch {
                        isVisible = false
                        delay(160)
                        onDismiss()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(160)) + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialScale = 0.72f
                ),
                exit = fadeOut(animationSpec = tween(160)) + scaleOut(
                    animationSpec = tween(160, easing = FastOutSlowInEasing),
                    targetScale = 0.72f
                )
            ) {
                Card(
                    modifier = Modifier
                        .width(310.dp)
                        .padding(16.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(22.dp),
                    border = nightGlassBorder(intensity = 1.35f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Header Preview Box
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NovaAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = NovaAccent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${video.folderName} • ${video.durationFormatted}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NovaAccent,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(NovaAccent.copy(alpha = 0.25f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Menu Action Items
                        PopupMenuItem(
                            icon = Icons.Default.PlayArrow,
                            text = "Play Video",
                            iconTint = NovaAccent,
                            onClick = { animateAndDismiss(onPlay) }
                        )

                        PopupMenuItem(
                            icon = Icons.Default.Share,
                            text = "Share Video",
                            iconTint = NovaAccent,
                            onClick = {
                                animateAndDismiss {
                                    shareFile(context, video.path, "video/*")
                                }
                            }
                        )

                        PopupMenuItem(
                            icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            text = if (video.isFavorite) "Remove from Favorites" else "Add to Favorites",
                            iconTint = if (video.isFavorite) Color(0xFFEF4444) else NovaAccent,
                            onClick = { animateAndDismiss(onToggleFavorite) }
                        )

                        if (onRemoveFromPlaylist == null) {
                            PopupMenuItem(
                                icon = Icons.Default.PlaylistAdd,
                                text = "Add to Playlist",
                                iconTint = NovaAccent,
                                onClick = { animateAndDismiss(onAddToPlaylist) }
                            )
                        } else {
                            PopupMenuItem(
                                icon = Icons.Default.Delete,
                                text = "Remove from Playlist",
                                iconTint = Color(0xFFEF4444),
                                onClick = { animateAndDismiss(onRemoveFromPlaylist) }
                            )
                        }

                        PopupMenuItem(
                            icon = Icons.Default.Info,
                            text = "File Information",
                            iconTint = NovaAccent,
                            onClick = { animateAndDismiss(onShowInfo) }
                        )

                        PopupMenuItem(
                            icon = Icons.Default.Delete,
                            text = "Delete Video",
                            iconTint = Color(0xFFEF4444),
                            textColor = Color(0xFFEF4444),
                            onClick = { animateAndDismiss(onDelete) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupMenuItem(
    icon: ImageVector,
    text: String,
    iconTint: Color,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            ),
            color = textColor
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    isCurrentlyPlaying: Boolean = false,
    isLatestPlayed: Boolean = false,
    currentPosMs: Long = 0L,
    onToggleFavorite: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onShowInfo: () -> Unit = {},
    onDelete: () -> Unit = {},
    onRemoveFromPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showOptionsMenu by remember { mutableStateOf(false) }

    if (showOptionsMenu) {
        VideoOptionsMenuDialog(
            video = video,
            onDismiss = { showOptionsMenu = false },
            onPlay = onClick,
            onToggleFavorite = onToggleFavorite,
            onAddToPlaylist = onAddToPlaylist,
            onShowInfo = onShowInfo,
            onDelete = onDelete,
            onRemoveFromPlaylist = onRemoveFromPlaylist
        )
    }

    val isPartiallyWatched = video.isPartiallyWatched || (video.lastPositionMs > 5000L && video.progressFraction in 0.02f..0.95f)

    val titleColor = when {
        isCurrentlyPlaying -> NovaAccent
        isLatestPlayed -> NovaAccent
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_card_${video.id}"),
        shape = RoundedCornerShape(12.dp),
        border = nightGlassBorder(isCurrentlyPlaying = isCurrentlyPlaying),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentlyPlaying) 1.5.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showOptionsMenu = true }
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Box with Duration Badge & Playing Indicator
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(82.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                VideoThumbnailView(
                    video = video,
                    modifier = Modifier.fillMaxSize()
                )

                // Playing state overlay or Play Icon overlay
                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            NowPlayingEqualizer(barColor = NovaAccent)
                            Text(
                                text = "PLAYING",
                                color = NovaAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0x99000000))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0x66000000), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Duration badge bottom-right
                if (video.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.durationFormatted,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // New badge top-left
                if (video.isNew) {
                    NewTagBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    )
                }

                // Playback progress bar at bottom of thumbnail
                val activePos = if (isCurrentlyPlaying) currentPosMs else video.lastPositionMs
                val activeDuration = if (isCurrentlyPlaying && video.durationMs > 0) video.durationMs else video.durationMs
                val effectiveProgress = if (activeDuration > 0) (activePos.toFloat() / activeDuration.toFloat()).coerceIn(0f, 1f) else video.progressFraction

                if (activePos > 0 || isCurrentlyPlaying) {
                    LinearProgressIndicator(
                        progress = { effectiveProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .align(Alignment.BottomCenter),
                        color = NovaAccent,
                        trackColor = Color(0x66FFFFFF),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isCurrentlyPlaying) {
                        NowPlayingEqualizer(
                            modifier = Modifier.size(14.dp),
                            barColor = NovaAccent
                        )
                    }
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 18.sp
                        ),
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = video.folderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaAccent,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isCurrentlyPlaying) {
                    Spacer(modifier = Modifier.height(3.dp))
                    val displayPos = if (currentPosMs > 0L) currentPosMs else video.lastPositionMs
                    Text(
                        text = "${Video.formatDuration(displayPos)} / ${video.durationFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isPartiallyWatched) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Resume at ${Video.formatDuration(video.lastPositionMs)} (${(video.progressFraction * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaAccent.copy(alpha = 0.9f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // More actions button
            Box {
                IconButton(
                    onClick = { showOptionsMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("video_more_menu_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoGridCard(
    video: Video,
    onClick: () -> Unit,
    isCurrentlyPlaying: Boolean = false,
    isLatestPlayed: Boolean = false,
    currentPosMs: Long = 0L,
    onToggleFavorite: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onShowInfo: () -> Unit = {},
    onDelete: () -> Unit = {},
    onRemoveFromPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showOptionsMenu by remember { mutableStateOf(false) }

    if (showOptionsMenu) {
        VideoOptionsMenuDialog(
            video = video,
            onDismiss = { showOptionsMenu = false },
            onPlay = onClick,
            onToggleFavorite = onToggleFavorite,
            onAddToPlaylist = onAddToPlaylist,
            onShowInfo = onShowInfo,
            onDelete = onDelete,
            onRemoveFromPlaylist = onRemoveFromPlaylist
        )
    }

    val titleColor = when {
        isCurrentlyPlaying -> NovaAccent
        isLatestPlayed -> NovaAccent
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_grid_card_${video.id}"),
        shape = RoundedCornerShape(12.dp),
        border = nightGlassBorder(isCurrentlyPlaying = isCurrentlyPlaying),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentlyPlaying) 1.5.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showOptionsMenu = true }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                VideoThumbnailView(
                    video = video,
                    modifier = Modifier.fillMaxSize()
                )

                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            NowPlayingEqualizer(barColor = NovaAccent)
                            Text(
                                text = "PLAYING",
                                color = NovaAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0x99000000))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0x66000000), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (video.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.durationFormatted,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // New badge top-left
                if (video.isNew) {
                    NewTagBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    )
                }

                val activePos = if (isCurrentlyPlaying && currentPosMs > 0L) currentPosMs else video.lastPositionMs
                val activeFraction = if (video.durationMs > 0) (activePos.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f) else video.progressFraction
                if (activePos > 0 || isCurrentlyPlaying) {
                    LinearProgressIndicator(
                        progress = { activeFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = NovaAccent,
                        trackColor = Color(0x66FFFFFF),
                    )
                }
            }

            Column(modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showOptionsMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.folderName,
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaAccent,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = video.sizeFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
