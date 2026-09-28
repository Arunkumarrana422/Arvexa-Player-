package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.Brush
import coil.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.AudioPlaylist
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.domain.model.Video
import com.example.ui.screens.CreatePlaylistPillButton
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.theme.isNightMode
import com.example.ui.theme.nightGlassBorder

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val isDark = isNightMode()

    val dialogBg = if (isDark) Color(0xFF131B2E) else Color(0xFFFFFFFF)
    val glassBorder = if (isDark) nightGlassBorder(strokeWidth = 1.2.dp, intensity = 1.4f) else null

    // Text field color configuration matching navigation & pill colors
    val tfContainerColor = if (isDark) Color(0xFF192238) else Color(0xFFF8FAFC)
    val tfFocusedBorder = if (isDark) NovaAccent else Color(0xFF007A99)
    val tfUnfocusedBorder = if (isDark) Color(0xFF2A375A) else Color(0xFFCBD5E1)
    val tfTextColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val tfPlaceholderColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val tfIconColor = if (isDark) NovaAccent else Color(0xFF007A99)

    val createBtnEnabled = name.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            color = dialogBg,
            border = glassBorder,
            shadowElevation = if (isDark) 0.dp else 6.dp
        ) {
            Box(
                modifier = if (isDark) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color(0x1522D3EE),
                                Color.Black.copy(alpha = 0.22f)
                            )
                        )
                    )
                } else Modifier
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    if (isDark) listOf(NovaPrimary, NovaSecondary)
                                    else listOf(Color(0xFFE0F7FA), Color(0xFFCCFBF1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isDark) NovaAccent else Color(0xFF007A99),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "New Playlist",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create a custom mix of videos & music",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Playlist Name Text Field with strict single-line "Playlist Name" placeholder
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = {
                            Text(
                                text = "Playlist Name",
                                color = tfPlaceholderColor,
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DriveFileRenameOutline,
                                contentDescription = null,
                                tint = tfIconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = tfContainerColor,
                            unfocusedContainerColor = tfContainerColor,
                            focusedBorderColor = tfFocusedBorder,
                            unfocusedBorderColor = tfUnfocusedBorder,
                            focusedTextColor = tfTextColor,
                            unfocusedTextColor = tfTextColor,
                            cursorColor = tfFocusedBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description Text Field with single-line guarantee
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = {
                            Text(
                                text = "Description (Optional)",
                                color = tfPlaceholderColor,
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = tfIconColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = tfContainerColor,
                            unfocusedContainerColor = tfContainerColor,
                            focusedBorderColor = tfFocusedBorder,
                            unfocusedBorderColor = tfUnfocusedBorder,
                            focusedTextColor = tfTextColor,
                            unfocusedTextColor = tfTextColor,
                            cursorColor = tfFocusedBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_desc_input")
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("cancel_create_playlist_btn")
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onCreate(name.trim(), description.trim())
                                }
                            },
                            enabled = createBtnEnabled,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) NovaAccent else Color(0xFF007A99),
                                contentColor = if (isDark) Color(0xFF0B1020) else Color.White,
                                disabledContainerColor = if (isDark) Color(0xFF1E2638) else Color(0xFFE2E8F0),
                                disabledContentColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            ),
                            modifier = Modifier.testTag("confirm_create_playlist_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Create",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    video: Video,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    val isDark = isNightMode()
    val dialogBg = if (isDark) Color(0xFF131B2E) else Color(0xFFFFFFFF)
    val glassBorder = if (isDark) nightGlassBorder(intensity = 1.35f) else null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            color = dialogBg,
            border = glassBorder,
            shadowElevation = if (isDark) 0.dp else 6.dp
        ) {
            Box(
                modifier = if (isDark) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.20f)
                            )
                        )
                    )
                } else Modifier
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    if (isDark) listOf(NovaPrimary, NovaSecondary)
                                    else listOf(Color(0xFFE0F7FA), Color(0xFFCCFBF1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = if (isDark) NovaAccent else Color(0xFF007A99),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Add Video to Playlist",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDark) NovaAccent else Color(0xFF007A99),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Create New Playlist Pill Button inside Add Dialog
                    CreatePlaylistPillButton(
                        onClick = onCreateNewPlaylist,
                        text = "New Playlist"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (playlists.isEmpty()) {
                        Text(
                            text = "No playlists created yet. Create one above!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playlists) { playlist ->
                                Card(
                                    onClick = { onSelectPlaylist(playlist) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    border = nightGlassBorder(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF1A2238) else Color(0xFFF1F5F9)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistPlay,
                                            contentDescription = null,
                                            tint = if (isDark) NovaAccent else Color(0xFF007A99),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${playlist.videoCount} videos",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddSongToPlaylistDialog(
    song: Song,
    playlists: List<AudioPlaylist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (AudioPlaylist) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    val isDark = isNightMode()
    val dialogBg = if (isDark) Color(0xFF131B2E) else Color(0xFFFFFFFF)
    val glassBorder = if (isDark) nightGlassBorder(intensity = 1.35f) else null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            color = dialogBg,
            border = glassBorder,
            shadowElevation = if (isDark) 0.dp else 6.dp
        ) {
            Box(
                modifier = if (isDark) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.20f)
                            )
                        )
                    )
                } else Modifier
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    if (isDark) listOf(NovaPrimary, NovaSecondary)
                                    else listOf(Color(0xFFE0F7FA), Color(0xFFCCFBF1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isDark) NovaAccent else Color(0xFF007A99),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Add Song to Playlist",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${song.title} • ${song.artist}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isDark) NovaAccent else Color(0xFF007A99),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Create New Playlist Pill Button inside Add Dialog
                    CreatePlaylistPillButton(
                        onClick = onCreateNewPlaylist,
                        text = "New Playlist"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (playlists.isEmpty()) {
                        Text(
                            text = "No playlists created yet. Create one above!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playlists) { playlist ->
                                Card(
                                    onClick = { onSelectPlaylist(playlist) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    border = nightGlassBorder(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF1A2238) else Color(0xFFF1F5F9)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistPlay,
                                            contentDescription = null,
                                            tint = if (isDark) NovaAccent else Color(0xFF007A99),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${playlist.songs.size} tracks",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeletePlaylistConfirmDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isDark = isNightMode()
    val dialogBg = if (isDark) Color(0xFF131B2E) else Color(0xFFFFFFFF)
    val glassBorder = if (isDark) nightGlassBorder(intensity = 1.35f) else null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            color = dialogBg,
            border = glassBorder,
            shadowElevation = if (isDark) 0.dp else 6.dp
        ) {
            Box(
                modifier = if (isDark) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.20f)
                            )
                        )
                    )
                } else Modifier
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFEF4444).copy(alpha = if (isDark) 0.20f else 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Delete Playlist",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to delete \"$playlistName\"? The items inside will remain safely on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.testTag("confirm_delete_playlist_btn")
                        ) {
                            Text(
                                text = "Delete",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddSongsScreen(
    allSongs: List<Song>,
    existingSongIds: Set<String>,
    onBack: () -> Unit,
    onAddSongs: (List<Song>) -> Unit
) {
    val availableSongs = remember(allSongs, existingSongIds) {
        allSongs.filter { !existingSongIds.contains(it.id) }
    }
    var selectedSongIds by remember { mutableStateOf(setOf<String>()) }
    val isDark = isNightMode()

    // Matching pill colors for the round outline button
    val lightBg = Color(0xFFE0F7FA)
    val lightBorder = Color(0xFF22D3EE).copy(alpha = 0.85f)
    val lightContent = Color(0xFF007A99) // Deep sharp cyan
    val darkContent = NovaAccent // Vibrant neon cyan #22D3EE
    val darkBtnBg = Color(0xFF131B2E).copy(alpha = 0.75f)

    val isEnabled = selectedSongIds.isNotEmpty()

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Songs",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    if (selectedSongIds.size == availableSongs.size) {
                        selectedSongIds = emptySet()
                    } else {
                        selectedSongIds = availableSongs.map { it.id }.toSet()
                    }
                }
            ) {
                Text(
                    text = if (selectedSongIds.size == availableSongs.size) "Deselect All" else "Select All",
                    color = if (isDark) NovaAccent else Color(0xFF007A99),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (availableSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All available songs are already in this playlist.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableSongs, key = { it.id }) { song ->
                    val isSelected = selectedSongIds.contains(song.id)
                    Card(
                        onClick = {
                            selectedSongIds = if (isSelected) {
                                selectedSongIds - song.id
                            } else {
                                selectedSongIds + song.id
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF162032) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) BorderStroke(2.dp, NovaAccent) else nightGlassBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedSongIds = if (checked) {
                                        selectedSongIds + song.id
                                    } else {
                                        selectedSongIds - song.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val albumArtUri = android.content.ContentUris.withAppendedId(
                                android.net.Uri.parse("content://media/external/audio/albumart"),
                                song.albumId
                            )
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF2A3347), Color(0xFF1E2433)))),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = albumArtUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Add Selected Stadium Pill Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Surface(
                onClick = {
                    if (isEnabled) {
                        val songsToAdd = availableSongs.filter { selectedSongIds.contains(it.id) }
                        onAddSongs(songsToAdd)
                    }
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_selected_songs_btn"),
                shape = RoundedCornerShape(50),
                color = if (!isEnabled) {
                    if (isDark) Color(0xFF161F33).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
                } else {
                    if (isDark) darkBtnBg else lightBg
                },
                border = if (!isEnabled) {
                    BorderStroke(1.dp, if (isDark) Color(0xFF2E3A52) else Color(0xFFE2E8F0))
                } else {
                    if (isDark) nightGlassBorder(strokeWidth = 1.5.dp, intensity = 1.4f) else BorderStroke(1.5.dp, lightBorder)
                },
                shadowElevation = if (isDark || !isEnabled) 0.dp else 2.dp
            ) {
                Box(
                    modifier = if (isDark && isEnabled) {
                        Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.09f),
                                    Color(0x2222D3EE),
                                    Color(0x350B1020)
                                )
                            )
                        )
                    } else Modifier,
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (!isEnabled) {
                                if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            } else {
                                if (isDark) darkContent else lightContent
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Selected (${selectedSongIds.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 0.4.sp
                            ),
                            color = if (!isEnabled) {
                                if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            } else {
                                if (isDark) darkContent else lightContent
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddVideosScreen(
    allVideos: List<Video>,
    existingVideoIds: Set<String>,
    onBack: () -> Unit,
    onAddVideos: (List<Video>) -> Unit
) {
    val availableVideos = remember(allVideos, existingVideoIds) {
        allVideos.filter { !existingVideoIds.contains(it.id) }
    }
    var selectedVideoIds by remember { mutableStateOf(setOf<String>()) }
    val isDark = isNightMode()

    // Matching pill colors for the round outline button
    val lightBg = Color(0xFFE0F7FA)
    val lightBorder = Color(0xFF22D3EE).copy(alpha = 0.85f)
    val lightContent = Color(0xFF007A99) // Deep sharp cyan
    val darkContent = NovaAccent // Vibrant neon cyan #22D3EE
    val darkBtnBg = Color(0xFF131B2E).copy(alpha = 0.75f)

    val isEnabled = selectedVideoIds.isNotEmpty()

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Videos",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    if (selectedVideoIds.size == availableVideos.size) {
                        selectedVideoIds = emptySet()
                    } else {
                        selectedVideoIds = availableVideos.map { it.id }.toSet()
                    }
                }
            ) {
                Text(
                    text = if (selectedVideoIds.size == availableVideos.size) "Deselect All" else "Select All",
                    color = if (isDark) NovaAccent else Color(0xFF007A99),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (availableVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All available videos are already in this playlist.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableVideos, key = { it.id }) { video ->
                    val isSelected = selectedVideoIds.contains(video.id)
                    Card(
                        onClick = {
                            selectedVideoIds = if (isSelected) {
                                selectedVideoIds - video.id
                            } else {
                                selectedVideoIds + video.id
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF162032) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) BorderStroke(2.dp, NovaAccent) else nightGlassBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedVideoIds = if (checked) {
                                        selectedVideoIds + video.id
                                    } else {
                                        selectedVideoIds - video.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            VideoThumbnailView(
                                video = video,
                                modifier = Modifier
                                    .size(64.dp, 40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = video.folderName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Add Selected Stadium Pill Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Surface(
                onClick = {
                    if (isEnabled) {
                        val videosToAdd = availableVideos.filter { selectedVideoIds.contains(it.id) }
                        onAddVideos(videosToAdd)
                    }
                },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_selected_videos_btn"),
                shape = RoundedCornerShape(50),
                color = if (!isEnabled) {
                    if (isDark) Color(0xFF161F33).copy(alpha = 0.5f) else Color(0xFFF1F5F9)
                } else {
                    if (isDark) darkBtnBg else lightBg
                },
                border = if (!isEnabled) {
                    BorderStroke(1.dp, if (isDark) Color(0xFF2E3A52) else Color(0xFFE2E8F0))
                } else {
                    if (isDark) nightGlassBorder(strokeWidth = 1.5.dp, intensity = 1.4f) else BorderStroke(1.5.dp, lightBorder)
                },
                shadowElevation = if (isDark || !isEnabled) 0.dp else 2.dp
            ) {
                Box(
                    modifier = if (isDark && isEnabled) {
                        Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.09f),
                                    Color(0x2222D3EE),
                                    Color(0x350B1020)
                                )
                            )
                        )
                    } else Modifier,
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (!isEnabled) {
                                if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            } else {
                                if (isDark) darkContent else lightContent
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Selected (${selectedVideoIds.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 0.4.sp
                            ),
                            color = if (!isEnabled) {
                                if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            } else {
                                if (isDark) darkContent else lightContent
                            }
                        )
                    }
                }
            }
        }
    }
}
