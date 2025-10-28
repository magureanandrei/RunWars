package tech.titans.runwars.models

data class User(
    val userId: Int,
    val firstName: String,
    val lastName: String,
    val userName: String,
    val email: String,
    val password: String
)
