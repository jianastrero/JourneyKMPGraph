package dev.jianastrero.layout

import dev.jianastrero.canvas.COL_GAP
import dev.jianastrero.canvas.EST_TAG_H
import dev.jianastrero.canvas.NODE_H
import dev.jianastrero.canvas.NODE_W
import dev.jianastrero.canvas.ROW_GAP
import dev.jianastrero.model.JourneyGraph
import dev.jianastrero.model.LayoutDirection
import dev.jianastrero.model.StepNode
import java.awt.Point

class BfsLayoutEngine : LayoutEngine {
    override fun compute(graph: JourneyGraph, direction: LayoutDirection): MutableMap<String, Point> {
        val depth = bfsDepth(graph)
        val maxDepth = (depth.values.maxOrNull() ?: -1) + 1
        val stepsByDepth = graph.steps.groupBy { depth[it.name] ?: 0 }

        return if (direction == LayoutDirection.VERTICAL)
            computeVertical(graph.steps, depth, maxDepth, stepsByDepth)
        else
            computeHorizontal(graph.steps, depth)
    }

    private fun bfsDepth(graph: JourneyGraph): MutableMap<String, Int> {
        val depth = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()
        graph.steps.firstOrNull { it.isInitial }?.name?.let { queue.add(it); depth[it] = 0 }

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val d = depth[cur] ?: 0
            graph.edges.filter { it.from == cur && !depth.containsKey(it.to) }.forEach {
                depth[it.to] = d + 1
                queue.add(it.to)
            }
        }

        val maxDepth = (depth.values.maxOrNull() ?: -1) + 1
        graph.steps.filter { !depth.containsKey(it.name) }.forEach { depth[it.name] = maxDepth }
        return depth
    }

    private fun assignSlots(
        steps: List<StepNode>,
        depth: Map<String, Int>,
        pointFor: (d: Int, slot: Int) -> Point
    ): MutableMap<String, Point> {
        val slotCounter = mutableMapOf<Int, Int>()
        return steps.associate { step ->
            val d    = depth[step.name] ?: 0
            val slot = slotCounter.getOrDefault(d, 0).also { slotCounter[d] = it + 1 }
            step.name to pointFor(d, slot)
        }.toMutableMap()
    }

    private fun computeHorizontal(steps: List<StepNode>, depth: Map<String, Int>) =
        assignSlots(steps, depth) { d, slot ->
            Point(d * (NODE_W + COL_GAP) + 40, slot * (NODE_H + ROW_GAP) + 40)
        }

    private fun computeVertical(
        steps: List<StepNode>,
        depth: Map<String, Int>,
        maxDepth: Int,
        stepsByDepth: Map<Int, List<StepNode>>
    ): MutableMap<String, Point> {
        val depthY = mutableMapOf<Int, Int>()
        var currentY = 40
        (0..maxDepth).forEach { d ->
            depthY[d] = currentY
            val atThis = stepsByDepth[d] ?: emptyList()
            val atNext = stepsByDepth[d + 1] ?: emptyList()
            val maxExitH  = atThis.maxOfOrNull { piggyTagsHeight(it, "ON_EXIT")  } ?: 0
            val maxEnterH = atNext.maxOfOrNull { piggyTagsHeight(it, "ON_ENTER") } ?: 0
            currentY += NODE_H + ROW_GAP + maxExitH + maxEnterH
        }
        return assignSlots(steps, depth) { d, slot ->
            Point(slot * (NODE_W + COL_GAP) + 40, depthY[d] ?: 40)
        }
    }

    private fun piggyTagsHeight(step: StepNode, trigger: String): Int {
        val count = step.piggybacks.count { it.trigger == trigger }
        return if (count == 0) 0 else count * EST_TAG_H
    }
}
