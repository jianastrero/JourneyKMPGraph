package dev.jianastrero.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class JourneyGraphToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val window = JourneyGraphWindow(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(window.mainPanel, null, false)
        toolWindow.contentManager.addContent(content)
    }
}
