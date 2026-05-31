package dev.jianastrero.layout

import dev.jianastrero.model.JourneyGraph
import dev.jianastrero.model.LayoutDirection
import java.awt.Point

interface LayoutEngine {
    fun compute(graph: JourneyGraph, direction: LayoutDirection): MutableMap<String, Point>
}
