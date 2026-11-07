package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.domain.model.AppThemeColor

object ThemeColorManager {
    var currentThemeColor by mutableStateOf(AppThemeColor.DEFAULT)
        private set

    fun setAppThemeColor(themeColor: AppThemeColor) {
        currentThemeColor = themeColor
    }

    val currentAccent: Color get() = currentThemeColor.accent
    val currentPrimary: Color get() = currentThemeColor.primary
    val currentSecondary: Color get() = currentThemeColor.secondary
}

// Primary Branding - dynamically backed by ThemeColorManager
val NovaPrimary: Color get() = ThemeColorManager.currentPrimary
val NovaSecondary: Color get() = ThemeColorManager.currentSecondary
val NovaAccent: Color get() = ThemeColorManager.currentAccent

// Dark Theme Surfaces
val NovaDarkBackground = Color(0xFF0B1020)
val NovaDarkSurface = Color(0xFF151C30)
val NovaDarkCard = Color(0xFF1C2540)
val NovaDarkSurfaceVariant = Color(0xFF263254)

// Light Theme Surfaces
val NovaLightBackground = Color(0xFFF8FAFC)
val NovaLightSurface = Color(0xFFFFFFFF)
val NovaLightCard = Color(0xFFF1F5F9)
val NovaLightSurfaceVariant = Color(0xFFE2E8F0)

// Text & Status
val NovaTextPrimary = Color(0xFFF8FAFC)
val NovaTextSecondary = Color(0xFF94A3B8)
val NovaTextMuted = Color(0xFF64748B)

val NovaLightTextPrimary = Color(0xFF0F172A)
val NovaLightTextSecondary = Color(0xFF475569)

val NovaSuccess = Color(0xFF22C55E)
val NovaError = Color(0xFFEF4444)
val NovaWarning = Color(0xFFF59E0B)
val NovaInfo = Color(0xFF38BDF8)
