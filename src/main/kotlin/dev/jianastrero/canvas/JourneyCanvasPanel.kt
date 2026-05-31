package dev.jianastrero.canvas

import dev.jianastrero.canvas.painter.EdgePainter
import dev.jianastrero.canvas.painter.EmptyStatePainter
import dev.jianastrero.canvas.painter.NodePainter
import dev.jianastrero.canvas.painter.PiggybackPainter
import dev.jianastrero.layout.LayoutEngine
import dev.jianastrero.layout.BfsLayoutEngine
import dev.jianastrero.model.JourneyGraph
import dev.jianastrero.model.LayoutDirection
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import javax.swing.JPanel

class JourneyCanvasPanel(
    private val layoutEngine: LayoutEngine = BfsLayoutEngine()
) : JPanel() {

    private var graph: JourneyGraph? = null
    var nodePositions: MutableMap<String, Point> = mutableMapOf()
    var zoomLevel = 1.0

    private var theme = CanvasTheme.light()

    init {
        background = theme.background
        isOpaque = true
        setupDrag()
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    fun setTheme(dark: Boolean, bgOverride: Color? = null) {
        theme = if (dark) CanvasTheme.dark(bgOverride) else CanvasTheme.light(bgOverride)
        background = theme.background
        repaint()
    }

    fun display(g: JourneyGraph, direction: LayoutDirection = LayoutDirection.HORIZONTAL) {
        graph = g
        nodePositions = layoutEngine.compute(g, direction)
        updatePreferredSize()
        revalidate()
        repaint()
    }

    fun rearrange(direction: LayoutDirection) {
        val g = graph ?: return
        nodePositions = layoutEngine.compute(g, direction)
        updatePreferredSize()
        revalidate()
        repaint()
    }

    fun clear() {
        graph = null
        nodePositions = mutableMapOf()
        zoomLevel = 1.0
        preferredSize = Dimension(400, 200)
        revalidate()
        repaint()
    }

    fun nodeAt(x: Int, y: Int): String? =
        nodePositions.entries.firstOrNull { (_, pt) ->
            x >= pt.x && x <= pt.x + NODE_W && y >= pt.y && y <= pt.y + NODE_H
        }?.key

    fun zoomIn()    { zoomLevel = (zoomLevel * 1.2).coerceAtMost(8.0); updatePreferredSize(); revalidate(); repaint() }
    fun zoomOut()   { zoomLevel = (zoomLevel / 1.2).coerceAtLeast(0.1); updatePreferredSize(); revalidate(); repaint() }
    fun resetZoom() { zoomLevel = 1.0; updatePreferredSize(); revalidate(); repaint() }

    fun fitToView(viewportSize: Dimension) {
        if (nodePositions.isEmpty()) return
        val cw = (nodePositions.values.maxOfOrNull { it.x } ?: 0) + NODE_W + 60
        val ch = (nodePositions.values.maxOfOrNull { it.y } ?: 0) + NODE_H + 60
        zoomLevel = minOf(viewportSize.width.toDouble() / cw, viewportSize.height.toDouble() / ch).coerceIn(0.1, 8.0)
        updatePreferredSize(); revalidate(); repaint()
    }

    fun renderToImage(): BufferedImage {
        val cw = (nodePositions.values.maxOfOrNull { it.x } ?: 0) + NODE_W + 60
        val ch = (nodePositions.values.maxOfOrNull { it.y } ?: 0) + NODE_H + 60
        val img = UIUtil.createImage(null, cw.coerceAtLeast(1), ch.coerceAtLeast(1), BufferedImage.TYPE_INT_ARGB)
        val ig = img.createGraphics()
        ig.color = theme.background; ig.fillRect(0, 0, img.width, img.height)
        val saved = zoomLevel; zoomLevel = 1.0
        paintComponent(ig)
        zoomLevel = saved; ig.dispose()
        return img
    }

    // ── Painting ───────────────────────────────────────────────────────────────

    override fun paintComponent(g2: Graphics) {
        super.paintComponent(g2)
        val g = g2 as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        if (graph == null) {
            EmptyStatePainter.paint(g, theme, width, height)
            return
        }

        g.scale(zoomLevel, zoomLevel)
        val cur = graph!!

        val tagFm = g.getFontMetrics(Font("SansSerif", Font.PLAIN, 10))
        val effBounds = cur.steps.associate { s ->
            val pt = nodePositions[s.name] ?: Point(0, 0)
            s.name to PiggybackPainter.computeEffectiveBounds(s, pt, tagFm)
        }

        // 1. Edges (behind everything)
        cur.edges.forEach { e ->
            val f  = nodePositions[e.from] ?: return@forEach
            val t  = nodePositions[e.to]   ?: return@forEach
            val fb = effBounds[e.from]     ?: Rectangle(f.x, f.y, NODE_W, NODE_H)
            val tb = effBounds[e.to]       ?: Rectangle(t.x, t.y, NODE_W, NODE_H)
            EdgePainter.paint(g, theme, f, t, fb, tb, e.label)
        }

        // 2. Piggyback tags (behind node boxes)
        cur.steps.forEach { s ->
            PiggybackPainter.paint(g, theme, s, nodePositions[s.name] ?: return@forEach)
        }

        // 3. Node boxes on top
        cur.steps.forEach { s ->
            NodePainter.paint(g, theme, s, nodePositions[s.name] ?: return@forEach)
        }
    }

    // ── Drag ───────────────────────────────────────────────────────────────────

    private fun setupDrag() {
        var dragNode = ""
        var dragOffX = 0
        var dragOffY = 0

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val sx = (e.x / zoomLevel).toInt()
                val sy = (e.y / zoomLevel).toInt()
                dragNode = nodeAt(sx, sy) ?: ""
                if (dragNode.isNotEmpty()) {
                    val pt = nodePositions[dragNode]!!
                    dragOffX = sx - pt.x
                    dragOffY = sy - pt.y
                    cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                }
            }
            override fun mouseReleased(e: MouseEvent) {
                dragNode = ""
                cursor = Cursor.getDefaultCursor()
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (dragNode.isEmpty()) return
                val sx = (e.x / zoomLevel).toInt()
                val sy = (e.y / zoomLevel).toInt()
                nodePositions[dragNode] = Point(sx - dragOffX, sy - dragOffY)
                repaint()
            }
        })
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun updatePreferredSize() {
        val cw = (nodePositions.values.maxOfOrNull { it.x } ?: 0) + NODE_W + 60
        val ch = (nodePositions.values.maxOfOrNull { it.y } ?: 0) + NODE_H + 60
        preferredSize = Dimension((cw * zoomLevel).toInt(), (ch * zoomLevel).toInt())
    }
}
