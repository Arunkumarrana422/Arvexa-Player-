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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.DarkColorScheme
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EqualizerBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var isEqEnabled by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf("Normal") }
    var band60Hz by remember { mutableFloatStateOf(0.6f) }
    var band230Hz by remember { mutableFloatStateOf(0.5f) }
    var band910Hz by remember { mutableFloatStateOf(0.7f) }
    var band3kHz by remember { mutableFloatStateOf(0.6f) }
    var band14kHz by remember { mutableFloatStateOf(0.8f) }
    var bassBoost by remember { mutableFloatStateOf(0.7f) }
    var virtualizer by remember { mutableFloatStateOf(0.5f) }

    val presets = listOf("Normal", "Pop", "Rock", "Jazz", "Bass Boost", "Vocal", "Heavy Metal", "Flat")

    MaterialTheme(colorScheme = DarkColorScheme) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color(0xFF0F172A),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("equalizer_bottom_sheet")
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
                                text = "Pro Equalizer & Sound FX",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Arvexa Audio Enhancement Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0x99FFFFFF),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isEqEnabled,
                            onCheckedChange = { isEqEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NovaAccent,
                                uncheckedTrackColor = Color(0xFF334155)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets
                Text(
                    text = "PRESETS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NovaAccent
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NovaAccent else Color(0xFF334155),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .background(if (isSelected) NovaPrimary.copy(alpha = 0.3f) else Color(0xFF1E293B))
                                .clickable {
                                    selectedPreset = preset
                                    when (preset) {
                                        "Normal" -> { band60Hz = 0.5f; band230Hz = 0.5f; band910Hz = 0.5f; band3kHz = 0.5f; band14kHz = 0.5f }
                                        "Pop" -> { band60Hz = 0.4f; band230Hz = 0.6f; band910Hz = 0.7f; band3kHz = 0.6f; band14kHz = 0.4f }
                                        "Rock" -> { band60Hz = 0.8f; band230Hz = 0.6f; band910Hz = 0.4f; band3kHz = 0.7f; band14kHz = 0.8f }
                                        "Jazz" -> { band60Hz = 0.6f; band230Hz = 0.5f; band910Hz = 0.4f; band3kHz = 0.6f; band14kHz = 0.7f }
                                        "Bass Boost" -> { band60Hz = 0.95f; band230Hz = 0.8f; band910Hz = 0.5f; band3kHz = 0.5f; band14kHz = 0.5f; bassBoost = 0.9f }
                                        "Vocal" -> { band60Hz = 0.3f; band230Hz = 0.5f; band910Hz = 0.9f; band3kHz = 0.8f; band14kHz = 0.4f }
                                        "Heavy Metal" -> { band60Hz = 0.85f; band230Hz = 0.7f; band910Hz = 0.3f; band3kHz = 0.8f; band14kHz = 0.9f }
                                        "Flat" -> { band60Hz = 0.5f; band230Hz = 0.5f; band910Hz = 0.5f; band3kHz = 0.5f; band14kHz = 0.5f }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = preset,
                                color = if (isSelected) Color.White else Color(0xCCFFFFFF),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 16.dp))

                // 5-Band Sliders
                Text(
                    text = "5-BAND EQUALIZER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NovaAccent
                )
                Spacer(modifier = Modifier.height(12.dp))

                EqBandSlider("60 Hz (Sub Bass)", band60Hz, isEqEnabled) { band60Hz = it; selectedPreset = "Custom" }
                EqBandSlider("230 Hz (Bass)", band230Hz, isEqEnabled) { band230Hz = it; selectedPreset = "Custom" }
                EqBandSlider("910 Hz (Midrange)", band910Hz, isEqEnabled) { band910Hz = it; selectedPreset = "Custom" }
                EqBandSlider("3 kHz (Upper Mid)", band3kHz, isEqEnabled) { band3kHz = it; selectedPreset = "Custom" }
                EqBandSlider("14 kHz (Treble)", band14kHz, isEqEnabled) { band14kHz = it; selectedPreset = "Custom" }

                HorizontalDivider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 16.dp))

                // Sound Effects: Bass Boost & 3D Virtualizer
                Text(
                    text = "SOUND ENHANCEMENTS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NovaAccent
                )
                Spacer(modifier = Modifier.height(12.dp))

                EqBandSlider("Bass Boost", bassBoost, isEqEnabled) { bassBoost = it }
                EqBandSlider("3D Surround Virtualizer", virtualizer, isEqEnabled) { virtualizer = it }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EqBandSlider(
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = if (enabled) Color.White else Color.Gray, fontSize = 12.sp)
            Text(text = "${(value * 100).toInt()}%", color = if (enabled) NovaAccent else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = NovaAccent,
                activeTrackColor = NovaAccent,
                inactiveTrackColor = Color(0xFF334155)
            )
        )
    }
}
