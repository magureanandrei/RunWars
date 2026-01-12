package tech.titans.runwars.utils

import android.util.Log
import tech.titans.runwars.models.Coordinates // Make sure this matches your actual class name
import tech.titans.runwars.models.RunSession
import tech.titans.runwars.models.User
import tech.titans.runwars.repo.UserRepo
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

object TerritoryGenerator {

    fun createBot(
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Double, // We'll use this as the "approximate width" of the block
        botName: String,
        botId: String = "bot_${UUID.randomUUID().toString().take(8)}"
    ) {
        // 1. Generate a "City Block" shape instead of a circle
        // We make a rectangle roughly 2x longer than it is wide (classic running route)
        val pointsList = generateBlockPath(centerLat, centerLng, radiusMeters)

        // 2. Calculate rough area (Width * Height)
        val width = radiusMeters * 2
        val height = radiusMeters
        val areaMeters = width * height

        // 3. Create Session (same as before)
        val fakeRunId = "run_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        val fakeRun = RunSession(
            runId = fakeRunId,
            startTime = now - 1800000,
            stopTime = now,
            distance = (2 * width) + (2 * height), // Perimeter
            duration = 1800,
            capturedArea = areaMeters,
            coordinatesList = pointsList
        )

        // 4. Create User (same as before)
        val nameParts = botName.split(" ")
        val fName = nameParts.getOrElse(0) { "Bot" }
        val lName = nameParts.getOrElse(1) { "User" }

        val botUser = User(
            userId = botId,
            firstName = fName,
            lastName = lName,
            userName = botName,
            email = "${botId}@bot.com",
            runSessionList = mutableListOf(fakeRun)
        )

        UserRepo.addUser(botUser)
        Log.i("TerritoryGen", "✅ Created Real-Style Bot: $botName")
    }

    // --- NEW: Generates a noisy rectangular path ---
    private fun generateBlockPath(
        centerLat: Double,
        centerLng: Double,
        sizeMeters: Double
    ): MutableList<Coordinates> {
        val points = mutableListOf<Coordinates>()
        val earthRadius = 6371000.0

        // Define a rectangle relative to the center
        // Width = size * 2, Height = size
        val latOffset = (sizeMeters / earthRadius) * (180.0 / Math.PI)
        val lngOffset =
            ((sizeMeters * 2) / earthRadius) * (180.0 / Math.PI) / cos(centerLat * Math.PI / 180.0)

        // The 4 corners of the "block"
        val corners = listOf(
            Pair(centerLat + latOffset, centerLng - lngOffset), // Top-Left
            Pair(centerLat + latOffset, centerLng + lngOffset), // Top-Right
            Pair(centerLat - latOffset, centerLng + lngOffset), // Bottom-Right
            Pair(centerLat - latOffset, centerLng - lngOffset)  // Bottom-Left
        )

        // Walk between corners adding "GPS Jitter"
        for (i in corners.indices) {
            val start = corners[i]
            val end = corners[(i + 1) % corners.size]

            // Add 5 points per side to make it look organic
            val steps = 5
            for (j in 0 until steps) {
                val fraction = j.toDouble() / steps

                // Linear interpolation (move from start to end)
                var interpLat = start.first + (end.first - start.first) * fraction
                var interpLng = start.second + (end.second - start.second) * fraction

                // Add "Jitter" (Random noise of ~5-10 meters)
                // This makes it look like real GPS drift, not a perfect ruler line
                val noise = 0.00005 // roughly 5 meters
                interpLat += (Math.random() - 0.5) * noise
                interpLng += (Math.random() - 0.5) * noise

                points.add(Coordinates(interpLat, interpLng))
            }
        }

        // Close the loop
        points.add(points[0])

        return points
    }
}