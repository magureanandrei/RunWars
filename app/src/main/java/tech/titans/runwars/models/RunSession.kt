package tech.titans.runwars.models

import java.util.Locale

data class RunSession(
    val runId: String = "",
    val startTime: Long = 0L,
    val stopTime: Long = 0L,
    val distance: Double = 0.0,
    val duration: Long = 0L,
    val capturedArea: Double = 0.0, // Area in m², 0 means no territory captured
    val coordinatesList: MutableList<Coordinates> = mutableListOf()
) {
    /**
     * Check if this run captured a territory (formed a closed loop)
     */
    fun hasTerritory(): Boolean = capturedArea > 0

    /**
     * Get captured area in hectares
     */
    fun getCapturedAreaHectares(): Double = capturedArea / 10_000

    /**
     * Get distance in kilometers
     */
    fun getDistanceKm(): Double = distance / 1000

    /**
     * Get formatted duration string (MM:SS or HH:MM:SS)
     */
    fun getFormattedDuration(): String {
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Get pace in minutes per kilometer (returns formatted string like "5:30")
     */
    fun getPacePerKm(): String {
        if (distance <= 0 || duration <= 0) return "--:--"

        val totalSeconds = duration / 1000
        val distanceKm = distance / 1000
        val paceSeconds = totalSeconds / distanceKm

        val paceMinutes = (paceSeconds / 60).toInt()
        val paceSecondsRemainder = (paceSeconds % 60).toInt()

        return String.format(Locale.US, "%d:%02d", paceMinutes, paceSecondsRemainder)
    }
}
