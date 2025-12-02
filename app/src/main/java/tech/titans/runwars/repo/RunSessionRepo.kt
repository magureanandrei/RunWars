package tech.titans.runwars.repo

import com.google.firebase.database.DatabaseReference
import tech.titans.runwars.models.RunSession

object RunSessionRepo {
    private val runSessions: DatabaseReference = FirebaseProvider.database.getReference("runSessions")

    fun addRunSession(runSession: RunSession){
        val runSessionMap = mapOf(
            "runId" to runSession.runId,
            "startTime" to runSession.startTime,
            "stopTime" to runSession.stopTime,
            "distance" to runSession.distance,
            "duration" to runSession.duration,
            "coordinatesList" to runSession.coordinatesList,
        )

        runSessions.child(runSession.runId).setValue(runSessionMap)
    }
}