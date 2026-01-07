package tech.titans.runwars.repo

import android.util.Log
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
            "runSessionList" to user.runSessionList,
            "friendsList" to user.friendsList
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

    fun searchUsers(query: String, onResult: (List<User>) -> Unit) {
        Log.d("UserRepo", "Searching for: $query")
        if(query.isEmpty()){
            onResult(emptyList())
            return
        }
        users.orderByChild("userName")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limitToFirst(50)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("UserRepo", "Found ${snapshot.childrenCount} matches")
                val searchResults = mutableListOf<User>()
                snapshot.children.forEach { child ->
                    val user = child.getValue(User::class.java)
                    if(user != null){
                        user.userId = child.key ?: ""
                        searchResults.add(user)
                    }
                }
                onResult(searchResults)
            }
            .addOnFailureListener { exception ->
                Log.e("UserRepo", "FAILURE: ${exception.message}", exception)
                onResult(emptyList())
            }
    }
    fun addFriend(currentUserId: String, friendUser: User, onResult: (Boolean, String?) -> Unit) {
        getUser(currentUserId) { currentUser, error ->
            if (currentUser != null) {
                // Check if already friends (compare IDs)
                val isAlreadyFriend = currentUser.friendsList.any { it.userId == friendUser.userId }

                if (!isAlreadyFriend) {
                    // IMPORTANT: Create a "clean" copy of the friend to avoid recursion loops.
                    // We DO NOT want to save the friend's own lists (territories, friends) inside our user object.
                    val cleanFriend = friendUser.copy(
                        friendsList = mutableListOf(),
                        territoryList = mutableListOf(),
                        runSessionList = mutableListOf()
                    )

                    currentUser.friendsList.add(cleanFriend)
                    addUser(currentUser) // Reuse your existing save method
                    onResult(true, null)
                } else {
                    onResult(false, "User is already a friend")
                }
            } else {
                onResult(false, error ?: "Current user not found")
            }
        }
    }

}