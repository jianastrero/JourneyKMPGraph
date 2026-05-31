package dev.jianastrero.canvas.painter

import dev.jianastrero.canvas.CanvasTheme
import dev.jianastrero.canvas.NODE_H
import dev.jianastrero.canvas.NODE_W
import java.awt.*
import java.awt.geom.GeneralPath

object EdgePainter {
    fun paint(g: Graphics2D, theme: CanvasTheme, from: Point, to: Point, fromBounds: Rectangle, toBounds: Rectangle, label: String) {
        val fcx = fromBounds.x + fromBounds.width  / 2
        val fcy = fromBounds.y + fromBounds.height / 2
        val tcx = toBounds.x   + toBounds.width    / 2
        val tcy = toBounds.y   + toBounds.height   / 2
        val dx = tcx - fcx;  val dy = tcy - fcy

        val x1: Int; val y1: Int; val x2: Int; val y2: Int
        val cx1: Int; val cy1: Int; val cx2: Int; val cy2: Int

        if (Math.abs(dx) >= Math.abs(dy)) {
            val fromMidY = from.y + NODE_H / 2
            val toMidY   = to.y   + NODE_H / 2
            if (dx >= 0) { x1 = from.x + NODE_W; y1 = fromMidY; x2 = to.x;          y2 = toMidY }
            else         { x1 = from.x;           y1 = fromMidY; x2 = to.x + NODE_W; y2 = toMidY }
            val midx = (x1 + x2) / 2
            cx1 = midx; cy1 = y1; cx2 = midx; cy2 = y2
        } else {
            if (dy >= 0) { x1 = fcx; y1 = fromBounds.y + fromBounds.height; x2 = tcx; y2 = toBounds.y                }
            else         { x1 = fcx; y1 = fromBounds.y;                     x2 = tcx; y2 = toBounds.y + toBounds.height }
            val midy = (y1 + y2) / 2
            cx1 = x1; cy1 = midy; cx2 = x2; cy2 = midy
        }

        g.color  = theme.edge
        g.stroke = BasicStroke(1.5f)
        val path = GeneralPath()
        path.moveTo(x1.toFloat(), y1.toFloat())
        path.curveTo(cx1.toFloat(), cy1.toFloat(), cx2.toFloat(), cy2.toFloat(), x2.toFloat(), y2.toFloat())
        g.draw(path)

        drawArrowhead(g, x2, y2, Math.atan2((y2 - cy2).toDouble(), (x2 - cx2).toDouble()))

        // Label at bezier midpoint (t = 0.5)
        val lx = ((x1 + 3*cx1 + 3*cx2 + x2) / 8.0).toInt()
        val ly = ((y1 + 3*cy1 + 3*cy2 + y2) / 8.0).toInt() - 4
        g.font = Font("SansSerif", Font.ITALIC, 10)
        val fm = g.fontMetrics
        val lw = fm.stringWidth(label)
        g.color = theme.background
        g.fillRect(lx - lw / 2 - 2, ly - fm.ascent, lw + 4, fm.height)
        g.color = theme.edgeLabel
        g.drawString(label, lx - lw / 2, ly)
    }

    private fun drawArrowhead(g: Graphics2D, x: Int, y: Int, angle: Double) {
        val s = 8
        g.fillPolygon(
            intArrayOf(x, x - (s * Math.cos(angle - 0.4)).toInt(), x - (s * Math.cos(angle + 0.4)).toInt()),
            intArrayOf(y, y - (s * Math.sin(angle - 0.4)).toInt(), y - (s * Math.sin(angle + 0.4)).toInt()),
            3
        )
    }
}
