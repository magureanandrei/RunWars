package tech.titans.runwars.services


import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import tech.titans.runwars.models.RunSession
import tech.titans.runwars.models.User
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

    fun addRunSessionToUser(runSession: RunSession, userId: String){
        UserRepo.getUser(userId) { user, error ->
            if(user != null){
                user.runSessionList.add(runSession)
                UserRepo.addUser(user)
            }
        }
    }
}