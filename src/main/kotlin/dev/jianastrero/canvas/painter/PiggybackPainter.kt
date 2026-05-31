package dev.jianastrero.canvas.painter

import dev.jianastrero.canvas.CanvasTheme
import dev.jianastrero.canvas.NODE_H
import dev.jianastrero.canvas.NODE_W
import dev.jianastrero.model.StepNode
import java.awt.*

object PiggybackPainter {
    private val TAG_W       = NODE_W - 16
    private const val TAG_PAD_H   = 5
    private const val TAG_PAD_V   = 3
    private const val TAG_GAP     = 3
    private const val TAG_OVERLAP = 2

    fun paint(g: Graphics2D, theme: CanvasTheme, step: StepNode, pt: Point) {
        g.font = Font("SansSerif", Font.PLAIN, 10)
        val fm = g.fontMetrics

        var enterBottom = pt.y + TAG_OVERLAP
        step.piggybacks.filter { it.trigger == "ON_ENTER" }.forEach { pb ->
            val lines = wrapText(pb.id, fm)
            val th = tagHeight(lines.size, fm)
            drawTag(g, theme, pt.x + 8, enterBottom - th, lines, fm, theme.piggyEnter)
            enterBottom -= th + TAG_GAP
        }

        var exitTop = pt.y + NODE_H - TAG_OVERLAP
        step.piggybacks.filter { it.trigger == "ON_EXIT" }.forEach { pb ->
            val lines = wrapText(pb.id, fm)
            val th = tagHeight(lines.size, fm)
            drawTag(g, theme, pt.x + 8, exitTop, lines, fm, theme.piggyExit)
            exitTop += th + TAG_GAP
        }
    }

    /** Full bounding box of a node including piggyback tags above/below. */
    fun computeEffectiveBounds(step: StepNode, pt: Point, fm: FontMetrics): Rectangle {
        var topY = pt.y
        var enterBottom = pt.y + TAG_OVERLAP
        step.piggybacks.filter { it.trigger == "ON_ENTER" }.forEach { pb ->
            val th = tagHeight(wrapText(pb.id, fm).size, fm)
            enterBottom -= th
            topY = minOf(topY, enterBottom)
            enterBottom -= TAG_GAP
        }

        var bottomY = pt.y + NODE_H
        var exitTop = pt.y + NODE_H - TAG_OVERLAP
        step.piggybacks.filter { it.trigger == "ON_EXIT" }.forEach { pb ->
            val th = tagHeight(wrapText(pb.id, fm).size, fm)
            bottomY = maxOf(bottomY, exitTop + th)
            exitTop += th + TAG_GAP
        }

        return Rectangle(pt.x, topY, NODE_W, bottomY - topY)
    }

    private fun drawTag(g: Graphics2D, theme: CanvasTheme, tx: Int, ty: Int, lines: List<String>, fm: FontMetrics, border: Color) {
        val th = tagHeight(lines.size, fm)
        g.color = theme.nodeBg
        g.fillRoundRect(tx, ty, TAG_W, th, 8, 8)
        g.color = border
        g.stroke = BasicStroke(1.5f)
        g.drawRoundRect(tx, ty, TAG_W, th, 8, 8)
        g.color = theme.nodeLabel
        lines.forEachIndexed { i, line ->
            g.drawString(line, tx + TAG_PAD_H, ty + TAG_PAD_V + fm.ascent + fm.height * i)
        }
    }

    fun wrapText(text: String, fm: FontMetrics): List<String> {
        val maxW = TAG_W - TAG_PAD_H * 2
        val lines = mutableListOf<String>()
        var rem = text
        while (rem.isNotEmpty()) {
            var i = rem.length
            while (i > 0 && fm.stringWidth(rem.substring(0, i)) > maxW) i--
            if (i == 0) i = 1
            lines.add(rem.substring(0, i))
            rem = rem.substring(i)
        }
        return lines
    }

    fun tagHeight(lineCount: Int, fm: FontMetrics) = fm.height * lineCount + TAG_PAD_V * 2
}
