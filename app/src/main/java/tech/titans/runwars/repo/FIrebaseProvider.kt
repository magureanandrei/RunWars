package tech.titans.runwars.repo

import com.google.firebase.database.FirebaseDatabase
import tech.titans.runwars.config.AppConfig

object FirebaseProvider {
    val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance(AppConfig.FIREBASE_DB_URL)
    }
}