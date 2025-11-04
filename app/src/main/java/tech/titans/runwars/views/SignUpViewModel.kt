package tech.titans.runwars.views

import androidx.lifecycle.ViewModel
import tech.titans.runwars.repo.UserRepo
import tech.titans.runwars.services.UserService

class SignUpViewModel: ViewModel() {
    fun signUp(firstName: String, lastName: String, userName: String, email: String, password: String) {
        UserService.addUser(firstName, lastName, userName, email, password)
    }
}