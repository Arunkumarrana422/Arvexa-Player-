package com.example.domain.model

import androidx.compose.ui.graphics.Color

enum class AppThemeColor(
    val id: String,
    val displayName: String,
    val hexCode: String,
    val colorValue: Long,
    val primaryValue: Long,
    val secondaryValue: Long
) {
    DEFAULT("default", "Default (Arvexa Cyan)", "#22D3EE", 0xFF22D3EE, 0xFF4F46E5, 0xFF2563EB),
    TEAL("teal", "Teal Turquoise", "#06B8A6", 0xFF06B8A6, 0xFF0D9488, 0xFF0F766E),
    PEACH("peach", "Peach Pink", "#FFC4BF", 0xFFFFC4BF, 0xFFE11D48, 0xFFFB7185),
    SKY_CYAN("sky_cyan", "Sky Cyan", "#80E8FF", 0xFF80E8FF, 0xFF0284C7, 0xFF0369A1),
    EMERALD("emerald", "Emerald Green", "#34A853", 0xFF34A853, 0xFF16A34A, 0xFF15803D),
    CORAL_RED("coral_red", "Coral Red", "#DC493A", 0xFFDC493A, 0xFFE11D48, 0xFFBE123C),
    LEMON("lemon", "Pastel Lemon", "#FFF49B", 0xFFFFF49B, 0xFFCA8A04, 0xFFA16207),
    LAVENDER("lavender", "Lavender Purple", "#D7B2FF", 0xFFD7B2FF, 0xFF9333EA, 0xFF7E22CE),
    MINT("mint", "Mint Green", "#B7FFBE", 0xFFB7FFBE, 0xFF10B981, 0xFF059669),
    APRICOT("apricot", "Apricot Orange", "#F0BC8C", 0xFFF0BC8C, 0xFFEA580C, 0xFFC2410C),
    ROYAL_BLUE("royal_blue", "Royal Blue", "#4392F1", 0xFF4392F1, 0xFF2563EB, 0xFF1D4ED8);

    val accent: Color get() = Color(colorValue)
    val primary: Color get() = Color(primaryValue)
    val secondary: Color get() = Color(secondaryValue)

    companion object {
        fun fromId(id: String?): AppThemeColor {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
