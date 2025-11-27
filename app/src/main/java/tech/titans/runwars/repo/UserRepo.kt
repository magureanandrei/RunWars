package tech.titans.runwars.repo

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import tech.titans.runwars.models.User

object UserRepo {
    private val users: DatabaseReference = FirebaseProvider.database.getReference("users")

    fun addUser(user: User){
        print("ADd user: $user")
        val userMap = mapOf(
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "userName" to user.userName,
            "email" to user.email,
            "territoryList" to user.territoryList,
            "runSessionList" to user.runSessionList
        )
        users.child(user.userId).setValue(userMap)
    }

    fun getUser(userId: String, onResult: (User?, String?) -> Unit) {
        users.child(userId).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(User::class.java)
            user?.userId = userId
            if(user != null){
                onResult(user, null)
            }
            else {
                onResult(null, "User not found")
            }
        }
            .addOnFailureListener { exception ->
                onResult(null, exception.message)
            }
    }
}