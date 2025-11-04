package tech.titans.runwars.repo

import com.google.firebase.database.DatabaseReference
import tech.titans.runwars.models.User

object UserRepo {
    private val users: DatabaseReference = FirebaseProvider.database.getReference("users")

    fun addUser(user: User){
        val userMap = mapOf(
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "userName" to user.userName,
            "email" to user.email
        )
        users.child(user.userId).setValue(userMap)
    }
}