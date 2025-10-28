package tech.titans.runwars.models

data class Territory(
    val territoryId: Int,
    val latitude: String,
    val longitude: String,
    val owner: User
)
