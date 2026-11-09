package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.SortOption
import com.example.domain.model.Video
import com.example.domain.model.ViewMode
import com.example.ui.components.EmptyStateView
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoGridCard

@Composable
fun VideosScreen(
    videos: List<Video>,
    sortOption: SortOption,
    viewMode: ViewMode,
    isScanning: Boolean,
    currentPlayingVideoId: String? = null,
    isPlaying: Boolean = false,
    currentPosMs: Long = 0L,
    onSortChange: (SortOption) -> Unit = {},
    onViewModeChange: (ViewMode) -> Unit = {},
    onRefresh: () -> Unit = {},
    onPlayVideo: (Video, List<Video>) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onAddToPlaylist: (Video) -> Unit,
    onShowVideoInfo: (Video) -> Unit,
    onDeleteVideo: (String) -> Unit,
    isMiniPlayerActive: Boolean = false
) {
    val latestPlayedVideoId = androidx.compose.runtime.remember(videos, currentPlayingVideoId) {
        if (currentPlayingVideoId != null) {
            currentPlayingVideoId
        } else {
            videos.filter { it.lastPlayedTimestamp > 0L }.maxByOrNull { it.lastPlayedTimestamp }?.id
        }
    }

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("videos_screen")
    ) {
        if (videos.isEmpty()) {
            EmptyStateView(
                title = "No Videos Found",
                description = "Scan your device storage or stream videos online by providing direct URLs.",
                actionButtonText = "Scan Storage",
                onActionClick = onRefresh
            )
        } else if (viewMode == ViewMode.LIST) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (isMiniPlayerActive) 86.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onPlayVideo(video, videos) },
                        isCurrentlyPlaying = (currentPlayingVideoId == video.id && isPlaying),
                        isLatestPlayed = (video.id == latestPlayedVideoId),
                        currentPosMs = currentPosMs,
                        onToggleFavorite = { onToggleFavorite(video) },
                        onAddToPlaylist = { onAddToPlaylist(video) },
                        onShowInfo = { onShowVideoInfo(video) },
                        onDelete = { onDeleteVideo(video.id) }
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = if (isMiniPlayerActive) 86.dp else 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoGridCard(
                        video = video,
                        onClick = { onPlayVideo(video, videos) },
                        isCurrentlyPlaying = (currentPlayingVideoId == video.id && isPlaying),
                        isLatestPlayed = (video.id == latestPlayedVideoId),
                        currentPosMs = currentPosMs,
                        onToggleFavorite = { onToggleFavorite(video) },
                        onAddToPlaylist = { onAddToPlaylist(video) },
                        onShowInfo = { onShowVideoInfo(video) },
                        onDelete = { onDeleteVideo(video.id) }
                    )
                }
            }
        }
    }
}
