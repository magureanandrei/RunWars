package tech.titans.runwars.services


import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import tech.titans.runwars.models.Coordinates
import tech.titans.runwars.models.RunSession
import tech.titans.runwars.models.User
import tech.titans.runwars.repo.FirebaseProvider
import tech.titans.runwars.repo.RunSessionRepo
import tech.titans.runwars.repo.UserRepo

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

    fun addRunSessionToUser(distance: Double, pathPoints: List<LatLng>, userId: String){
        val runId = FirebaseProvider.database.getReference().push().key!!
        val coordinatesList = mutableListOf<Coordinates>()
        var index = 1

        for(coord in pathPoints){
            if(index == 1 || index%2 == 0){
                coordinatesList.add(Coordinates(coord.latitude,coord.longitude))
            }
            index++
        }
        Log.i("AddRunSessionToUser", "Index: $index")
        if(coordinatesList[coordinatesList.size -1] != Coordinates(pathPoints[index-2].latitude, pathPoints[index-2].longitude)){
            coordinatesList.add(Coordinates(pathPoints[index-2].latitude, pathPoints[index-2].longitude))
        }

        val runSession = RunSession(runId = runId, distance = distance, coordinatesList = coordinatesList)
        RunSessionRepo.addRunSession(runSession)

        UserRepo.getUser(userId) { user, error ->
            if(user != null){
                user.runSessionList.add(runSession)
                UserRepo.addUser(user)
            }
        }
    }
}