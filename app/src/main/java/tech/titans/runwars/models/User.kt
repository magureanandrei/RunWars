package tech.titans.runwars.models

data class User(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val userName: String,
    val email: String,
    val territoryList: List<Territory>?=null,
    val runSessionList: List<RunSession>?=null
)
