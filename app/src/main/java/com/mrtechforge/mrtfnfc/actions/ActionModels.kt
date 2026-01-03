package com.mrtechforge.mrtfnfc.actions

/**
 * Versioned internal action schema.
 * Safe to evolve without changing NFC tags.
 */
data class ActionEnvelope(
    val version: Int = 1,
    val action: ActionType,
    val payload: Map<String, String> = emptyMap(),
    val confirm: Boolean = true,
    val fallback: FallbackMode = FallbackMode.SYSTEM_UI
)

enum class ActionType {
    WIFI,
    BLUETOOTH,
    NFC,
    UNKNOWN
}

enum class FallbackMode {
    SYSTEM_UI,
    NONE
}