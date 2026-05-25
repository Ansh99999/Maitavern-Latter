package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class ActiveThemeColors(
    val name: String,
    val isDark: Boolean,
    val background: Color,
    val cardBackground: Color,
    val aiBubble: Color,
    val userBubble: Color,
    val accent: Color,
    val text: Color,
    val textSecondary: Color
)

object ThemePresets {
    val MidnightVoid = ActiveThemeColors(
        name = "Midnight Void",
        isDark = true,
        background = Color(0xFF07070B),
        cardBackground = Color(0xFF0F0F19),
        aiBubble = Color(0xFF141424),
        userBubble = Color(0xFF1B223C),
        accent = Color(0xFFE94560),
        text = Color(0xFFE5E6ED),
        textSecondary = Color(0xFF9499B3)
    )

    val ParchmentTavern = ActiveThemeColors(
        name = "Parchment Tavern",
        isDark = false,
        background = Color(0xFFFAF5E8),
        cardBackground = Color(0xFFF0E5CE),
        aiBubble = Color(0xFFEADBCE),
        userBubble = Color(0xFFDFCCB7),
        accent = Color(0xFFAB2328),
        text = Color(0xFF2E221D),
        textSecondary = Color(0xFF635249)
    )

    val CyberpunkNeon = ActiveThemeColors(
        name = "Cyberpunk Neon",
        isDark = true,
        background = Color(0xFF030303),
        cardBackground = Color(0xFF0F141A),
        aiBubble = Color(0xFF161E26),
        userBubble = Color(0xFF07303B),
        accent = Color(0xFF00F0FF),
        text = Color(0xFFE0F7FA),
        textSecondary = Color(0xFF4F8394)
    )

    val ForestSanctuary = ActiveThemeColors(
        name = "Forest Sanctuary",
        isDark = true,
        background = Color(0xFF0F1411),
        cardBackground = Color(0xFF1A241F),
        aiBubble = Color(0xFF22312A),
        userBubble = Color(0xFF2A4235),
        accent = Color(0xFF81C784),
        text = Color(0xFFE8ECE9),
        textSecondary = Color(0xFF7D8F83)
    )

    val FrostedGlass = ActiveThemeColors(
        name = "Frosted Glass",
        isDark = true,
        background = Color(0xFF0F1115),
        cardBackground = Color(0x19FFFFFF), // 10% white for frosted glow
        aiBubble = Color(0x0EFFFFFF), // 5.5% white
        userBubble = Color(0xFF3F4759),
        accent = Color(0xFF818CF8), // Indigo, as present in the design HTML
        text = Color(0xFFE2E2E6),
        textSecondary = Color(0xFF9E9E9E)
    )

    fun getTheme(name: String): ActiveThemeColors {
        return when (name) {
            "Frosted Glass" -> FrostedGlass
            "Midnight Void" -> MidnightVoid
            "Parchment Tavern" -> ParchmentTavern
            "Cyberpunk Neon" -> CyberpunkNeon
            "Forest Sanctuary" -> ForestSanctuary
            else -> FrostedGlass
        }
    }
}
