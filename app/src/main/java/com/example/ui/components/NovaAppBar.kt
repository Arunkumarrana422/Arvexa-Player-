package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ThemePreference
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.HorizontalDivider
import com.example.domain.model.SortOption
import com.example.domain.model.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaAppBar(
    title: String = "Arvexa Player",
    isRefreshing: Boolean = false,
    isUpdateAvailable: Boolean = false,
    currentViewMode: ViewMode = ViewMode.LIST,
    currentSortOption: SortOption = SortOption.DATE_DESC,
    showViewSortButton: Boolean = true,
    onViewModeChange: (ViewMode) -> Unit = {},
    onSortOptionChange: (SortOption) -> Unit = {},
    onUpdateClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    var showViewSortDialog by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 0.dp
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            actions = {
                // Glowing Red Update Icon (Clean icon only, no outer circle/border)
                if (isUpdateAvailable) {
                    val infiniteTransition = rememberInfiniteTransition(label = "update_glow_anim")
                    val iconAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.55f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow_alpha"
                    )

                    IconButton(
                        onClick = onUpdateClick,
                        modifier = Modifier.testTag("appbar_update_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "New Update Available",
                            tint = Color(0xFFEF4444).copy(alpha = iconAlpha),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.testTag("appbar_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Filter & View Options Button (Between Search and Settings)
                if (showViewSortButton) {
                    IconButton(
                        onClick = { showViewSortDialog = true },
                        modifier = Modifier.testTag("appbar_filter_view_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort and View Options",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    ViewAndSortDialog(
                        isOpen = showViewSortDialog,
                        onDismissRequest = { showViewSortDialog = false },
                        currentViewMode = currentViewMode,
                        currentSortOption = currentSortOption,
                        onApply = { viewMode, sortOption ->
                            onViewModeChange(viewMode)
                            onSortOptionChange(sortOption)
                        }
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("appbar_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
    }
}

@Composable
fun ViewAndSortDialog(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    currentViewMode: ViewMode,
    currentSortOption: SortOption,
    onApply: (ViewMode, SortOption) -> Unit
) {
    if (!isOpen) return

    var selectedViewMode by remember(currentViewMode, isOpen) { mutableStateOf(currentViewMode) }
    var selectedSortOption by remember(currentSortOption, isOpen) { mutableStateOf(currentSortOption) }
    val themeColor = NovaAccent

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "View & Sort Options",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Section: VIEW MODE
                Text(
                    text = "VIEW MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = themeColor,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedViewMode = ViewMode.LIST }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedViewMode == ViewMode.LIST),
                        onClick = { selectedViewMode = ViewMode.LIST }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.Default.ViewList, contentDescription = null, tint = if (selectedViewMode == ViewMode.LIST) themeColor else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "List View", fontWeight = if (selectedViewMode == ViewMode.LIST) FontWeight.Bold else FontWeight.Normal, color = if (selectedViewMode == ViewMode.LIST) themeColor else MaterialTheme.colorScheme.onSurface)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedViewMode = ViewMode.GRID }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedViewMode == ViewMode.GRID),
                        onClick = { selectedViewMode = ViewMode.GRID }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.Default.GridView, contentDescription = null, tint = if (selectedViewMode == ViewMode.GRID) themeColor else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Grid View (2 Columns)", fontWeight = if (selectedViewMode == ViewMode.GRID) FontWeight.Bold else FontWeight.Normal, color = if (selectedViewMode == ViewMode.GRID) themeColor else MaterialTheme.colorScheme.onSurface)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Section: SORT BY
                Text(
                    text = "SORT BY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = themeColor,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                SortOption.values().forEach { option ->
                    val isSelected = (option == selectedSortOption)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedSortOption = option }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedSortOption = option }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = option.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(selectedViewMode, selectedSortOption)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Text("Confirm", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
