package dev.jianastrero.model

data class JourneyGraph(
    val name: String,
    val steps: List<StepNode>,
    val edges: List<ExitEdge>
)
