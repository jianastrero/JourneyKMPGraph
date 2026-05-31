package dev.jianastrero.export

import dev.jianastrero.model.JourneyGraph

object MermaidExporter {
    fun export(graph: JourneyGraph): String {
        val sb = StringBuilder("flowchart LR\n")
        graph.steps.forEach { s ->
            val lbl = buildString {
                append(s.name)
                if (s.isInitial)  append("\\n(start)")
                if (s.isTerminal) append("\\n(end)")
            }
            sb.appendLine("    ${s.name}[\"$lbl\"]")
        }
        graph.edges.forEach { e ->
            sb.appendLine("    ${e.from} -->|${e.label}| ${e.to}")
        }
        return sb.toString().trimEnd()
    }
}
