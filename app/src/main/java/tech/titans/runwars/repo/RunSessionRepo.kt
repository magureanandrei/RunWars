package tech.titans.runwars.repo

import com.google.firebase.database.DatabaseReference
import tech.titans.runwars.models.Coordinates
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

    fun getRunSession(runId: String, onResult: (RunSession?, String?) -> Unit) {
        runSessions.child(runId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val runSession = snapshot.getValue(RunSession::class.java)
                onResult(runSession, null)
            } else {
                onResult(null, "Run session not found")
            }
        }.addOnFailureListener { exception ->
            onResult(null, exception.message)
        }
    }

    fun getRunSessionsByIds(runIds: List<String>, onResult: (List<RunSession>) -> Unit) {
        if (runIds.isEmpty()) {
            onResult(emptyList())
            return
        }

        val results = mutableListOf<RunSession>()
        var completedCount = 0

        runIds.forEach { runId ->
            runSessions.child(runId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    try {
                        // Manual parsing for more control
                        val runIdValue = snapshot.child("runId").getValue(String::class.java) ?: ""
                        val distance = snapshot.child("distance").getValue(Double::class.java) ?: 0.0
                        val coordinatesSnapshot = snapshot.child("coordinatesList")

                        val coordinatesList = mutableListOf<Coordinates>()
                        for (coordSnapshot in coordinatesSnapshot.children) {
                            val lat = coordSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                            val lng = coordSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                            coordinatesList.add(Coordinates(lat, lng))
                        }

                        val runSession = RunSession(
                            runId = runIdValue,
                            distance = distance,
                            coordinatesList = coordinatesList
                        )
                        results.add(runSession)
                        println("✅ Loaded run session $runIdValue with ${coordinatesList.size} coordinates")
                    } catch (e: Exception) {
                        println("❌ Error parsing run session: ${e.message}")
                    }
                }
                completedCount++
                if (completedCount == runIds.size) {
                    onResult(results)
                }
            }.addOnFailureListener {
                completedCount++
                if (completedCount == runIds.size) {
                    onResult(results)
                }
            }
        }
    }
}