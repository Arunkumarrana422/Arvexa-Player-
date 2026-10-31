package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AudioTrack
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.nightGlassBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTracksBottomSheet(
    audioTracks: List<AudioTrack>,
    selectedAudioTrack: AudioTrack?,
    onSelectAudioTrack: (AudioTrack) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        dragHandle = null
    ) {
        AudioTracksPanelContent(
            audioTracks = audioTracks,
            selectedAudioTrack = selectedAudioTrack,
            onSelectAudioTrack = onSelectAudioTrack,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun AudioTracksPanelContent(
    audioTracks: List<AudioTrack>,
    selectedAudioTrack: AudioTrack?,
    onSelectAudioTrack: (AudioTrack) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
            .testTag("audio_tracks_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = NovaAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AUDIO STREAM",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (audioTracks.isEmpty()) {
            Text(
                text = "Default Audio Track",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            audioTracks.forEach { track ->
                val isSelected = selectedAudioTrack?.id == track.id || track.isSelected
                val trackBorder = if (isSelected) {
                    nightGlassBorder(strokeWidth = 1.5.dp, intensity = 1.35f) ?: BorderStroke(1.5.dp, NovaAccent)
                } else {
                    BorderStroke(1.dp, Color(0xFF2A374F).copy(alpha = 0.5f))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(trackBorder, RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.12f),
                                        Color(0x3322D3EE),
                                        Color(0x400B1020)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        Color(0x25162032),
                                        Color(0x25162032)
                                    )
                                )
                            }
                        )
                        .clickable {
                            onSelectAudioTrack(track)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) NovaAccent else Color.White
                        )
                        if (track.language.isNotBlank()) {
                            Text(
                                text = "Language: ${track.language}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0x99FFFFFF),
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = NovaAccent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
