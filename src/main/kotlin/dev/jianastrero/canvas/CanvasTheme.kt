package dev.jianastrero.canvas

import java.awt.Color

data class CanvasTheme(
    val background: Color,
    val nodeBg: Color,
    val nodeLabel: Color,
    val edgeLabel: Color,
    val edge: Color,
    val initial: Color,
    val terminal: Color,
    val regular: Color,
    val piggyEnter: Color,
    val piggyExit: Color
) {
    val isDark: Boolean
        get() = (background.red * 0.299 + background.green * 0.587 + background.blue * 0.114) < 128

    companion object {
        fun light(bgOverride: Color? = null) = CanvasTheme(
            background = bgOverride ?: Color(0xF5F5F5),
            nodeBg     = Color(0xFAFAFA),
            nodeLabel  = Color(0x37474F),
            edgeLabel  = Color(0x78909C),
            edge       = Color(0x546E7A),
            initial    = Color(0x4CAF50),
            terminal   = Color(0xFF9800),
            regular    = Color(0x90A4AE),
            piggyEnter = Color(0xA5D6A7),
            piggyExit  = Color(0xEF9A9A)
        )

        fun dark(bgOverride: Color? = null) = CanvasTheme(
            background = bgOverride ?: Color(0x1A1A1A),
            nodeBg     = Color(0x2C2C2C),
            nodeLabel  = Color(0xE8E8E8),
            edgeLabel  = Color(0xAAAAAA),
            edge       = Color(0x90A4AE),
            initial    = Color(0x66BB6A),
            terminal   = Color(0xFFA726),
            regular    = Color(0x78909C),
            piggyEnter = Color(0x4CAF50),
            piggyExit  = Color(0xEF5350)
        )
    }
}
