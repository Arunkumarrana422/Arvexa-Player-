package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerPanelContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isForVideo: Boolean = true
) {
    val audioPlayerManager = remember { com.example.player.AudioPlayerManager.activeInstance }
    val novaPlayerManager = remember { com.example.player.NovaPlayerManager.activeInstance }

    val isEqEnabledState = if (isForVideo) {
        novaPlayerManager?.isEqualizerEnabled?.collectAsState()
    } else {
        audioPlayerManager?.isEqualizerEnabled?.collectAsState()
    }
    val isEqEnabled = isEqEnabledState?.value ?: false

    val selectedPresetState = if (isForVideo) {
        novaPlayerManager?.selectedPreset?.collectAsState()
    } else {
        audioPlayerManager?.selectedPreset?.collectAsState()
    }
    val selectedPreset = selectedPresetState?.value ?: "Normal"

    val bandLevelsState = if (isForVideo) {
        novaPlayerManager?.bandLevels?.collectAsState()
    } else {
        audioPlayerManager?.bandLevels?.collectAsState()
    }
    val bandLevels = bandLevelsState?.value ?: listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)

    val bassBoostState = if (isForVideo) {
        novaPlayerManager?.bassBoostLevel?.collectAsState()
    } else {
        audioPlayerManager?.bassBoostLevel?.collectAsState()
    }
    val bassBoost = bassBoostState?.value ?: 0.5f

    val virtualizerState = if (isForVideo) {
        novaPlayerManager?.virtualizerLevel?.collectAsState()
    } else {
        audioPlayerManager?.virtualizerLevel?.collectAsState()
    }
    val virtualizer = virtualizerState?.value ?: 0.5f

    // "Custom" is placed first before "Normal"
    val presets = listOf("Custom", "Normal", "Pop", "Rock", "Jazz", "Bass Boost", "Vocal", "Heavy Metal", "Flat")

    // Theme color definitions
    val titleTextColor = if (isForVideo) Color.White else MaterialTheme.colorScheme.onSurface
    val subtitleTextColor = if (isForVideo) Color(0xB3FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = if (isForVideo) Color(0x26FFFFFF) else MaterialTheme.colorScheme.outlineVariant
    val unselectedChipBg = if (isForVideo) Color(0x22FFFFFF) else MaterialTheme.colorScheme.surfaceVariant
    val unselectedChipBorder = if (isForVideo) Color(0x33FFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val unselectedChipText = if (isForVideo) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("equalizer_panel_content")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Brush.linearGradient(listOf(NovaPrimary, NovaSecondary)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Equalizer, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Equalizer & Sound FX",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = titleTextColor
                    )
                    Text(
                        text = "Audio Enhancement Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleTextColor,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = isEqEnabled,
                onCheckedChange = { enabled ->
                    if (isForVideo) {
                        novaPlayerManager?.setEqualizerEnabled(enabled)
                    } else {
                        audioPlayerManager?.setEqualizerEnabled(enabled)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NovaAccent,
                    uncheckedTrackColor = if (isForVideo) Color(0x33FFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    uncheckedThumbColor = if (isForVideo) Color(0x88FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("equalizer_master_switch")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Presets Section (Disabled when Equalizer is OFF)
        Text(
            text = "PRESETS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isEqEnabled) NovaAccent else NovaAccent.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = if (isEqEnabled) 1f else 0.4f }
        ) {
            presets.forEach { preset ->
                val isSelected = selectedPreset == preset && isEqEnabled
                val chipBorder = if (!isEqEnabled) unselectedChipBorder.copy(alpha = 0.2f)
                                 else if (isSelected) NovaAccent
                                 else unselectedChipBorder
                val chipBg = if (!isEqEnabled) unselectedChipBg.copy(alpha = 0.3f)
                             else if (isSelected) NovaAccent.copy(alpha = 0.25f)
                             else unselectedChipBg
                val chipTextColor = if (!isEqEnabled) unselectedChipText.copy(alpha = 0.4f)
                                    else if (isSelected) NovaAccent
                                    else unselectedChipText

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = chipBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .background(chipBg)
                        .clickable(enabled = isEqEnabled) {
                            if (isEqEnabled) {
                                if (isForVideo) {
                                    novaPlayerManager?.setPreset(preset)
                                } else {
                                    audioPlayerManager?.setPreset(preset)
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("eq_preset_$preset")
                ) {
                    Text(
                        text = preset,
                        color = chipTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 16.dp))

        // 5-Band Sliders (Disabled when Equalizer is OFF)
        Text(
            text = "5-BAND EQUALIZER",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isEqEnabled) NovaAccent else NovaAccent.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        val bandNames = listOf(
            "60 Hz (Sub Bass)",
            "230 Hz (Bass)",
            "910 Hz (Midrange)",
            "3 kHz (Upper Mid)",
            "14 kHz (Treble)"
        )

        bandNames.forEachIndexed { index, name ->
            val value = bandLevels.getOrElse(index) { 0.5f }
            EqBandSlider(
                label = name,
                value = value,
                enabled = isEqEnabled,
                isForVideo = isForVideo,
                onValueChange = { newVal ->
                    if (isEqEnabled) {
                        if (isForVideo) {
                            novaPlayerManager?.setBandLevel(index, newVal)
                        } else {
                            audioPlayerManager?.setBandLevel(index, newVal)
                        }
                    }
                }
            )
        }

        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 16.dp))

        // Sound Effects: Bass Boost & 3D Virtualizer (Disabled when Equalizer is OFF)
        Text(
            text = "SOUND ENHANCEMENTS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isEqEnabled) NovaAccent else NovaAccent.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        EqBandSlider(
            label = "Bass Boost",
            value = bassBoost,
            enabled = isEqEnabled,
            isForVideo = isForVideo,
            onValueChange = { newVal ->
                if (isEqEnabled) {
                    if (isForVideo) {
                        novaPlayerManager?.setBassBoost(newVal)
                    } else {
                        audioPlayerManager?.setBassBoost(newVal)
                    }
                }
            }
        )

        EqBandSlider(
            label = "3D Surround Virtualizer",
            value = virtualizer,
            enabled = isEqEnabled,
            isForVideo = isForVideo,
            onValueChange = { newVal ->
                if (isEqEnabled) {
                    if (isForVideo) {
                        novaPlayerManager?.setVirtualizer(newVal)
                    } else {
                        audioPlayerManager?.setVirtualizer(newVal)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqBandSlider(
    label: String,
    value: Float,
    enabled: Boolean,
    isForVideo: Boolean,
    onValueChange: (Float) -> Unit
) {
    val enabledTextColor = if (isForVideo) Color.White else MaterialTheme.colorScheme.onSurface
    val disabledTextColor = if (isForVideo) Color(0x66FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val inactiveTrackColor = if (isForVideo) Color(0x33FFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val thumbBorderColor = if (isForVideo) Color.White else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = if (enabled) enabledTextColor else disabledTextColor,
                fontSize = 12.sp
            )
            Text(
                text = "${(value * 100).toInt()}%",
                color = if (enabled) NovaAccent else disabledTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = NovaAccent,
                activeTrackColor = NovaAccent,
                inactiveTrackColor = inactiveTrackColor,
                disabledThumbColor = disabledTextColor,
                disabledActiveTrackColor = inactiveTrackColor.copy(alpha = 0.5f),
                disabledInactiveTrackColor = inactiveTrackColor.copy(alpha = 0.2f)
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (enabled) NovaAccent else disabledTextColor,
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = thumbBorderColor,
                            shape = CircleShape
                        )
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(3.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = NovaAccent,
                        inactiveTrackColor = inactiveTrackColor
                    )
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    onDismiss: () -> Unit,
    isForVideo: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isForVideo) Color(0x8C0F172A) else MaterialTheme.colorScheme.surface,
        contentColor = if (isForVideo) Color.White else MaterialTheme.colorScheme.onSurface,
        dragHandle = null
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            EqualizerPanelContent(
                onDismiss = onDismiss,
                isForVideo = isForVideo
            )
        }
    }
}
