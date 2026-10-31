package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.Screen
import com.example.ui.theme.NovaAccent

@Composable
fun FloatingBottomNavigationBar(
    items: List<Screen>,
    selectedIndex: Int,
    onItemSelected: (Int, Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val surfaceColor = if (isDark) Color(0xFF0F172A) else MaterialTheme.colorScheme.surface
    val topBorderColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)

    // Full width bottom bar attached to bottom
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("floating_bottom_nav_bar"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = topBorderColor, thickness = 1.dp)
            Surface(
                color = surfaceColor,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex

                        FullWidthNavItem(
                            item = item,
                            isSelected = isSelected,
                            isDark = isDark,
                            onClick = { onItemSelected(index, item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FullWidthNavItem(
    item: Screen,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Shift active tab upwards
    val animatedOffsetY by animateDpAsState(
        targetValue = if (isSelected) (-8).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_item_offset_y"
    )

    val activeBgColor = if (isDark) NovaAccent.copy(alpha = 0.25f) else NovaAccent.copy(alpha = 0.18f)
    val inactiveBgColor = Color.Transparent

    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) activeBgColor else inactiveBgColor,
        animationSpec = tween(durationMillis = 200),
        label = "nav_item_bg"
    )

    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) NovaAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        animationSpec = tween(durationMillis = 200),
        label = "nav_item_icon_color"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.20f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nav_icon_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = NovaAccent),
                onClick = onClick
            )
            .testTag("nav_tab_${item.route}"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(y = animatedOffsetY)
                .size(width = 56.dp, height = 44.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(animatedBgColor),
            contentAlignment = Alignment.Center
        ) {
            item.icon?.let { iconVector ->
                Icon(
                    imageVector = iconVector,
                    contentDescription = item.title,
                    tint = animatedIconColor,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(iconScale)
                )
            }
        }
    }
}
