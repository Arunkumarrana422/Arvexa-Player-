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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Playlist
import com.example.domain.model.Video
import com.example.ui.components.EmptyStateView
import com.example.ui.components.DeletePlaylistConfirmDialog
import com.example.ui.components.AddVideosScreen
import com.example.ui.components.PlaylistCard
import com.example.ui.components.VideoCard
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.isNightMode
import com.example.ui.theme.nightGlassBorder
import kotlinx.coroutines.flow.Flow

@Composable
fun CreatePlaylistPillButton(
    onClick: () -> Unit,
    text: String = "Create New Playlist",
    modifier: Modifier = Modifier
) {
    val isDark = isNightMode()

    // Vibrant, high-contrast cyan styling matching navigation bar in Light Mode
    val lightBg = Color(0xFFE0F7FA) // Matching navigation active pill cyan
    val lightBorder = Color(0xFF22D3EE).copy(alpha = 0.80f)
    val lightContent = Color(0xFF007A99) // Deep, crisp, razor-sharp cyan (no blurriness)

    // Glowing metallic glass effect in Dark / Night Mode
    val darkBgGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.09f),
            Color(0x2222D3EE),
            Color(0x350B1020)
        )
    )
    val darkContent = NovaAccent // Vibrant neon cyan #22D3EE

    val glassBorder = nightGlassBorder(intensity = 1.35f) ?: BorderStroke(1.2.dp, lightBorder)

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("create_playlist_btn"),
        shape = RoundedCornerShape(50),
        color = if (isDark) Color(0xFF131B2E).copy(alpha = 0.75f) else lightBg,
        border = if (isDark) glassBorder else BorderStroke(1.5.dp, lightBorder),
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Box(
            modifier = if (isDark) Modifier.background(darkBgGradient) else Modifier
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isDark) darkContent else lightContent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = if (isDark) darkContent else lightContent
                )
            }
        }
    }
}

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    selectedPlaylist: Playlist?,
    playlistVideosFlow: ((Long) -> Flow<List<Video>>)?,
    allVideos: List<Video> = emptyList(),
    currentPlayingVideoId: String? = null,
    isPlaying: Boolean = false,
    currentPosMs: Long = 0L,
    onSelectPlaylist: (Playlist?) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onRemoveFromPlaylist: (Long, String) -> Unit,
    onAddVideosToPlaylist: ((Long, List<Video>) -> Unit)? = null,
    onPlayVideo: (Video, List<Video>) -> Unit,
    onToggleFavorite: (Video) -> Unit,
    onAddToPlaylist: (Video) -> Unit,
    onShowVideoInfo: (Video) -> Unit,
    onToggleBottomBarVisibility: ((Boolean) -> Unit)? = null
) {
    val currentSelectedPlaylist = remember(selectedPlaylist, playlists) {
        selectedPlaylist?.let { sp -> playlists.find { it.id == sp.id } ?: sp }
    }

    if (currentSelectedPlaylist != null) {
        BackHandler {
            onSelectPlaylist(null)
        }
    }

    AnimatedContent(
        targetState = currentSelectedPlaylist,
        transitionSpec = {
            fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing))
                .togetherWith(fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing)))
        },
        label = "video_playlist_screen_transfer"
    ) { playlist ->
        if (playlist != null && playlistVideosFlow != null) {
            val playlistVideos by playlistVideosFlow(playlist.id).collectAsState(initial = emptyList())
            var showDeleteConfirm by remember { mutableStateOf(false) }
            var showMultiSelectAdd by remember { mutableStateOf(false) }

            if (showDeleteConfirm) {
                DeletePlaylistConfirmDialog(
                    playlistName = playlist.name,
                    onDismiss = { showDeleteConfirm = false },
                    onConfirm = {
                        showDeleteConfirm = false
                        onDeletePlaylist(playlist.id)
                        onSelectPlaylist(null)
                    }
                )
            }

            AnimatedContent(
                targetState = showMultiSelectAdd,
                transitionSpec = {
                    if (targetState) {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(300)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(200))
                        )
                    } else {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 4 },
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(300)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(200))
                        )
                    }
                },
                label = "playlist_add_videos_transfer"
            ) { isAdding ->
                if (isAdding) {
                    DisposableEffect(Unit) {
                        onToggleBottomBarVisibility?.invoke(false)
                        onDispose {
                            onToggleBottomBarVisibility?.invoke(true)
                        }
                    }
                    AddVideosScreen(
                        allVideos = allVideos,
                        existingVideoIds = playlistVideos.map { it.id }.toSet(),
                        onBack = { showMultiSelectAdd = false },
                        onAddVideos = { videos ->
                            showMultiSelectAdd = false
                            onAddVideosToPlaylist?.invoke(playlist.id, videos)
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .testTag("playlist_detail_screen")
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onSelectPlaylist(null) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${playlistVideos.size} videos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NovaAccent
                                )
                            }
                            IconButton(onClick = { showMultiSelectAdd = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Videos",
                                    tint = NovaAccent
                                )
                            }
                            IconButton(onClick = {
                                showDeleteConfirm = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Playlist",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }

                        // Play / Shuffle Row
                        if (playlistVideos.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onPlayVideo(playlistVideos.first(), playlistVideos) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NovaAccent),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Play All")
                                }

                                OutlinedButton(
                                    onClick = { onPlayVideo(playlistVideos.shuffled().first(), playlistVideos.shuffled()) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Shuffle")
                                }
                            }
                        }

                        if (playlistVideos.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.PlaylistPlay,
                                title = "Playlist is Empty",
                                description = "Add videos to '${playlist.name}' from your video library or folders."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(playlistVideos, key = { it.id }) { video ->
                                    VideoCard(
                                        video = video,
                                        onClick = { onPlayVideo(video, playlistVideos) },
                                        isCurrentlyPlaying = (currentPlayingVideoId == video.id && isPlaying),
                                        currentPosMs = currentPosMs,
                                        onToggleFavorite = { onToggleFavorite(video) },
                                        onAddToPlaylist = { onAddToPlaylist(video) },
                                        onShowInfo = { onShowVideoInfo(video) },
                                        onRemoveFromPlaylist = { onRemoveFromPlaylist(playlist.id, video.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("playlists_screen")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    CreatePlaylistPillButton(
                        onClick = onCreatePlaylistClick,
                        text = "Create New Playlist"
                    )
                }

                if (playlists.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.PlaylistPlay,
                        title = "No Playlists Yet",
                        description = "Create custom playlists to organize your favorite movies, clips, and series."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(playlists, key = { it.id }) { p ->
                            PlaylistCard(
                                playlist = p,
                                onClick = { onSelectPlaylist(p) },
                                onDelete = { onDeletePlaylist(p.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
