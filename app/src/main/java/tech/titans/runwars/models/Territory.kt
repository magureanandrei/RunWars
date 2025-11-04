package tech.titans.runwars.models

data class Territory(
    val territoryId: String,
    val latitude: String,
    val longitude: String,
    val owner: User
)
