package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SortOption
import com.example.domain.model.Video
import com.example.domain.model.VideoFolder
import com.example.domain.model.ViewMode
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FolderCard
import com.example.ui.components.FolderGridCard
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoGridCard
import com.example.ui.theme.NovaAccent

@Composable
fun FoldersScreen(
    folders: List<VideoFolder>,
    selectedFolder: VideoFolder?,
    viewMode: ViewMode = ViewMode.LIST,
    sortOption: SortOption = SortOption.DATE_DESC,
    onViewModeChange: (ViewMode) -> Unit = {},
    onSortChange: (SortOption) -> Unit = {},
    currentPlayingVideoId: String? = null,
    isPlaying: Boolean = false,
    currentPosMs: Long = 0L,
    durationMs: Long = 0L,
    currentPlayingVideo: Video? = null,
    isScanning: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    onSelectFolder: (VideoFolder?) -> Unit,
    onPlayVideo: (Video, List<Video>) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onAddToPlaylist: (Video) -> Unit,
    onShowVideoInfo: (Video) -> Unit,
    onDeleteVideo: (String) -> Unit,
    isMiniPlayerActive: Boolean = false
) {
    val currentSelectedFolder = remember(selectedFolder, folders) {
        selectedFolder?.let { sf -> folders.find { it.name == sf.name } ?: sf }
    }

    if (currentSelectedFolder != null) {
        BackHandler {
            onSelectFolder(null)
        }

        val sortedVideos = remember(currentSelectedFolder.videos, sortOption) {
            when (sortOption) {
                SortOption.DATE_DESC -> currentSelectedFolder.videos.sortedByDescending { it.dateAdded }
                SortOption.DATE_ASC -> currentSelectedFolder.videos.sortedBy { it.dateAdded }
                SortOption.NAME_ASC -> currentSelectedFolder.videos.sortedBy { it.title.lowercase() }
                SortOption.NAME_DESC -> currentSelectedFolder.videos.sortedByDescending { it.title.lowercase() }
                SortOption.DURATION_DESC -> currentSelectedFolder.videos.sortedByDescending { it.durationMs }
                SortOption.SIZE_DESC -> currentSelectedFolder.videos.sortedByDescending { it.sizeBytes }
                SortOption.RECENTLY_PLAYED -> currentSelectedFolder.videos.sortedByDescending { it.lastPlayedTimestamp }
            }
        }

        val latestPlayedVideoId = remember(sortedVideos, currentPlayingVideoId) {
            if (currentPlayingVideoId != null) {
                currentPlayingVideoId
            } else {
                sortedVideos.filter { it.lastPlayedTimestamp > 0L }.maxByOrNull { it.lastPlayedTimestamp }?.id
            }
        }

        // Folder Detail View
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("folder_detail_view")
        ) {
            // Folder Breadcrumb Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSelectFolder(null) },
                    modifier = Modifier.testTag("folder_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Folders",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSelectedFolder.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${currentSelectedFolder.videoCount} videos • ${currentSelectedFolder.totalDurationFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaAccent
                    )
                }

                Button(
                    onClick = {
                        if (sortedVideos.isNotEmpty()) {
                            onPlayVideo(sortedVideos.first(), sortedVideos)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaAccent, contentColor = Color(0xFF0F172A)),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play All", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Videos List / Grid
            if (sortedVideos.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Folder,
                    title = "Folder is Empty",
                    description = "No videos found in '${currentSelectedFolder.name}'."
                )
            } else if (viewMode == ViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = if (isMiniPlayerActive) 86.dp else 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedVideos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onPlayVideo(video, sortedVideos) },
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
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = if (isMiniPlayerActive) 86.dp else 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedVideos, key = { it.id }) { video ->
                        VideoGridCard(
                            video = video,
                            onClick = { onPlayVideo(video, sortedVideos) },
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
    } else {
        // Folder Overview List / Grid
        if (folders.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Folder,
                title = "No Video Folders",
                description = "Your device has no media folders discovered yet."
            )
        } else if (viewMode == ViewMode.LIST) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("folders_screen"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (isMiniPlayerActive) 86.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(folders, key = { it.name }) { folder ->
                    val isFolderPlaying = isPlaying && folder.videos.any { it.id == currentPlayingVideoId }
                    FolderCard(
                        folder = folder,
                        onClick = { onSelectFolder(folder) },
                        isCurrentlyPlaying = isFolderPlaying,
                        playingVideoTitle = if (isFolderPlaying) currentPlayingVideo?.title else null,
                        playingPosMs = if (isFolderPlaying) currentPosMs else 0L,
                        playingDurMs = if (isFolderPlaying) durationMs else 0L
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("folders_screen"),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = if (isMiniPlayerActive) 86.dp else 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(folders, key = { it.name }) { folder ->
                    val isFolderPlaying = isPlaying && folder.videos.any { it.id == currentPlayingVideoId }
                    FolderGridCard(
                        folder = folder,
                        onClick = { onSelectFolder(folder) },
                        isCurrentlyPlaying = isFolderPlaying,
                        playingVideoTitle = if (isFolderPlaying) currentPlayingVideo?.title else null
                    )
                }
            }
        }
    }
}
