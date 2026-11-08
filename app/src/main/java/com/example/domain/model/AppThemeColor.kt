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
    PEACH("peach", "Peach Pink", "#FB7185", 0xFFFB7185, 0xFFE11D48, 0xFFFB7185),
    SKY_CYAN("sky_cyan", "Sky Cyan", "#38BDF8", 0xFF38BDF8, 0xFF0284C7, 0xFF0369A1),
    EMERALD("emerald", "Emerald Green", "#22C55E", 0xFF22C55E, 0xFF16A34A, 0xFF15803D),
    CORAL_RED("coral_red", "Coral Red", "#EF4444", 0xFFEF4444, 0xFFE11D48, 0xFFBE123C),
    LEMON("lemon", "Pastel Lemon", "#EAB308", 0xFFEAB308, 0xFFCA8A04, 0xFFA16207),
    LAVENDER("lavender", "Lavender Purple", "#C084FC", 0xFFC084FC, 0xFF9333EA, 0xFF7E22CE),
    MINT("mint", "Mint Green", "#10B981", 0xFF10B981, 0xFF10B981, 0xFF059669),
    APRICOT("apricot", "Apricot Orange", "#F97316", 0xFFF97316, 0xFFEA580C, 0xFFC2410C),
    ROYAL_BLUE("royal_blue", "Royal Blue", "#3B82F6", 0xFF3B82F6, 0xFF2563EB, 0xFF1D4ED8);

    val accent: Color get() = Color(colorValue)
    val primary: Color get() = Color(primaryValue)
    val secondary: Color get() = Color(secondaryValue)

    companion object {
        fun fromId(id: String?): AppThemeColor {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
