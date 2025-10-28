package tech.titans.runwars.models

import java.sql.Time

data class RunSession(
    val runId: Int,
    val startLocation: Time,
    val stopLocation: Time,
    val distance: Int,
    val duration: Time,
    val path: String
)
