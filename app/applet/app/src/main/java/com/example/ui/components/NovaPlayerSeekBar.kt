package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Video
import com.example.ui.theme.NovaAccent

/**
 * Custom modern Video Player Seekbar:
 * - Rounded pill background track
 * - Active played progress bar with NovaAccent
 * - Circular thumb cursor for seeking
 * - Left & Right timestamp display with proper spacing and clickable remaining time toggle on right timestamp
 */
@Composable
fun NovaPlayerSeekBar(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }
    var showRemainingTime by remember { mutableStateOf(false) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val actualProgressFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayProgressFraction = if (isDragging) dragProgressFraction else actualProgressFraction
    val bufferedFraction = (bufferedPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val currentDisplayPosMs = (displayProgressFraction * safeDuration).toLong()

    val remainingMs = (safeDuration - currentDisplayPosMs).coerceAtLeast(0L)
    val rightTimestampText = if (showRemainingTime) {
        "-${Video.formatDuration(remainingMs)}"
    } else {
        Video.formatDuration(safeDuration)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left timestamp
        Text(
            text = Video.formatDuration(currentDisplayPosMs),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Custom Seekbar Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .testTag("nova_player_seekbar")
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeekTo((fraction * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgressFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeekTo((dragProgressFraction * safeDuration).toLong())
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragProgressFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                val width = size.width
                val height = size.height
                val trackHeight = 4.dp.toPx()
                val trackCornerRadius = trackHeight / 2f
                val trackTop = (height - trackHeight) / 2f

                // 1. Inactive Track (Dark Grey pill)
                drawRoundRect(
                    color = Color(0xFF404654),
                    topLeft = Offset(0f, trackTop),
                    size = Size(width, trackHeight),
                    cornerRadius = CornerRadius(trackCornerRadius, trackCornerRadius)
                )

                // 2. Buffered Track
                if (bufferedFraction > 0f) {
                    val bufferedWidth = width * bufferedFraction
                    drawRoundRect(
                        color = Color(0xFF5A6478),
                        topLeft = Offset(0f, trackTop),
                        size = Size(bufferedWidth, trackHeight),
                        cornerRadius = CornerRadius(trackCornerRadius, trackCornerRadius)
                    )
                }

                // 3. Active Progress Track
                val progressWidth = (width * displayProgressFraction).coerceIn(0f, width)
                if (progressWidth > 0f) {
                    drawRoundRect(
                        color = NovaAccent,
                        topLeft = Offset(0f, trackTop),
                        size = Size(progressWidth, trackHeight),
                        cornerRadius = CornerRadius(trackCornerRadius, trackCornerRadius)
                    )
                }

                // 4. Subtle buffer indicator dot on unplayed section if applicable
                if (bufferedFraction > displayProgressFraction && bufferedFraction < 0.99f) {
                    drawCircle(
                        color = NovaAccent.copy(alpha = 0.7f),
                        radius = 2.dp.toPx(),
                        center = Offset(width * bufferedFraction, height / 2f)
                    )
                }

                // 5. Thumb: Circular Thumb Cursor
                val thumbRadius = 7.dp.toPx()
                val thumbCenter = Offset(progressWidth.coerceIn(thumbRadius, width - thumbRadius), height / 2f)
                drawCircle(
                    color = NovaAccent,
                    radius = thumbRadius,
                    center = thumbCenter
                )
            }
        }

        // Right timestamp (Clickable to toggle remaining countdown time)
        Text(
            text = rightTimestampText,
            color = Color(0xCCFFFFFF),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable { showRemainingTime = !showRemainingTime }
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .testTag("right_timestamp")
        )
    }
}
