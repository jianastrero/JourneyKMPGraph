package dev.jianastrero.scanner

import com.intellij.openapi.project.Project
import dev.jianastrero.model.JourneyGraph

interface JourneyScanner {
    fun scan(project: Project): List<JourneyGraph>
}
