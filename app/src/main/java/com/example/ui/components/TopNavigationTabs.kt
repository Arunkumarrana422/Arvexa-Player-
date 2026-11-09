package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.Screen
import com.example.ui.theme.NovaAccent

@Composable
fun TopNavigationTabs(
    items: List<Screen>,
    selectedIndex: Int,
    onItemSelected: (Int, Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)

    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth().testTag("top_navigation_tabs")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                val itemCount = items.size.coerceAtLeast(1)
                val tabWidth = maxWidth / itemCount
                val safeSelectedIndex = selectedIndex.coerceIn(0, itemCount - 1)

                // Smooth sliding animation for active tab underline indicator
                val animatedIndicatorOffset by animateDpAsState(
                    targetValue = tabWidth * safeSelectedIndex,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "tab_indicator_offset"
                )

                // Row of clickable tab icons
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex

                        TopNavItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = { onItemSelected(index, item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Smoothly sliding active underline indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = animatedIndicatorOffset)
                        .width(tabWidth)
                        .height(3.5.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.70f)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(NovaAccent)
                    )
                }
            }

            HorizontalDivider(color = bottomBorderColor, thickness = 1.dp)
        }
    }
}

@Composable
private fun TopNavItem(
    item: Screen,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) NovaAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(durationMillis = 220),
        label = "top_nav_icon_color"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = NovaAccent),
                    onClick = onClick
                )
                .testTag("top_nav_tab_${item.route}"),
            contentAlignment = Alignment.Center
        ) {
            item.icon?.let { iconVector ->
                Icon(
                    imageVector = iconVector,
                    contentDescription = item.title,
                    tint = animatedIconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
