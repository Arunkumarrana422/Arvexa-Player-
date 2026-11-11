package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.utils.UpdateInfo
import com.example.ui.utils.UpdateState
import java.io.File

@Composable
fun AppUpdateFullScreen(
    updateState: UpdateState,
    fallbackInfo: UpdateInfo?,
    onUpdateClick: (UpdateInfo) -> Unit,
    onInstallClick: (File) -> Unit,
    onDismiss: () -> Unit,
    onBrowserDownloadClick: ((String) -> Unit)? = null
) {
    val info = when (updateState) {
        is UpdateState.Available -> updateState.info
        is UpdateState.Downloading -> updateState.info
        is UpdateState.ReadyToInstall -> updateState.info
        else -> fallbackInfo
    } ?: return

    val isDownloading = updateState is UpdateState.Downloading
    val downloadPercent = if (updateState is UpdateState.Downloading) updateState.progressPercent else 0
    val isReadyToInstall = updateState is UpdateState.ReadyToInstall
    val readyFile = if (updateState is UpdateState.ReadyToInstall) updateState.apkFile else null

    BackHandler(enabled = !isDownloading) {
        onDismiss()
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Solid, 100% opaque background colors (NO transparency)
    val solidBgColor = if (isDark) Color(0xFF0B1020) else Color(0xFFF8FAFC)
    val cardBgColor = if (isDark) Color(0xFF151C30) else Color(0xFFFFFFFF)
    val cardBorderColor = if (isDark) Color(0xFF263254) else Color(0xFFE2E8F0)
    val backBtnBgColor = if (isDark) Color(0xFF1C2540) else Color(0xFFE2E8F0)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = solidBgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // TOP BAR with Back/Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (!isDownloading) onDismiss() },
                    modifier = Modifier
                        .size(42.dp)
                        .background(backBtnBgColor, CircleShape)
                        .testTag("update_screen_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "System Update",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    color = NovaAccent.copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Text(
                            text = "OFFICIAL",
                            color = NovaAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // SCROLLABLE CONTENT
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // APP LOGO
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                listOf(NovaPrimary, NovaSecondary, NovaAccent)
                            ),
                            CircleShape
                        )
                        .padding(4.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFF00D2FF),
                                    Color(0xFF3B82F6)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Arvexa Player Logo",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // APP NAME
                Text(
                    text = "Arvexa Player",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "HD Video & Audio Media Player",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // VERSION BADGES
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = NovaAccent.copy(alpha = 0.15f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, NovaAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "New: ${info.versionName}",
                            color = NovaAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        color = cardBgColor,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Text(
                            text = "Installed: v${BuildConfig.VERSION_NAME}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    if (info.fileSizeFormatted.isNotBlank()) {
                        Surface(
                            color = cardBgColor,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, cardBorderColor)
                        ) {
                            Text(
                                text = info.fileSizeFormatted,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // WHAT'S NEW SECTION (Solid Card)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBgColor
                    ),
                    border = BorderStroke(1.dp, cardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NovaAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "What's New in this Update",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = info.changelog.ifBlank { "Performance improvements, enhanced video engine, and bug fixes." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // INSTRUCTIONS SECTION (Solid Card)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBgColor
                    ),
                    border = BorderStroke(1.dp, cardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Update Instructions",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        InstructionStepRow(
                            stepNumber = "1",
                            title = "Download APK",
                            desc = "Tap 'Download' below to fetch the latest APK build directly."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        InstructionStepRow(
                            stepNumber = "2",
                            title = "Allow Permission",
                            desc = "If prompted by Android, grant 'Install unknown apps' permission to Arvexa Player."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        InstructionStepRow(
                            stepNumber = "3",
                            title = "Install & Keep Data",
                            desc = "Tap 'Install'. All your playlists, favorites, and watch history remain 100% safe."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // BOTTOM ACTION AREA (Pill-Shaped Buttons)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // DOWNLOAD PROGRESS (When Downloading)
                if (isDownloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (downloadPercent > 0) "Downloading update... $downloadPercent%" else "Connecting to download server...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (downloadPercent > 0) {
                            LinearProgressIndicator(
                                progress = { downloadPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape),
                                color = NovaAccent,
                                trackColor = cardBgColor
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape),
                                color = NovaAccent,
                                trackColor = cardBgColor
                            )
                        }
                    }
                }

                // ERROR BANNER (When Download fails e.g. 504 / network timeout)
                if (updateState is UpdateState.Error) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .background(Color(0x22EF4444), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x66EF4444), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = updateState.message,
                            color = Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onBrowserDownloadClick?.invoke(info.downloadUrl) },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, NovaAccent)
                        ) {
                            Text("Download in Browser", color = NovaAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // PILL-SHAPED ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Later Button (Pill Shape)
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("update_later_button"),
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, cardBorderColor)
                    ) {
                        Text(
                            text = "Later",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Primary Action Button (Pill Shape, Single-Line Text)
                    if (isReadyToInstall && readyFile != null) {
                        Button(
                            onClick = { onInstallClick(readyFile) },
                            modifier = Modifier
                                .weight(1.6f)
                                .height(52.dp)
                                .testTag("update_install_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Install",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Button(
                            onClick = { onUpdateClick(info) },
                            enabled = !isDownloading,
                            modifier = Modifier
                                .weight(1.6f)
                                .height(52.dp)
                                .testTag("update_download_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NovaAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDownloading) "Downloading..." else "Download",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionStepRow(
    stepNumber: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(NovaAccent.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, NovaAccent.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = NovaAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

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
                lineHeight = 16.sp
            )
        }
    }
}
