package dev.jianastrero.toolwindow

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import dev.jianastrero.canvas.JourneyCanvasPanel
import dev.jianastrero.export.MermaidExporter
import dev.jianastrero.model.JourneyGraph
import dev.jianastrero.model.LayoutDirection
import dev.jianastrero.scanner.JourneyScanner
import dev.jianastrero.scanner.PsiJourneyScanner
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class JourneyGraphWindow(
    private val project: Project,
    toolWindow: ToolWindow,
    private val scanner: JourneyScanner = PsiJourneyScanner(),
    private val icons: IconManager = IconManager()
) {
    private val disposable = toolWindow.disposable

    val canvas   = JourneyCanvasPanel()
    val dropdown = ComboBox<String>()
    private var journeys = listOf<JourneyGraph>()

    @Suppress("UseJBColor") // Fallback when UIManager returns null; used as canvas bgOverride, not an IDE UI color
    private val ideBg      = UIManager.getColor("Panel.background") ?: Color(0xF5F5F5)
    private val ideIsDark  = (ideBg.red * 0.299 + ideBg.green * 0.587 + ideBg.blue * 0.114) < 128
    private var currentIdeDark = ideIsDark

    // ── Buttons ─────────────────────────────────────────────────────────────────
    private val refreshBtn       = JButton("Refresh")
    private val horizBtn         = squareIconButton("Arrange horizontally")
    private val vertBtn          = squareIconButton("Arrange vertically")
    private val exportMermaidBtn = JButton("Mermaid")
    private val exportPngBtn     = JButton("PNG")
    private val zoomOutBtn       = squareIconButton("Zoom out")
    private val zoomResetBtn     = squareIconButton("Reset zoom to 100%")
    private val zoomInBtn        = squareIconButton("Zoom in")
    private val fitBtn           = squareIconButton("Fit to view")
    private val themeToggle      = JToggleButton(null as String?, ideIsDark).apply {
        toolTipText = "Toggle light/dark theme"
        isFocusPainted = false
        margin = JBUI.insets(4)
    }

    private var themeIconLight: Icon? = null
    private var themeIconDark:  Icon? = null

    val mainPanel: JPanel

    init {
        canvas.setTheme(ideIsDark, ideBg)
        applyIcons(ideIsDark)
        wireActions()

        val scrollPane = JBScrollPane(canvas)
        wireExportActions(scrollPane)

        mainPanel = JPanel(BorderLayout()).apply {
            add(buildTopBar(),    BorderLayout.NORTH)
            add(scrollPane,       BorderLayout.CENTER)
            add(buildBottomBar(), BorderLayout.SOUTH)
        }

        subscribeToEvents()
        refresh()
    }

    // ── Actions ──────────────────────────────────────────────────────────────────

    private fun wireActions() {
        dropdown.addActionListener {
            journeys.find { it.name == dropdown.selectedItem }?.let { canvas.display(it) }
        }
        refreshBtn.addActionListener   { refresh() }
        horizBtn.addActionListener     { canvas.rearrange(LayoutDirection.HORIZONTAL) }
        vertBtn.addActionListener      { canvas.rearrange(LayoutDirection.VERTICAL) }
        themeToggle.addActionListener  { canvas.setTheme(themeToggle.isSelected); applyIcons(currentIdeDark) }
        zoomOutBtn.addActionListener   { canvas.zoomOut() }
        zoomResetBtn.addActionListener { canvas.resetZoom() }
        zoomInBtn.addActionListener    { canvas.zoomIn() }
    }

    private fun wireExportActions(scrollPane: JScrollPane) {
        fitBtn.addActionListener {
            canvas.fitToView(scrollPane.viewport.size)
            scrollPane.viewport.viewPosition = Point(0, 0)
        }

        exportMermaidBtn.addActionListener {
            val graph = journeys.find { it.name == dropdown.selectedItem } ?: return@addActionListener
            val mermaid = MermaidExporter.export(graph)
            JDialog().apply {
                title = "Mermaid — ${graph.name}"
                setSize(520, 320)
                setLocationRelativeTo(mainPanel)
                layout = BorderLayout()
                val ta = JTextArea(mermaid).apply { font = Font("Monospaced", Font.PLAIN, 12); isEditable = false }
                val copyBtn = JButton("Copy to Clipboard").apply {
                    addActionListener {
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(mermaid), null)
                    }
                }
                add(JScrollPane(ta), BorderLayout.CENTER)
                add(copyBtn, BorderLayout.SOUTH)
                isVisible = true
            }
        }

        exportPngBtn.addActionListener {
            val img = canvas.renderToImage()
            val graph = journeys.find { it.name == dropdown.selectedItem }
            JFileChooser().apply {
                dialogTitle = "Save PNG"
                fileFilter = FileNameExtensionFilter("PNG Image", "png")
                selectedFile = File((graph?.name ?: "journey") + ".png")
                if (showSaveDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                    var file = selectedFile
                    if (!file.name.endsWith(".png")) file = File(file.absolutePath + ".png")
                    ImageIO.write(img, "png", file)
                }
            }
        }
    }

    // ── Refresh ──────────────────────────────────────────────────────────────────

    fun refresh() {
        val found = ReadAction.compute<List<JourneyGraph>, Throwable> { scanner.scan(project) }.sortedBy { it.name }
        SwingUtilities.invokeLater {
            val prev = dropdown.selectedItem as? String
            journeys = found
            dropdown.removeAllItems()
            found.forEach { dropdown.addItem(it.name) }
            when {
                prev != null && found.any { it.name == prev } -> dropdown.selectedItem = prev
                found.isNotEmpty()                             -> dropdown.selectedIndex = 0
                else                                           -> canvas.clear()
            }
        }
    }

    // ── Icons ─────────────────────────────────────────────────────────────────────

    @Suppress("UseJBColor") // Tint color is a fixed light-grey for dark IDE themes, not an IDE UI component color
    private fun applyIcons(isDark: Boolean) {
        val tint = if (isDark) Color(0xE8E8E8) else null
        fun ic(raw: Icon?) = if (tint != null && raw != null) icons.tint(raw, tint) else raw
        ic(icons.export)?.let   { exportMermaidBtn.icon = it; exportPngBtn.icon = it }
        ic(icons.refresh)?.let  { refreshBtn.icon = it }
        ic(icons.horiz)?.let    { horizBtn.icon = it }
        ic(icons.vert)?.let     { vertBtn.icon = it }
        ic(icons.zoomOut)?.let  { zoomOutBtn.icon = it }
        ic(icons.realSize)?.let { zoomResetBtn.icon = it }
        ic(icons.zoomIn)?.let   { zoomInBtn.icon = it }
        ic(icons.fit)?.let      { fitBtn.icon = it }
        themeIconLight = ic(icons.light)
        themeIconDark  = ic(icons.dark)
        themeToggle.icon = if (themeToggle.isSelected) themeIconDark else themeIconLight
    }

    // ── Event subscriptions ───────────────────────────────────────────────────────

    private fun subscribeToEvents() {
        project.messageBus
            .connect(disposable)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun selectionChanged(e: FileEditorManagerEvent) = refresh()
            })

        ApplicationManager.getApplication().messageBus
            .connect(disposable)
            .subscribe(LafManagerListener.TOPIC, LafManagerListener { _ ->
                @Suppress("UseJBColor") // Fallback when UIManager returns null; passed as canvas bgOverride
                val newBg   = UIManager.getColor("Panel.background") ?: Color(0xF5F5F5)
                val newDark = (newBg.red * 0.299 + newBg.green * 0.587 + newBg.blue * 0.114) < 128
                currentIdeDark = newDark
                SwingUtilities.invokeLater {
                    canvas.setTheme(newDark, newBg)
                    themeToggle.isSelected = newDark
                    applyIcons(newDark)
                }
            })

        project.messageBus
            .connect(disposable)
            .subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    if (events.any { it is VFileContentChangeEvent && it.file.extension == "kt" }) refresh()
                }
            })
    }

    // ── UI builders ───────────────────────────────────────────────────────────────

    private fun buildTopBar() = JPanel(BorderLayout()).apply {
        add(JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply {
            add(JLabel("Journey:")); add(dropdown); add(refreshBtn)
        }, BorderLayout.WEST)
        add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 4)).apply {
            add(JLabel("Distribute")); add(horizBtn); add(vertBtn)
        }, BorderLayout.EAST)
    }

    private fun buildBottomBar() = JPanel(BorderLayout()).apply {
        add(JPanel(FlowLayout(FlowLayout.LEFT,   8, 4)).apply { add(themeToggle) },                               BorderLayout.WEST)
        add(JPanel(FlowLayout(FlowLayout.CENTER, 4, 4)).apply { add(zoomOutBtn); add(zoomResetBtn); add(zoomInBtn); add(fitBtn) }, BorderLayout.CENTER)
        add(JPanel(FlowLayout(FlowLayout.RIGHT,  8, 4)).apply { add(exportMermaidBtn); add(exportPngBtn) },        BorderLayout.EAST)
    }

    private fun squareIconButton(tip: String) = object : JButton() {
        init { toolTipText = tip; isFocusPainted = false; margin = JBUI.insets(2) }
        override fun getPreferredSize(): Dimension { val h = super.getPreferredSize().height; return Dimension(h, h) }
    }
}
