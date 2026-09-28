package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.VideoCard
import com.example.ui.components.VideoGridCard
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary

@Composable
fun FoldersScreen(
    folders: List<VideoFolder>,
    selectedFolder: VideoFolder?,
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
    onDeleteVideo: (String) -> Unit
) {
    val currentSelectedFolder = remember(selectedFolder, folders) {
        selectedFolder?.let { sf -> folders.find { it.name == sf.name } ?: sf }
    }

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var currentSort by remember { mutableStateOf(SortOption.DATE_DESC) }
    var currentViewMode by remember { mutableStateOf(ViewMode.LIST) }

    if (currentSelectedFolder != null) {
        BackHandler {
            onSelectFolder(null)
        }
    }

    AnimatedContent(
        targetState = currentSelectedFolder,
        transitionSpec = {
            if (targetState != null) {
                // Forward animation: Enter Folder Detail (slide in from right + fade)
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(320)))
                .togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 4 },
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(250))
                )
            } else {
                // Backward animation: Exit Folder to Overview (slide out to right + fade)
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 4 },
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(320)))
                .togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(250))
                )
            }
        },
        label = "video_folder_screen_transfer"
    ) { folder ->
        if (folder != null) {
            val sortedVideos = remember(folder.videos, currentSort) {
                when (currentSort) {
                    SortOption.DATE_DESC -> folder.videos.sortedByDescending { it.dateAdded }
                    SortOption.DATE_ASC -> folder.videos.sortedBy { it.dateAdded }
                    SortOption.NAME_ASC -> folder.videos.sortedBy { it.title.lowercase() }
                    SortOption.NAME_DESC -> folder.videos.sortedByDescending { it.title.lowercase() }
                    SortOption.DURATION_DESC -> folder.videos.sortedByDescending { it.durationMs }
                    SortOption.SIZE_DESC -> folder.videos.sortedByDescending { it.sizeBytes }
                    SortOption.RECENTLY_PLAYED -> folder.videos.sortedByDescending { it.lastPlayedTimestamp }
                }
            }

            // Folder Detail View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
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
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "${folder.videoCount} videos • ${folder.totalDurationFormatted}",
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
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All", fontSize = 12.sp)
                    }
                }

                // FIXED Control Bar (Count + Sort Button + Grid/List Toggle + Refresh) — Pinned and does not scroll
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${sortedVideos.size} Videos",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Sort Dropdown Pill
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { sortMenuExpanded = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    tint = NovaAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentSort.label,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (option == currentSort) FontWeight.Bold else FontWeight.Normal,
                                                color = if (option == currentSort) NovaAccent else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            sortMenuExpanded = false
                                            currentSort = option
                                        }
                                    )
                                }
                            }
                        }

                        // View Mode Toggle (Grid / List)
                        IconButton(
                            onClick = {
                                currentViewMode = if (currentViewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("folder_view_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (currentViewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                contentDescription = "Toggle Grid/List",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Refresh Button
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(2.dp),
                                strokeWidth = 2.dp,
                                color = NovaAccent
                            )
                        } else {
                            IconButton(
                                onClick = { onRefresh?.invoke() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("refresh_folder_videos_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Scan Media",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Videos List / Grid
                if (sortedVideos.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Folder,
                        title = "Folder is Empty",
                        description = "No videos found in '${folder.name}'."
                    )
                } else if (currentViewMode == ViewMode.LIST) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sortedVideos, key = { it.id }) { video ->
                            VideoCard(
                                video = video,
                                onClick = { onPlayVideo(video, sortedVideos) },
                                isCurrentlyPlaying = (currentPlayingVideoId == video.id && isPlaying),
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
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sortedVideos, key = { it.id }) { video ->
                            VideoGridCard(
                                video = video,
                                onClick = { onPlayVideo(video, sortedVideos) },
                                isCurrentlyPlaying = (currentPlayingVideoId == video.id && isPlaying),
                                currentPosMs = currentPosMs,
                                onToggleFavorite = { onToggleFavorite(video) }
                            )
                        }
                    }
                }
            }
        } else {
            // Folder Overview List
            if (folders.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Folder,
                    title = "No Video Folders",
                    description = "Your device has no media folders discovered yet."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("folders_screen"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(folders, key = { it.name }) { f ->
                        val isFolderPlaying = isPlaying && f.videos.any { it.id == currentPlayingVideoId }
                        FolderCard(
                            folder = f,
                            onClick = { onSelectFolder(f) },
                            isCurrentlyPlaying = isFolderPlaying,
                            playingVideoTitle = if (isFolderPlaying) currentPlayingVideo?.title else null,
                            playingPosMs = if (isFolderPlaying) currentPosMs else 0L,
                            playingDurMs = if (isFolderPlaying) durationMs else 0L
                        )
                    }
                }
            }
        }
    }
}

