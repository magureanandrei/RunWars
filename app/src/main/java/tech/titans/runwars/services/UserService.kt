package tech.titans.runwars.services

import tech.titans.runwars.models.User
import tech.titans.runwars.repo.UserRepo

object UserService {

    fun addUser(user: User){
        UserRepo.addUser(user)
    }
}