package tech.titans.runwars.services


import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import tech.titans.runwars.models.User
import tech.titans.runwars.repo.UserRepo

object UserService {

    fun addUser(firstName: String, lastName: String, userName: String, email: String, password: String){
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
                }
                else{
                    Log.e("UserService: addUser: ", "Uid is null")
                    throw NullPointerException("Uid is null")
                }
            }
            else{
                Log.e("UserService: addUser: ", "Task was not successful")
            }
        }
    }
}