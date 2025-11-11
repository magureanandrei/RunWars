package tech.titans.runwars.views

import androidx.lifecycle.ViewModel
import tech.titans.runwars.services.UserService

class LoginViewModel: ViewModel() {
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit){
        UserService.loginUser(email, password, onResult)
    }
}