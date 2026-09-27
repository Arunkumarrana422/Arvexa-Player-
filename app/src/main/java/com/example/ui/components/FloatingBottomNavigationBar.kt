package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.theme.NovaAccent
import com.example.ui.theme.NovaPrimary

@Composable
fun FloatingBottomNavigationBar(
    items: List<Screen>,
    selectedIndex: Int,
    onItemSelected: (Int, Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Premium Glass Effect Border specifically tailored for Night Mode
    val navBorder = if (isDark) {
        BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.38f),
                    NovaAccent.copy(alpha = 0.65f),
                    NovaPrimary.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.15f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        )
    } else {
        BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    }

    val surfaceColor = if (isDark) {
        Color(0xFF131B2E)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("floating_bottom_nav_bar"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = surfaceColor,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
            border = navBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = if (isDark) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.15f)
                            )
                        )
                    )
                } else Modifier
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex

                        FloatingNavItem(
                            item = item,
                            isSelected = isSelected,
                            isDark = isDark,
                            onClick = { onItemSelected(index, item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    item: Screen,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Smooth color animation with night mode glass styling
    val activePillColor = if (isDark) NovaAccent.copy(alpha = 0.22f) else NovaAccent.copy(alpha = 0.16f)
    val inactivePillColor = Color.Transparent

    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) activePillColor else inactivePillColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_item_bg"
    )

    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) NovaAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        animationSpec = tween(durationMillis = 220),
        label = "nav_item_icon_color"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nav_icon_scale"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(animatedBgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = NovaAccent),
                onClick = onClick
            )
            .padding(horizontal = if (isSelected) 14.dp else 12.dp, vertical = 8.dp)
            .testTag("nav_tab_${item.route}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            item.icon?.let { iconVector ->
                Icon(
                    imageVector = iconVector,
                    contentDescription = item.title,
                    tint = animatedIconColor,
                    modifier = Modifier
                        .size(22.dp)
                        .scale(iconScale)
                )
            }

            // Smooth expansion/transfer animation for the label
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(180, delayMillis = 40)) +
                        expandHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            expandFrom = Alignment.Start
                        ),
                exit = fadeOut(animationSpec = tween(120)) +
                        shrinkHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            shrinkTowards = Alignment.Start
                        )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.title,
                        color = NovaAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
