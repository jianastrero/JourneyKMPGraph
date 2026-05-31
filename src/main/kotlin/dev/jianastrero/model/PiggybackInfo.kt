package dev.jianastrero.model

/** Represents a piggyback action attached to a step. trigger = "ON_ENTER" or "ON_EXIT". */
data class PiggybackInfo(val id: String, val trigger: String)
