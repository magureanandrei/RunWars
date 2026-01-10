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
import tech.titans.runwars.utils.LocationUtils.mergeIfOverlapping

object UserService {

    fun signUpUser(firstName: String, lastName: String, userName: String, email: String, password: String, onResult: (Boolean, String?) -> Unit){
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener{ task ->
            if(task.isSuccessful){
                val firebaseUser = auth.currentUser
                val uid = firebaseUser?.uid

                if(uid != null){
                    val user = User(
                        userId = uid,
                        firstName = firstName,
                        lastName = lastName,
                        userName = userName,
                        email = email
                    )

                    UserRepo.addUser(user)
                    onResult(true, null)
                }
                else{
                    onResult(false, "User ID is null")
                    Log.e("UserService: signUpUser: ", "Uid is null")
                    throw NullPointerException("Uid is null")
                }
            }
            else{
                onResult(false, task.exception?.message)
                Log.e("UserService: signUpUser: ", "Task was not successful")
            }
        }
    }
    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit){
        val auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task ->
            if(task.isSuccessful){
                onResult(true,null)
                Log.i("UserService: loginUser: ", "User logged in")
            }
            else{
                onResult(false, task.exception?.message)
            }
        }
    }

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
        // 1. Convert to Model Coordinates
        val newRunCoordinates = mutableListOf<Coordinates>()
        pathPoints.forEach {
            newRunCoordinates.add(Coordinates(it.latitude, it.longitude))
        }

        // Ensure closure (Needed for valid math, but creates the "straight line" if saved separately)
        if (newRunCoordinates.isNotEmpty() && newRunCoordinates.first() != newRunCoordinates.last()) {
            newRunCoordinates.add(newRunCoordinates.first())
        }

        val runSession = RunSession(
            runId = runId,
            startTime = startTime,
            stopTime = stopTime,
            distance = distance,
            duration = duration,
            capturedArea = capturedArea,
            coordinatesList = newRunCoordinates
        )

        // ALWAYS save to history (this is safe, history is just a list)
        RunSessionRepo.addRunSession(runSession)

        UserRepo.getUser(userId) { user, _ ->
            if (user != null) {
                // TRACKER FLAG: Did we merge this run into an existing one?
                var wasMerged = false

                // --- MERGE LOGIC START ---
                if (newRunCoordinates.size >= 3 && capturedArea > 0) {
                    for (existingSession in user.runSessionList) {
                        if (existingSession.coordinatesList.size < 3) continue

                        val mergedPath = LocationUtils.mergeIfOverlapping(
                            existingSession.coordinatesList,
                            newRunCoordinates
                        )

                        if (mergedPath != null) {
                            // Update the EXISTING session
                            existingSession.coordinatesList.clear()
                            mergedPath.forEach { coord -> existingSession.coordinatesList.add(coord) }

                            // Recalculate area
                            try {
                                val mergedLatLngs = mergedPath.map { LatLng(it.latitude, it.longitude) }
                                val newArea = LocationUtils.calculateCapturedArea(mergedLatLngs)
                                existingSession.capturedArea = newArea
                                Log.i("UserService", "Recalculated area: ${newArea}")
                            } catch (e: Exception) {
                                Log.e("UserService", "Failed to recalculate: ${e.message}")
                            }

                            Log.i("UserService", "Merged into ${existingSession.runId}")

                            // *** SET FLAG TO TRUE ***
                            wasMerged = true
                            break
                        }
                    }
                }
                // --- MERGE LOGIC END ---

                // *** THE FIX ***
                // Only add the separate run entry if it wasn't merged!
                if (!wasMerged) {
                    user.runSessionList.add(runSession)
                }

                UserRepo.addUser(user)
                Log.i("AddRunSessionToUser", "Saved. Was merged? $wasMerged")
            }
        }
    }

    /**
     * Update username with uniqueness check
     */
    fun updateUsername(
        userId: String,
        newUsername: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (newUsername.isBlank()) {
            onResult(false, "Username cannot be empty")
            return
        }

        if (newUsername.length < 3 || newUsername.length > 20) {
            onResult(false, "Username must be 3-20 characters")
            return
        }

        if (!newUsername.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            onResult(false, "Username can only contain letters, numbers and underscores")
            return
        }

        val db = FirebaseProvider.database.getReference("users")

        // Check if username already exists
        db.orderByChild("userName").equalTo(newUsername)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var usernameExists = false
                    for (child in snapshot.children) {
                        if (child.key != userId) {
                            usernameExists = true
                            break
                        }
                    }

                    if (usernameExists) {
                        onResult(false, "Username already taken")
                    } else {
                        // Update username
                        db.child(userId).child("userName").setValue(newUsername)
                            .addOnSuccessListener {
                                Log.i("UserService", "Username updated successfully")
                                onResult(true, null)
                            }
                            .addOnFailureListener { e ->
                                Log.e("UserService", "Failed to update username: ${e.message}")
                                onResult(false, e.message)
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserService", "Failed to check username availability: ${error.message}")
                    onResult(false, error.message)
                }
            })
    }

    /**
     * Update first name
     */
    fun updateFirstName(
        userId: String,
        newFirstName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (newFirstName.isBlank()) {
            onResult(false, "First name cannot be empty")
            return
        }

        if (newFirstName.length < 2 || newFirstName.length > 30) {
            onResult(false, "First name must be 2-30 characters")
            return
        }

        val db = FirebaseProvider.database.getReference("users")
        db.child(userId).child("firstName").setValue(newFirstName)
            .addOnSuccessListener {
                Log.i("UserService", "First name updated successfully")
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("UserService", "Failed to update first name: ${e.message}")
                onResult(false, e.message)
            }
    }

    /**
     * Update last name
     */
    fun updateLastName(
        userId: String,
        newLastName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (newLastName.isBlank()) {
            onResult(false, "Last name cannot be empty")
            return
        }

        if (newLastName.length < 2 || newLastName.length > 30) {
            onResult(false, "Last name must be 2-30 characters")
            return
        }

        val db = FirebaseProvider.database.getReference("users")
        db.child(userId).child("lastName").setValue(newLastName)
            .addOnSuccessListener {
                Log.i("UserService", "Last name updated successfully")
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("UserService", "Failed to update last name: ${e.message}")
                onResult(false, e.message)
            }
    }

    /**
     * Update password with current password confirmation
     */
    fun updatePassword(
        currentPassword: String,
        newPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.email == null) {
            onResult(false, "No authenticated user")
            return
        }

        // Validate password strength
        val passwordValidation = validatePasswordStrength(newPassword)
        if (!passwordValidation.first) {
            onResult(false, passwordValidation.second)
            return
        }

        // Re-authenticate with current password
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Update password
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Log.i("UserService", "Password updated successfully")
                        onResult(true, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserService", "Failed to update password: ${e.message}")
                        onResult(false, "Failed to update password: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("UserService", "Re-authentication failed: ${e.message}")
                onResult(false, "Current password is incorrect")
            }
    }

    /**
     * Validate password strength
     */
    private fun validatePasswordStrength(password: String): Pair<Boolean, String?> {
        when {
            password.length < 8 ->
                return Pair(false, "Password must be at least 8 characters")
            !password.any { it.isUpperCase() } ->
                return Pair(false, "Password must contain at least one uppercase letter")
            !password.any { it.isLowerCase() } ->
                return Pair(false, "Password must contain at least one lowercase letter")
            !password.any { it.isDigit() } ->
                return Pair(false, "Password must contain at least one number")
            !password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) } ->
                return Pair(false, "Password must contain at least one special character")
        }
        return Pair(true, null)
    }
}