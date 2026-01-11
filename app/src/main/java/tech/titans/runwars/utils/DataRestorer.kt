package tech.titans.runwars.utils

import android.util.Log
import tech.titans.runwars.models.RunSession
import tech.titans.runwars.repo.RunSessionRepo
import tech.titans.runwars.repo.UserRepo

/**
 * UTILITY TO RESTORE LOST DATA
 * Usage: Call DataRestorer.executeRestoration() from MainActivity.onCreate() once.
 */
object DataRestorer {

    fun executeRestoration() {
        // ==========================================
        // 👇 ENTER THE DATA HERE 👇
        // ==========================================

        // REPLACE WITH THE TARGET USER ID
        val targetUserId = "Userid"

        // REPLACE WITH THE RUN IDs YOU WANT TO GIVE BACK TO THEM
        val runIdsToRestore = listOf(
            "id1",
            "id2"
        )

        // ==========================================
        // 🚀 THE LOGIC (DO NOT TOUCH BELOW)
        // ==========================================
        restoreRuns(targetUserId, runIdsToRestore)
    }

    private fun restoreRuns(userId: String, runIds: List<String>) {
        Log.i("DataRestorer", "🔄 Starting restoration for User: $userId")

        // 1. Fetch the User securely (Deep Load)
        UserRepo.getUserWithRunSessions(userId) { user, currentRuns, userError ->
            if (user == null || userError != null) {
                Log.e("DataRestorer", "❌ User not found! Error: $userError")
                return@getUserWithRunSessions
            }

            Log.i("DataRestorer", "👤 User loaded. Currently has ${currentRuns.size} runs.")

            // 2. Fetch the Runs from the Master List
            RunSessionRepo.getRunSessionsByIds(runIds) { foundRuns ->
                if (foundRuns.isEmpty()) {
                    Log.e("DataRestorer", "❌ No runs found with those IDs.")
                    return@getRunSessionsByIds
                }

                Log.i("DataRestorer", "📂 Found ${foundRuns.size} runs in database.")

                // 3. Merge them (Prevent duplicates)
                val userRuns = user.runSessionList // This is now the list from the Deep Load
                var addedCount = 0

                foundRuns.forEach { newRun ->
                    // Check if user already has this runId
                    val alreadyExists = userRuns.any { it.runId == newRun.runId }

                    if (!alreadyExists) {
                        userRuns.add(newRun)
                        addedCount++
                        Log.i("DataRestorer", "   ➕ Attaching Run: ${newRun.runId} (${String.format("%.2f", newRun.distance/1000)}km)")
                    } else {
                        Log.w("DataRestorer", "   ⚠️ Run ${newRun.runId} already exists on user. Skipping.")
                    }
                }

                // 4. Save back to Firebase
                if (addedCount > 0) {
                    UserRepo.addUser(user)
                    Log.i("DataRestorer", "✅ SUCCESS! Restored $addedCount runs to ${user.userName}.")
                } else {
                    Log.i("DataRestorer", "ℹ️ No new runs were needed. User is up to date.")
                }
            }
        }
    }
}