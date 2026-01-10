package tech.titans.runwars.services

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import tech.titans.runwars.models.Coordinates
import tech.titans.runwars.models.RunSession
import tech.titans.runwars.models.User
import tech.titans.runwars.repo.FirebaseProvider
import tech.titans.runwars.repo.RunSessionRepo
import tech.titans.runwars.repo.UserRepo
import tech.titans.runwars.utils.LocationUtils

object UserService {

    // (Keep your existing signUpUser and loginUser functions here - omitting them to save space)
    fun signUpUser(firstName: String, lastName: String, userName: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        // ... (Paste your existing signUpUser code) ...
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener{ task ->
                if(task.isSuccessful){
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid
                    if(uid != null){
                        val user = User(userId = uid, firstName = firstName, lastName = lastName, userName = userName, email = email)
                        UserRepo.addUser(user)
                        onResult(true, null)
                    } else { onResult(false, "User ID is null") }
                } else { onResult(false, task.exception?.message) }
            }
    }

    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit){
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task ->
            if(task.isSuccessful){ onResult(true,null) } else{ onResult(false, task.exception?.message) }
        }
    }

    // --- THE FIX STARTS HERE ---
    fun addRunSessionToUser(
        distance: Double,
        pathPoints: List<LatLng>,
        userId: String,
        startTime: Long = System.currentTimeMillis(),
        duration: Long = 0L,
        capturedArea: Double = 0.0
    ) {
        val runId = FirebaseProvider.database.getReference().push().key!!
        val stopTime = System.currentTimeMillis()

        // 1. Prepare New Run Coordinates
        val newRunCoordinates = mutableListOf<Coordinates>()
        pathPoints.forEach { newRunCoordinates.add(Coordinates(it.latitude, it.longitude)) }

        // Ensure closure
        if (newRunCoordinates.isNotEmpty() && newRunCoordinates.first() != newRunCoordinates.last()) {
            newRunCoordinates.add(newRunCoordinates.first())
        }

        val runSession = RunSession(
            runId = runId,
            startTime = startTime, stopTime = stopTime,
            distance = distance, duration = duration, capturedArea = capturedArea,
            coordinatesList = newRunCoordinates
        )

        // Always save to History
        RunSessionRepo.addRunSession(runSession)

        // CRITICAL: Use getUserWithRunSessions
        UserRepo.getUserWithRunSessions(userId) { user, runSessions, _ ->
            if (user != null) {
                user.runSessionList.clear()
                user.runSessionList.addAll(runSessions)

                var wasMerged = false
                var attackerShape = newRunCoordinates.toList() // Copy

                // --- 1. SELF-MERGE LOGIC ---
                if (newRunCoordinates.size >= 3 && capturedArea > 0) {
                    for (existingSession in user.runSessionList) {
                        if (existingSession.coordinatesList.size < 3) continue

                        val mergedPath = LocationUtils.mergeIfOverlapping(existingSession.coordinatesList, newRunCoordinates)

                        if (mergedPath != null) {
                            existingSession.coordinatesList.clear()
                            existingSession.coordinatesList.addAll(mergedPath)

                            try {
                                val mergedLatLngs = mergedPath.map { LatLng(it.latitude, it.longitude) }
                                val newArea = LocationUtils.calculateCapturedArea(mergedLatLngs)
                                if (!newArea.isNaN() && !newArea.isInfinite()) {
                                    existingSession.capturedArea = newArea
                                }
                            } catch (e: Exception) { Log.e("UserService", "Area calc error: ${e.message}") }

                            attackerShape = mergedPath
                            wasMerged = true
                            break
                        }
                    }
                }

                // --- 2. STEALING LOGIC ---
                if (attackerShape.size >= 3 && user.friendsList.isNotEmpty()) {
                    user.friendsList.forEach { friend ->
                        UserRepo.getUserWithRunSessions(friend.userId) { friendFull, friendRuns, _ ->
                            if (friendFull != null) {
                                friendFull.runSessionList.clear()
                                friendFull.runSessionList.addAll(friendRuns)
                                var friendDamaged = false

                                friendRuns.forEach { victimRun ->
                                    if (victimRun.coordinatesList.size >= 3) {
                                        val remainingLand = LocationUtils.subtractPath(
                                            victimPath = victimRun.coordinatesList,
                                            attackerPath = attackerShape
                                        )

                                        if (remainingLand != null && remainingLand.size != victimRun.coordinatesList.size) {
                                            if (remainingLand.isEmpty()) {
                                                // FATALITY
                                                victimRun.coordinatesList.clear()
                                                victimRun.capturedArea = 0.0
                                                Log.i("UserService", "💀 FATALITY: ${friend.userName}")
                                            } else {
                                                // PARTIAL STEAL
                                                victimRun.coordinatesList.clear()
                                                victimRun.coordinatesList.addAll(remainingLand)
                                                try {
                                                    val newArea = LocationUtils.calculateCapturedArea(
                                                        remainingLand.map { LatLng(it.latitude, it.longitude) }
                                                    )
                                                    if (!newArea.isNaN()) victimRun.capturedArea = newArea
                                                } catch (e: Exception) {}
                                                Log.i("UserService", "⚔️ HIT: ${friend.userName}")
                                            }
                                            friendDamaged = true
                                        }
                                    }
                                }

                                if (friendDamaged) {
                                    // Remove dead runs to clean DB
                                    friendFull.runSessionList.removeAll { it.capturedArea == 0.0 && it.coordinatesList.isEmpty() }
                                    UserRepo.addUser(friendFull)
                                }
                            }
                        }
                    }
                }

                // --- 3. SAVE SELF ---
                if (!wasMerged) {
                    user.runSessionList.add(runSession)
                }

                // Remove dead runs
                user.runSessionList.removeAll { it.capturedArea == 0.0 && it.coordinatesList.isEmpty() }

                UserRepo.addUser(user)
                Log.i("UserService", "✅ Run Saved. Merged=$wasMerged")
            }
        }
    }

    // (Keep the rest of your update functions: updateUsername, updateFirstName, etc.)
    fun updateUsername(userId: String, newUsername: String, onResult: (Boolean, String?) -> Unit) {
        // ... (Paste existing code) ...
        if (newUsername.isBlank()) { onResult(false, "Username cannot be empty"); return }
        val db = FirebaseProvider.database.getReference("users")
        db.orderByChild("userName").equalTo(newUsername).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var exists = false
                for (child in snapshot.children) { if (child.key != userId) { exists = true; break } }
                if (exists) onResult(false, "Taken")
                else db.child(userId).child("userName").setValue(newUsername).addOnSuccessListener { onResult(true, null) }
            }
            override fun onCancelled(error: DatabaseError) { onResult(false, error.message) }
        })
    }

    fun updateFirstName(userId: String, name: String, onResult: (Boolean, String?) -> Unit) {
        val db = FirebaseProvider.database.getReference("users")
        db.child(userId).child("firstName").setValue(name).addOnSuccessListener{ onResult(true,null) }
    }

    fun updateLastName(userId: String, name: String, onResult: (Boolean, String?) -> Unit) {
        val db = FirebaseProvider.database.getReference("users")
        db.child(userId).child("lastName").setValue(name).addOnSuccessListener{ onResult(true,null) }
    }

    fun updatePassword(current: String, new: String, onResult: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val cred = EmailAuthProvider.getCredential(user.email!!, current)
        user.reauthenticate(cred).addOnSuccessListener {
            user.updatePassword(new).addOnSuccessListener { onResult(true,null) }
        }.addOnFailureListener { onResult(false, "Auth failed") }
    }
}