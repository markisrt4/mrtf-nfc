package com.mrtechforge.mrtfnfc.actions

fun parseNfcPayload(payload: String): ActionEnvelope {
    return when {
        payload.startsWith("WIFI", ignoreCase = true) -> {
            ActionEnvelope(
                action = ActionType.WIFI,
                payload = mapOf(
                    "profile" to payload.substringAfter(":", "default")
                )
            )
        }
        else -> {
            ActionEnvelope(action = ActionType.UNKNOWN)
        }
    }
}