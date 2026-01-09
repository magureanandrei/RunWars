package tech.titans.runwars.repo

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import tech.titans.runwars.models.Coordinates
import tech.titans.runwars.models.FriendTerritory
import tech.titans.runwars.models.RunSession
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

    // Manual parsing to properly deserialize nested runSessionList with coordinates
    fun getUserWithRunSessions(userId: String, onResult: (User?, List<RunSession>, String?) -> Unit) {
        println("🔍 UserRepo: Fetching user $userId with manual parsing")

        users.child(userId).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                println("❌ UserRepo: User not found")
                onResult(null, emptyList(), "User not found")
                return@addOnSuccessListener
            }

            // Parse basic user fields
            val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
            val lastName = snapshot.child("lastName").getValue(String::class.java) ?: ""
            val userName = snapshot.child("userName").getValue(String::class.java) ?: ""
            val email = snapshot.child("email").getValue(String::class.java) ?: ""

            println("👤 UserRepo: Found user $userName")

            // DEBUG: Print ALL children of user to see structure
            println("🔸 User snapshot children:")
            for (child in snapshot.children) {
                println("  🔸 Key: ${child.key}, Value type: ${child.value?.javaClass?.simpleName}, Value: ${child.value.toString().take(200)}")
            }

            // Manually parse runSessionList
            val runSessionsSnapshot = snapshot.child("runSessionList")
            println("📋 UserRepo: runSessionList exists: ${runSessionsSnapshot.exists()}")
            println("📋 UserRepo: runSessionList has ${runSessionsSnapshot.childrenCount} children")
            println("📋 UserRepo: runSessionList raw value: ${runSessionsSnapshot.value.toString().take(500)}")

            val runSessions = mutableListOf<RunSession>()

            for (sessionSnapshot in runSessionsSnapshot.children) {
                try {
                    println("  🔹 Session key: ${sessionSnapshot.key}")
                    println("  🔹 Session children: ${sessionSnapshot.children.map { it.key }}")

                    val runId = sessionSnapshot.child("runId").getValue(String::class.java) ?: ""
                    val distance = sessionSnapshot.child("distance").getValue(Double::class.java) ?: 0.0
                    val startTime = sessionSnapshot.child("startTime").getValue(Long::class.java) ?: 0L
                    val stopTime = sessionSnapshot.child("stopTime").getValue(Long::class.java) ?: 0L
                    val duration = sessionSnapshot.child("duration").getValue(Long::class.java) ?: 0L
                    val capturedArea = sessionSnapshot.child("capturedArea").getValue(Double::class.java) ?: 0.0

                    // Parse coordinates list
                    val coordinatesSnapshot = sessionSnapshot.child("coordinatesList")
                    println("  🔹 coordinatesList exists: ${coordinatesSnapshot.exists()}, count: ${coordinatesSnapshot.childrenCount}")
                    println("  🔹 coordinatesList raw: ${coordinatesSnapshot.value.toString().take(300)}")

                    val coordinatesList = mutableListOf<Coordinates>()

                    for (coordSnapshot in coordinatesSnapshot.children) {
                        val lat = coordSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                        val lng = coordSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                        if (lat != 0.0 || lng != 0.0) {
                            coordinatesList.add(Coordinates(lat, lng))
                        }
                    }

                    println("  📍 Run $runId: ${coordinatesList.size} coordinates, distance: $distance, area: $capturedArea")

                    if (runId.isNotEmpty()) {
                        runSessions.add(RunSession(
                            runId = runId,
                            startTime = startTime,
                            stopTime = stopTime,
                            distance = distance,
                            duration = duration,
                            capturedArea = capturedArea,
                            coordinatesList = coordinatesList
                        ))
                    }
                } catch (e: Exception) {
                    println("  ❌ Error parsing run session: ${e.message}")
                }
            }

            val user = User(
                userId = userId,
                firstName = firstName,
                lastName = lastName,
                userName = userName,
                email = email
            )

            // Parse friendsList
            val friendsListSnapshot = snapshot.child("friendsList")
            if (friendsListSnapshot.exists()) {
                for (friendSnapshot in friendsListSnapshot.children) {
                    try {
                        val friendId = friendSnapshot.child("userId").getValue(String::class.java) ?: ""
                        val friendFirstName = friendSnapshot.child("firstName").getValue(String::class.java) ?: ""
                        val friendLastName = friendSnapshot.child("lastName").getValue(String::class.java) ?: ""
                        val friendUserName = friendSnapshot.child("userName").getValue(String::class.java) ?: ""
                        val friendEmail = friendSnapshot.child("email").getValue(String::class.java) ?: ""

                        if (friendId.isNotEmpty()) {
                            user.friendsList.add(User(
                                userId = friendId,
                                firstName = friendFirstName,
                                lastName = friendLastName,
                                userName = friendUserName,
                                email = friendEmail
                            ))
                        }
                    } catch (e: Exception) {
                        println("  ❌ Error parsing friend: ${e.message}")
                    }
                }
                println("👥 UserRepo: Loaded ${user.friendsList.size} friends")
            }

            println("✅ UserRepo: Loaded ${runSessions.size} run sessions")
            onResult(user, runSessions, null)

        }.addOnFailureListener { exception ->
            println("❌ UserRepo: Failed to fetch user: ${exception.message}")
            onResult(null, emptyList(), exception.message)
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
                    friendUser.friendsList.add(currentUser)
                    addUser(friendUser)
                    onResult(true, null)
                } else {
                    onResult(false, "User is already a friend")
                }
            } else {
                onResult(false, error ?: "Current user not found")
            }
        }
    }

    fun removeFriend(currentUserId: String, friendIdToRemove: String, onResult: (Boolean, String?) -> Unit) {
        getUser(currentUserId) { currentUser, error ->
            if (currentUser != null) {
                // Remove the friend with the matching ID
                val wasRemoved = currentUser.friendsList.removeAll { it.userId == friendIdToRemove }
                if (wasRemoved) {
                    // Save the updated user back to Firebase
                    addUser(currentUser)

                    getUser(friendIdToRemove) { friend, _ ->
                        if (friend != null) {
                            val mutualRemoval = friend.friendsList.removeAll { it.userId == currentUserId }
                            if (mutualRemoval) {
                                // 5. IMPORTANT: Save the updated Friend back to the database
                                addUser(friend)
                            }
                        }
                    }

                    onResult(true, null)
                } else {
                    onResult(false, "Friend not found in list")
                }
            } else {
                onResult(false, error ?: "Current user not found")
            }
        }
    }

    /**
     * Fetch territories for all friends of a user.
     * Returns a list of FriendTerritory objects, each containing the friend's info and their territories.
     *
     * @param userId The current user's ID
     * @param friendIdsToShow Set of friend IDs whose territories should be shown (for toggle feature)
     * @param onResult Callback with list of FriendTerritory objects
     */
    fun getFriendsTerritories(
        userId: String,
        friendIdsToShow: Set<String>,
        onResult: (List<FriendTerritory>, String?) -> Unit
    ) {
        println("🔍 UserRepo: Fetching friends' territories for user $userId")
        println("🔍 UserRepo: Friends to show: $friendIdsToShow")

        if (friendIdsToShow.isEmpty()) {
            println("ℹ️ UserRepo: No friends selected to show territories")
            onResult(emptyList(), null)
            return
        }

        // First get the user to access their friends list
        getUser(userId) { user, error ->
            if (user == null) {
                println("❌ UserRepo: Could not fetch user: $error")
                onResult(emptyList(), error)
                return@getUser
            }

            val friendsList = user.friendsList.filter { it.userId in friendIdsToShow }
            println("👥 UserRepo: Found ${friendsList.size} friends to fetch territories for")

            if (friendsList.isEmpty()) {
                onResult(emptyList(), null)
                return@getUser
            }

            val friendTerritories = mutableListOf<FriendTerritory>()
            var completedCount = 0
            val totalFriends = friendsList.size

            friendsList.forEachIndexed { colorIndex, friend ->
                println("  🔸 Fetching territories for friend: ${friend.userName} (${friend.userId})")

                getUserWithRunSessions(friend.userId) { _, runSessions, friendError ->
                    if (friendError != null) {
                        println("  ❌ Error fetching friend ${friend.userName}: $friendError")
                    } else {
                        // Convert run sessions to territory points
                        val territories = runSessions.mapNotNull { runSession ->
                            if (runSession.coordinatesList.size >= 3) {
                                runSession.coordinatesList.map { coord ->
                                    LatLng(coord.latitude, coord.longitude)
                                }
                            } else {
                                null
                            }
                        }

                        if (territories.isNotEmpty()) {
                            synchronized(friendTerritories) {
                                friendTerritories.add(
                                    FriendTerritory(
                                        friendId = friend.userId,
                                        friendName = friend.userName,
                                        colorIndex = colorIndex,
                                        territories = territories
                                    )
                                )
                            }
                            println("  ✅ Friend ${friend.userName}: ${territories.size} territories loaded")
                        } else {
                            println("  ℹ️ Friend ${friend.userName}: No valid territories")
                        }
                    }

                    completedCount++
                    if (completedCount == totalFriends) {
                        println("✅ UserRepo: Finished loading all friends' territories. Total: ${friendTerritories.size} friends with territories")
                        onResult(friendTerritories.toList(), null)
                    }
                }
            }
        }
    }

}