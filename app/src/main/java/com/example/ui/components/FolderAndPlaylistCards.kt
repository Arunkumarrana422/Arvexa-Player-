package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Playlist
import com.example.domain.model.Video
import com.example.domain.model.VideoFolder
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaSecondary
import com.example.ui.theme.isNightMode
import com.example.ui.theme.nightGlassBorder

@Composable
fun ThemedFolderIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    accentColor: Color = NovaAccent,
    isCurrentlyPlaying: Boolean = false
) {
    val isDark = isNightMode()
    val backFlapColor = lerp(accentColor, Color.Black, if (isDark) 0.28f else 0.22f)
    val frontTopColor = lerp(accentColor, Color.White, 0.06f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val fw = w * 0.96f
            val fh = fw * 0.80f
            val left = (w - fw) / 2f
            val top = (h - fh) / 2f
            val right = left + fw
            val bottom = top + fh

            // 1. Darker back flap with folder tab on top-left
            val tlR = fw * 0.11f
            val trR = fw * 0.095f
            val bR = fw * 0.115f
            val tabEndX = left + fw * 0.305f
            val slopeEndX = left + fw * 0.425f
            val rightTopY = top + fh * 0.125f

            val backPath = Path().apply {
                moveTo(left + tlR, top)
                lineTo(tabEndX - fw * 0.03f, top)
                cubicTo(
                    tabEndX + fw * 0.045f, top,
                    slopeEndX - fw * 0.045f, rightTopY,
                    slopeEndX + fw * 0.03f, rightTopY
                )
                lineTo(right - trR, rightTopY)
                quadraticTo(right, rightTopY, right, rightTopY + trR)
                lineTo(right, bottom - bR)
                quadraticTo(right, bottom, right - bR, bottom)
                lineTo(left + bR, bottom)
                quadraticTo(left, bottom, left, bottom - bR)
                lineTo(left, top + tlR)
                quadraticTo(left, top, left + tlR, top)
                close()
            }
            drawPath(path = backPath, color = backFlapColor)

            // 2. Crisp white inner sheet peeking out above the front pocket
            val sheetInset = fw * 0.068f
            val sheetTop = top + fh * 0.175f
            val sheetBottom = top + fh * 0.45f
            val sheetRadius = fw * 0.045f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left + sheetInset, sheetTop),
                size = Size(fw - sheetInset * 2f, sheetBottom - sheetTop),
                cornerRadius = CornerRadius(sheetRadius, sheetRadius)
            )

            // 3. Bright theme-colored front pocket
            val frontTop = top + fh * 0.238f
            val ftR = fw * 0.048f
            val fbR = fw * 0.115f
            val frontPath = Path().apply {
                moveTo(left + ftR, frontTop)
                lineTo(right - ftR, frontTop)
                quadraticTo(right, frontTop, right, frontTop + ftR)
                lineTo(right, bottom - fbR)
                quadraticTo(right, bottom, right - fbR, bottom)
                lineTo(left + fbR, bottom)
                quadraticTo(left, bottom, left, bottom - fbR)
                lineTo(left, frontTop + ftR)
                quadraticTo(left, frontTop, left + ftR, frontTop)
                close()
            }
            drawPath(
                path = frontPath,
                brush = Brush.verticalGradient(
                    colors = listOf(frontTopColor, accentColor),
                    startY = frontTop,
                    endY = bottom
                )
            )
        }

        if (isCurrentlyPlaying) {
            Box(
                modifier = Modifier
                    .padding(top = size * 0.16f)
                    .background(Color(0x55000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                NowPlayingEqualizer(modifier = Modifier.size(16.dp), barColor = Color.White)
            }
        }
    }
}

@Composable
fun FolderCard(
    folder: VideoFolder,
    onClick: () -> Unit,
    isCurrentlyPlaying: Boolean = false,
    playingVideoTitle: String? = null,
    playingPosMs: Long = 0L,
    playingDurMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isCurrentlyPlaying) NovaAccent.copy(alpha = 0.06f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .testTag("folder_card_${folder.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemedFolderIcon(
                size = 48.dp,
                accentColor = NovaAccent,
                isCurrentlyPlaying = isCurrentlyPlaying
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isCurrentlyPlaying) {
                        NowPlayingEqualizer(modifier = Modifier.size(14.dp), barColor = NovaAccent)
                    }
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        color = if (isCurrentlyPlaying) NovaAccent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (folder.hasNewVideos) {
                        Text(
                            text = "NEW",
                            color = Color(0xFFFF2A4B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                if (isCurrentlyPlaying && playingPosMs > 0) {
                    Text(
                        text = "▶ ${playingVideoTitle ?: "Playing"} • ${Video.formatDuration(playingPosMs)} / ${Video.formatDuration(playingDurMs.takeIf { it > 0 } ?: folder.totalDurationMs)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = NovaAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "${folder.videoCount} videos • ${folder.totalDurationFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = if (isCurrentlyPlaying) NovaAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun FolderGridCard(
    folder: VideoFolder,
    onClick: () -> Unit,
    isCurrentlyPlaying: Boolean = false,
    playingVideoTitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("folder_grid_card_${folder.name}"),
        shape = RoundedCornerShape(12.dp),
        border = nightGlassBorder(isCurrentlyPlaying = isCurrentlyPlaying),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyPlaying) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentlyPlaying) 1.5.dp else 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ThemedFolderIcon(
                    size = 54.dp,
                    accentColor = NovaAccent,
                    isCurrentlyPlaying = isCurrentlyPlaying
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = if (isCurrentlyPlaying) NovaAccent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${folder.videoCount} videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaAccent,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (folder.hasNewVideos) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(
                            color = Color(0xFFFF2A4B),
                            shape = RoundedCornerShape(bottomEnd = 8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NEW",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("playlist_card_${playlist.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(listOf(NovaSecondary, NovaAccent)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistPlay,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (playlist.description.isNotBlank()) {
                    Text(
                        text = playlist.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${playlist.videoCount} videos",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = NovaAccent,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
