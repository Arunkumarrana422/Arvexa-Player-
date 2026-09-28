package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.theme.isNightMode
import com.example.ui.theme.nightGlassBorder

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    val isDark = isNightMode()

    // Matching pill colors for the round outline button
    val lightBg = Color(0xFFE0F7FA)
    val lightBorder = Color(0xFF22D3EE).copy(alpha = 0.85f)
    val lightContent = Color(0xFF007A99) // Deep sharp cyan
    val darkContent = NovaAccent // Vibrant neon cyan #22D3EE
    val darkBtnBg = Color(0xFF131B2E).copy(alpha = 0.75f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Icon Graphic with glowing ring
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                NovaPrimary,
                                NovaSecondary,
                                NovaAccent
                            )
                        )
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF0F172A) else Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NovaPrimary, NovaSecondary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Welcome to Arvexa Player",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "The ultimate video and audio playback engine for Android with fluid touch gestures, multi-format support, and streaming.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Feature Highlights Cards with Glass borders in Night Mode
            TutorialCard(
                icon = Icons.Default.BrightnessMedium,
                title = "Swipe Left: Brightness",
                desc = "Drag up/down on the left side of the screen to adjust display brightness."
            )

            Spacer(modifier = Modifier.height(10.dp))

            TutorialCard(
                icon = Icons.Default.VolumeUp,
                title = "Swipe Right: Volume",
                desc = "Drag up/down on the right side of the screen to smoothly change volume."
            )

            Spacer(modifier = Modifier.height(10.dp))

            TutorialCard(
                icon = Icons.Default.FastForward,
                title = "Swipe & Double Tap: Seek",
                desc = "Swipe horizontally to seek, or double tap the edges to skip 10 seconds."
            )

            Spacer(modifier = Modifier.height(10.dp))

            TutorialCard(
                icon = Icons.Default.Speed,
                title = "Long Press: 2X Speed Boost",
                desc = "Hold your finger anywhere on the screen during playback for an instant 2x fast-forward."
            )

            Spacer(modifier = Modifier.height(10.dp))

            TutorialCard(
                icon = Icons.Default.Subtitles,
                title = "Subtitles & Custom Sync",
                desc = "Load .srt/.vtt files, switch audio streams, and fine-tune subtitle synchronization delay."
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Fully Rounded Stadium Pill Button with matching border/theme color (No heavy solid fill)
        Surface(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_start_button"),
            shape = RoundedCornerShape(50),
            color = if (isDark) darkBtnBg else lightBg,
            border = if (isDark) nightGlassBorder(strokeWidth = 1.5.dp, intensity = 1.4f) else BorderStroke(1.5.dp, lightBorder),
            shadowElevation = if (isDark) 0.dp else 2.dp
        ) {
            Box(
                modifier = if (isDark) {
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
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = if (isDark) darkContent else lightContent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (isDark) darkContent else lightContent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialCard(
    icon: ImageVector,
    title: String,
    desc: String
) {
    val isDark = isNightMode()
    val cardBg = if (isDark) Color(0xFF131B2E).copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = nightGlassBorder(strokeWidth = 1.dp, intensity = 1.2f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            if (isDark) listOf(NovaPrimary.copy(alpha = 0.35f), NovaSecondary.copy(alpha = 0.25f))
                            else listOf(Color(0xFFE0F7FA), Color(0xFFCCFBF1))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDark) NovaAccent else Color(0xFF007A99),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

