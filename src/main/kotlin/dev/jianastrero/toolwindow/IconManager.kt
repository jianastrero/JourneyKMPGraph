package dev.jianastrero.toolwindow

import com.intellij.openapi.util.IconLoader
import com.intellij.util.IconUtil
import com.intellij.util.ui.UIUtil
import java.awt.Color
import javax.swing.Icon
import javax.swing.ImageIcon

class IconManager {
    private fun load(name: String): Icon? =
        IconLoader.findIcon("/icons/$name", IconManager::class.java)

    private fun scale(icon: Icon?): Icon? = icon?.let { IconUtil.scale(it, null, 16f / 24f) }

    fun tint(icon: Icon, color: Color): Icon {
        val w = icon.iconWidth.coerceAtLeast(1); val h = icon.iconHeight.coerceAtLeast(1)
        val img = UIUtil.createImage(null, w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics(); icon.paintIcon(null, g, 0, 0); g.dispose()
        val rgb = color.rgb and 0xFFFFFF
        (0 until w).forEach { x ->
            (0 until h).forEach { y ->
                val a = (img.getRGB(x, y) ushr 24) and 0xFF
                if (a > 0) img.setRGB(x, y, (a shl 24) or rgb)
            }
        }
        return ImageIcon(img)
    }

    val export   = scale(load("icon_export.svg"))
    val refresh  = scale(load("icon_refresh.svg"))
    val horiz    = scale(load("icon_horiz.svg"))
    val vert     = scale(load("icon_vert.svg"))
    val zoomOut  = scale(load("icon_zoom_out.svg"))
    val realSize = scale(load("icon_real_size.svg"))
    val zoomIn   = scale(load("icon_zoom_in.svg"))
    val fit      = scale(load("icon_fit.svg"))
    val light    = scale(load("icon_light.svg"))
    val dark     = scale(load("icon_dark.svg"))
}
