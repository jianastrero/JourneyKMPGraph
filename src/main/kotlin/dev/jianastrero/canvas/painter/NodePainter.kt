package dev.jianastrero.canvas.painter

import dev.jianastrero.canvas.CanvasTheme
import dev.jianastrero.canvas.NODE_H
import dev.jianastrero.canvas.NODE_W
import dev.jianastrero.model.StepNode
import java.awt.*

object NodePainter {
    @Suppress("UseJBColor") // Drop shadow alpha color is canvas-specific, not IDE-theme-driven
    fun paint(g: Graphics2D, theme: CanvasTheme, step: StepNode, pt: Point) {
        val border = borderColor(theme, step)

        // Drop shadow
        g.color = Color(0, 0, 0, 18)
        g.fillRoundRect(pt.x + 2, pt.y + 2, NODE_W, NODE_H, 12, 12)

        // Body
        g.color = theme.nodeBg
        g.fillRoundRect(pt.x, pt.y, NODE_W, NODE_H, 12, 12)
        g.color = border
        g.stroke = BasicStroke(2f)
        g.drawRoundRect(pt.x, pt.y, NODE_W, NODE_H, 12, 12)

        // Step name (centered)
        g.color = theme.nodeLabel
        g.font  = Font("SansSerif", Font.BOLD, 12)
        val fm  = g.fontMetrics
        g.drawString(
            step.name,
            pt.x + (NODE_W - fm.stringWidth(step.name)) / 2,
            pt.y + (NODE_H + fm.ascent - fm.descent) / 2
        )

        // "start" / "end" label on the border line
        val tag = when {
            step.isInitial  -> "start"
            step.isTerminal -> "end"
            else            -> null
        }
        if (tag != null) {
            g.font = Font("SansSerif", Font.ITALIC, 10)
            val lfm     = g.fontMetrics
            val lw      = lfm.stringWidth(tag)
            val lx      = pt.x + (NODE_W - lw) / 2
            val borderY = if (step.isInitial) pt.y else pt.y + NODE_H
            val bgY     = borderY - lfm.height / 2
            g.color = theme.nodeBg
            g.fillRect(lx - 2, bgY, lw + 4, lfm.height)
            g.color = border
            g.drawString(tag, lx, bgY + lfm.ascent)
        }
    }

    fun borderColor(theme: CanvasTheme, step: StepNode) = when {
        step.isInitial  -> theme.initial
        step.isTerminal -> theme.terminal
        else            -> theme.regular
    }
}
