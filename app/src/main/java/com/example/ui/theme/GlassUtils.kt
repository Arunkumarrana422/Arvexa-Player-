package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun isNightMode(): Boolean {
    return MaterialTheme.colorScheme.surface.luminance() < 0.5f
}

@Composable
fun nightGlassBorder(
    isCurrentlyPlaying: Boolean = false,
    strokeWidth: Dp = 1.dp,
    intensity: Float = 1.0f
): BorderStroke? {
    if (isCurrentlyPlaying) {
        return BorderStroke(1.5.dp, NovaAccent)
    }
    val isDark = isNightMode()
    return if (isDark) {
        BorderStroke(
            width = strokeWidth,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = (0.35f * intensity).coerceIn(0f, 1f)),
                    NovaAccent.copy(alpha = (0.55f * intensity).coerceIn(0f, 1f)),
                    NovaPrimary.copy(alpha = (0.30f * intensity).coerceIn(0f, 1f)),
                    Color.White.copy(alpha = (0.12f * intensity).coerceIn(0f, 1f))
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        )
    } else {
        null
    }
}
