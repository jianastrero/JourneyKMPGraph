package dev.jianastrero.canvas.painter

import dev.jianastrero.canvas.CanvasTheme
import java.awt.*

object EmptyStatePainter {
    fun paint(g: Graphics2D, theme: CanvasTheme, width: Int, height: Int) {
        val cx = width / 2;  val cy = height / 2

        drawMiniGraph(g, theme, cx, cy)

        val nw = 52; val nh = 26
        val titleY = (cy - 62) + nh + 32

        g.font = Font("SansSerif", Font.BOLD, 14);  g.color = theme.nodeLabel
        val title = "No @Journey found"
        g.drawString(title, cx - g.fontMetrics.stringWidth(title) / 2, titleY)

        g.font = Font("SansSerif", Font.PLAIN, 12);  g.color = theme.edgeLabel
        val sub = "Open a file with a @Journey-annotated sealed interface"
        g.drawString(sub, cx - g.fontMetrics.stringWidth(sub) / 2, titleY + 22)

        drawCodeBlock(g, theme, cx, titleY)
    }

    private fun drawMiniGraph(g: Graphics2D, theme: CanvasTheme, cx: Int, cy: Int) {
        val nw = 52; val nh = 26; val arrowGap = 20
        val totalW = nw * 3 + arrowGap * 2
        val nodeXs = List(3) { i -> cx - totalW / 2 + i * (nw + arrowGap) }
        val ny = cy - 62
        val midY = ny + nh / 2

        g.color = theme.edge;  g.stroke = BasicStroke(1.5f)
        for (i in 0..1) {
            val ax = nodeXs[i] + nw;  val bx = nodeXs[i + 1]
            g.drawLine(ax, midY, bx, midY)
            val s = 7
            val angle = Math.atan2(0.0, (bx - ax).toDouble())
            g.fillPolygon(
                intArrayOf(bx, bx - (s * Math.cos(angle - 0.4)).toInt(), bx - (s * Math.cos(angle + 0.4)).toInt()),
                intArrayOf(midY, midY - (s * Math.sin(angle - 0.4)).toInt(), midY - (s * Math.sin(angle + 0.4)).toInt()),
                3
            )
        }

        listOf(theme.initial, theme.regular, theme.terminal).forEachIndexed { i, col ->
            val x = nodeXs[i]
            g.color = theme.nodeBg;  g.fillRoundRect(x, ny, nw, nh, 10, 10)
            g.color = col;  g.stroke = BasicStroke(2f);  g.drawRoundRect(x, ny, nw, nh, 10, 10)
        }
    }

    private fun drawCodeBlock(g: Graphics2D, theme: CanvasTheme, cx: Int, titleY: Int) {
        val dark = theme.isDark
        val monoFont = Font("JetBrains Mono", Font.PLAIN, 12).takeUnless { it.family == "Dialog" }
            ?: Font("Monospaced", Font.PLAIN, 12)
        val kwColor  = if (dark) Color(0xCC7832) else Color(0x0033B3)
        val annColor = if (dark) Color(0xBBB529) else Color(0x808000)
        val clsColor = if (dark) Color(0xA9B7C6) else Color(0x000000)
        val blockBg  = if (dark) Color(0x2B2B2B) else Color(0xF8F8F8)
        val blockBdr = if (dark) Color(0x3C3F41) else Color(0xC9CCD6)

        g.font = monoFont
        val fm = g.fontMetrics
        val line2 = "sealed interface MyJourney : JourneyStep { ... }"
        val pad = 12; val lineH = fm.height
        val blockW = fm.stringWidth(line2) + pad * 2
        val blockH = lineH * 2 + pad * 2
        val bx = cx - blockW / 2
        val by = titleY + 38

        g.color = blockBg;  g.fillRoundRect(bx, by, blockW, blockH, 12, 12)
        g.color = blockBdr; g.stroke = BasicStroke(1f); g.drawRoundRect(bx, by, blockW, blockH, 12, 12)

        val textX = bx + pad
        val row1Y = by + pad + fm.ascent
        val row2Y = row1Y + lineH

        g.color = annColor;  g.drawString("@Journey", textX, row1Y)

        val clsBold = monoFont.deriveFont(Font.BOLD)
        fun drawToken(text: String, color: Color, x: Int, bold: Boolean = false): Int {
            g.font = if (bold) clsBold else monoFont
            g.color = color
            g.drawString(text, x, row2Y)
            return x + g.fontMetrics.stringWidth(text)
        }
        var x = textX
        x = drawToken("sealed ",     kwColor,  x)
        x = drawToken("interface ",  kwColor,  x)
        x = drawToken("MyJourney",   clsColor, x, bold = !dark)
        x = drawToken(" : ",         clsColor, x)
        x = drawToken("JourneyStep", clsColor, x, bold = !dark)
        drawToken(" { ... }",        clsColor, x)
    }
}
