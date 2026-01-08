package tech.titans.runwars.models

data class User(
    var userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val userName: String = "",
    val email: String = "",
    val territoryList: MutableList<Territory> = mutableListOf(),
    val runSessionList: MutableList<RunSession> = mutableListOf(),
    val friendsList: MutableList<User> = mutableListOf()
)
