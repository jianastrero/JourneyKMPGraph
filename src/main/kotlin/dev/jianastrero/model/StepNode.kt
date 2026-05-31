package dev.jianastrero.model

data class StepNode(
    val name: String,
    val isInitial: Boolean,
    val isTerminal: Boolean,
    val piggybacks: List<PiggybackInfo>
)
