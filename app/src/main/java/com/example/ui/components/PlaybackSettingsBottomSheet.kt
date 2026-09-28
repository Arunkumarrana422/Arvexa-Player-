package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AspectRatioMode
import com.example.domain.model.AudioTrack
import com.example.domain.model.DecoderMode
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.isNightMode
import com.example.ui.theme.nightGlassBorder

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaybackSettingsBottomSheet(
    currentSpeed: Float,
    currentAspectRatio: AspectRatioMode,
    decoderMode: DecoderMode,
    audioTracks: List<AudioTrack>,
    selectedAudioTrack: AudioTrack?,
    sleepTimerMinutes: Int?,
    backgroundAudioEnabled: Boolean,
    hwDecoderEnabled: Boolean,
    onSpeedChange: (Float) -> Unit,
    onAspectRatioChange: (AspectRatioMode) -> Unit,
    onDecoderModeChange: (DecoderMode) -> Unit,
    onSelectAudioTrack: (AudioTrack) -> Unit,
    onSleepTimerChange: (Int?) -> Unit,
    onBackgroundAudioToggle: (Boolean) -> Unit,
    onHwDecoderToggle: (Boolean) -> Unit,
    onEnterPiP: () -> Unit,
    onDismiss: () -> Unit
) {
    val speedPresets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    val sleepTimerPresets = listOf(null to "Off", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "60 min")
    val isDark = isNightMode()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
            .testTag("playback_settings_sheet"),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(380.dp)
                .clickable(enabled = false, onClick = {}),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playback Controls & Audio",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playback Speed
            SectionHeader(icon = Icons.Default.Speed, title = "PLAYBACK SPEED (${currentSpeed}x)")
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                speedPresets.forEach { speed ->
                    OptionChip(
                        text = "${speed}x",
                        isSelected = kotlin.math.abs(currentSpeed - speed) < 0.01f,
                        onClick = { onSpeedChange(speed) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

            // Aspect Ratio
            SectionHeader(icon = Icons.Default.AspectRatio, title = "ASPECT RATIO")
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AspectRatioMode.values().forEach { mode ->
                    OptionChip(
                        text = mode.label,
                        isSelected = currentAspectRatio == mode,
                        onClick = { onAspectRatioChange(mode) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

            // Decoder Mode (HW / HW+ / SW)
            SectionHeader(icon = Icons.Default.Memory, title = "DECODER MODE (HW / HW+ / SW)")
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DecoderMode.values().forEach { mode ->
                    OptionChip(
                        text = mode.label,
                        isSelected = decoderMode == mode,
                        onClick = { onDecoderModeChange(mode) }
                    )
                }
            }

            if (audioTracks.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                SectionHeader(icon = Icons.Default.Audiotrack, title = "AUDIO STREAM")
                Spacer(modifier = Modifier.height(8.dp))

                audioTracks.forEach { track ->
                    val isSelected = selectedAudioTrack?.id == track.id || track.isSelected
                    val audioTrackBorder = if (isSelected) {
                        if (isDark) nightGlassBorder(strokeWidth = 1.5.dp, intensity = 1.35f) ?: BorderStroke(1.5.dp, NovaAccent)
                        else BorderStroke(1.5.dp, Color(0xFF22D3EE).copy(alpha = 0.85f))
                    } else {
                        BorderStroke(1.dp, if (isDark) Color(0xFF2A374F).copy(alpha = 0.6f) else Color(0xFFE2E8F0))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(audioTrackBorder, RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) {
                                    if (isDark) {
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.08f),
                                                Color(0x2222D3EE),
                                                Color(0x350B1020)
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(Color(0xFFE0F7FA), Color(0xFFE0F2FE)))
                                    }
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            if (isDark) Color(0xFF162032).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            if (isDark) Color(0xFF162032).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            )
                            .clickable { onSelectAudioTrack(track) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) NovaAccent else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Language: ${track.language}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = NovaAccent)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

            // Sleep Timer
            SectionHeader(icon = Icons.Default.Bedtime, title = "SLEEP TIMER")
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sleepTimerPresets.forEach { (mins, label) ->
                    OptionChip(
                        text = label,
                        isSelected = sleepTimerMinutes == mins,
                        onClick = { onSleepTimerChange(mins) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

            // Switches
            ToggleRow(
                icon = Icons.Default.Headphones,
                title = "Background Audio Playback",
                description = "Continue playing audio when app is minimized or screen is locked",
                checked = backgroundAudioEnabled,
                onCheckedChange = onBackgroundAudioToggle
            )

            Spacer(modifier = Modifier.height(8.dp))

            ToggleRow(
                icon = Icons.Default.Memory,
                title = "Hardware Acceleration (HW+)",
                description = "Use device GPU hardware video decoding for smoother 4K/60fps playback",
                checked = hwDecoderEnabled,
                onCheckedChange = onHwDecoderToggle
            )

            Spacer(modifier = Modifier.height(8.dp))

            val pipBorder = if (isDark) nightGlassBorder(strokeWidth = 1.dp, intensity = 1.1f) ?: BorderStroke(1.dp, Color(0xFF2A374F).copy(alpha = 0.6f))
                            else BorderStroke(1.dp, Color(0xFFE2E8F0))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(pipBorder, RoundedCornerShape(14.dp))
                    .background(
                        if (isDark) {
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.05f),
                                    Color(0x1522D3EE),
                                    Color(0x250B1020)
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0).copy(alpha = 0.5f)))
                        }
                    )
                    .clickable {
                        onDismiss()
                        onEnterPiP()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PictureInPicture, contentDescription = null, tint = NovaAccent)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Picture-in-Picture (PiP) Mode",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Float video in a movable mini-window on screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = NovaAccent, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OptionChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isNightMode()
    val chipShape = RoundedCornerShape(50) // Full Stadium / Pill Shape

    val borderStroke = if (isSelected) {
        if (isDark) {
            nightGlassBorder(strokeWidth = 1.5.dp, intensity = 1.35f) ?: BorderStroke(1.5.dp, NovaAccent)
        } else {
            BorderStroke(1.5.dp, Color(0xFF22D3EE).copy(alpha = 0.85f))
        }
    } else {
        BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF2A374F).copy(alpha = 0.6f) else Color(0xFFCBD5E1).copy(alpha = 0.7f)
        )
    }

    Box(
        modifier = Modifier
            .clip(chipShape)
            .border(borderStroke, chipShape)
            .background(
                if (isSelected) {
                    if (isDark) {
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color(0x2822D3EE),
                                Color(0x350B1020)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFE0F7FA),
                                Color(0xFFE0F2FE)
                            )
                        )
                    }
                } else {
                    Brush.linearGradient(
                        listOf(
                            if (isDark) Color(0xFF162032).copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            if (isDark) Color(0xFF162032).copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) {
                if (isDark) Color.White else Color(0xFF007A99)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.5.sp
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NovaAccent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NovaAccent,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
