package tech.titans.runwars.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Result of location smoothing operation
 */
data class SmoothedLocationResult(
    val location: Location,
    val shouldAddToPath: Boolean // True if anchor was updated (moved > 10m)
)

/**
 * Production-ready GPS smoothing system using Kalman filtering.
 * Handles noise rejection, stationary detection, and confidence decay.
 */
class LocationSmoother {

    private val kalmanFilter = KalmanLatLong(processNoise = 0.5f)

    // Previous smoothed location for bearing calculation
    private var previousLocation: Location? = null

    // Last accepted location (for impossible speed check)
    private var lastAcceptedLocation: Location? = null
    private var lastAcceptedTime: Long = 0

    // Distance anchor (deadband filter to prevent drift)
    private var lastAnchorPoint: LatLng? = null
    private val ANCHOR_DISTANCE_THRESHOLD = 10.0 // meters - must move 10m to update anchor

    // Stationary detection
    private var stationaryStartTime: Long = 0
    private var stationaryPosition: LatLng? = null
    private val STATIONARY_THRESHOLD_METERS = 2.0
    private val STATIONARY_TIME_MS = 5000L // 5 seconds

    // Last valid update timestamp
    private var lastUpdateTime: Long = 0

    // Noise rejection thresholds - STRICT ACCURACY GATE
    private val MAX_ACCURACY_METERS = 20f // Strict: only high-confidence points
    private val MAX_VELOCITY_MS = 20.0 // meters/second (72 km/h max for walking/running)

    // Impossible speed rejection
    private val IMPOSSIBLE_SPEED_MS = 25.0 // meters/second (90 km/h - filters teleportation glitches)
    private var consecutiveRejectionsStartTime: Long = 0
    private val MAX_CONSECUTIVE_REJECTION_MS = 10000L // 10 seconds - reset if stuck

    // Signal loss tracking
    private val SIGNAL_LOSS_THRESHOLD_MS = 3000L // 3 seconds

    companion object {
        private const val EARTH_RADIUS_METERS = 6371000.0

        /**
         * Calculate distance between two lat/lng points in meters
         */
        private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)

            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLng / 2) * sin(dLng / 2)

            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return EARTH_RADIUS_METERS * c
        }
    }

    /**
     * Add a raw GPS location and return the smoothed location.
     *
     * @param rawLocation Raw GPS location from FusedLocationProviderClient
     * @return Smoothed location with recalculated bearing
     */
    fun addLocation(rawLocation: Location): Location {
        val currentTime = System.currentTimeMillis()

        // Check for signal loss and decay confidence
        if (lastUpdateTime > 0) {
            val timeSinceLastUpdate = currentTime - lastUpdateTime
            if (timeSinceLastUpdate > SIGNAL_LOSS_THRESHOLD_MS) {
                // Signal was lost, decay confidence
                val decayIterations = (timeSinceLastUpdate / 1000).toInt()
                repeat(decayIterations) {
                    kalmanFilter.decayConfidence(1.5f)
                }
                println("📡 [LocationSmoother] Signal loss detected, decayed confidence")
            }
        }

        // --- Impossible Speed Rejection (BEFORE other filters) ---

        if (lastAcceptedLocation != null && lastAcceptedTime > 0) {
            val timeDeltaSeconds = (currentTime - lastAcceptedTime) / 1000.0

            if (timeDeltaSeconds > 0 && timeDeltaSeconds < 60) { // Sanity check on time delta
                val distance = calculateDistance(
                    lastAcceptedLocation!!.latitude,
                    lastAcceptedLocation!!.longitude,
                    rawLocation.latitude,
                    rawLocation.longitude
                )
                val speed = distance / timeDeltaSeconds

                // Check if speed is impossible (teleportation glitch)
                if (speed > IMPOSSIBLE_SPEED_MS) {
                    // Start tracking consecutive rejections
                    if (consecutiveRejectionsStartTime == 0L) {
                        consecutiveRejectionsStartTime = currentTime
                    }

                    val rejectionDuration = currentTime - consecutiveRejectionsStartTime

                    // Check if we've been rejecting for too long (maybe in a vehicle?)
                    if (rejectionDuration > MAX_CONSECUTIVE_REJECTION_MS) {
                        println("⚠️ [LocationSmoother] Consecutive rejections exceeded ${MAX_CONSECUTIVE_REJECTION_MS}ms - resetting filter and accepting point")
                        // Reset the filter to prevent getting stuck
                        consecutiveRejectionsStartTime = 0
                        // Accept this point and continue processing
                    } else {
                        println("🚫 [LocationSmoother] IMPOSSIBLE SPEED REJECTED: ${speed.toInt()}m/s (${(speed * 3.6).toInt()}km/h) - distance: ${distance.toInt()}m in ${timeDeltaSeconds}s")
                        // Return the last smoothed location (don't update)
                        return createSmoothedLocation(currentTime)
                    }
                } else {
                    // Speed is reasonable, reset consecutive rejection tracker
                    consecutiveRejectionsStartTime = 0
                }
            }
        }

        // --- Signal Hygiene: Accuracy Gate (Trash Filter) ---

        // STRICT: Only accept high-confidence GPS points
        if (!rawLocation.hasAccuracy() || rawLocation.accuracy > MAX_ACCURACY_METERS) {
            val acc = if (rawLocation.hasAccuracy()) rawLocation.accuracy else Float.MAX_VALUE
            println("🚮 [LocationSmoother] ACCURACY GATE REJECTED: ${acc}m (threshold: ${MAX_ACCURACY_METERS}m)")
            return createSmoothedLocation(currentTime)
        }

        // --- Noise Rejection: Unrealistic Velocity ---
        if (previousLocation != null && lastUpdateTime > 0) {
            val dt = (currentTime - lastUpdateTime) / 1000.0 // seconds
            if (dt > 0 && dt < 60) {
                val distance = calculateDistance(
                    previousLocation!!.latitude,
                    previousLocation!!.longitude,
                    rawLocation.latitude,
                    rawLocation.longitude
                )
                val velocity = distance / dt

                if (velocity > MAX_VELOCITY_MS) {
                    println("❌ [LocationSmoother] Rejected: unrealistic velocity (${velocity}m/s)")
                    return createSmoothedLocation(currentTime)
                }
            }
        }

        // --- Kalman Filter Update ---

        kalmanFilter.predict(currentTime)
        kalmanFilter.update(
            rawLocation.latitude,
            rawLocation.longitude,
            rawLocation.accuracy.coerceAtLeast(5f),
            currentTime
        )

        lastUpdateTime = currentTime

        // Update last accepted location for impossible speed check
        lastAcceptedLocation = rawLocation
        lastAcceptedTime = currentTime

        // --- Distance Anchor Logic (Deadband Filter) ---

        val filteredLat = kalmanFilter.getLatitude()
        val filteredLng = kalmanFilter.getLongitude()
        val filteredPoint = LatLng(filteredLat, filteredLng)

        if (lastAnchorPoint == null) {
            // First point - initialize anchor
            lastAnchorPoint = filteredPoint
            println("⚓ [LocationSmoother] Anchor initialized at first point")
        } else {
            // Calculate distance from anchor
            val distanceFromAnchor = calculateDistance(
                lastAnchorPoint!!.latitude,
                lastAnchorPoint!!.longitude,
                filteredLat,
                filteredLng
            )

            if (distanceFromAnchor >= ANCHOR_DISTANCE_THRESHOLD) {
                // User has moved beyond deadband - update anchor
                lastAnchorPoint = filteredPoint
                println("⚓ [LocationSmoother] Anchor updated - moved ${distanceFromAnchor.toInt()}m (threshold: ${ANCHOR_DISTANCE_THRESHOLD}m)")
            } else {
                // Within deadband - ignore small movements to prevent drift
                println("🔒 [LocationSmoother] Within anchor deadband - ${distanceFromAnchor.toInt()}m (ignoring to prevent drift)")
            }
        }

        // --- Stationary Detection ---

        if (previousLocation != null) {
            val distanceFromPrevious = calculateDistance(
                previousLocation!!.latitude,
                previousLocation!!.longitude,
                filteredLat,
                filteredLng
            )

            // Check if user is stationary
            if (distanceFromPrevious < STATIONARY_THRESHOLD_METERS) {
                if (stationaryPosition == null) {
                    // Just became stationary
                    stationaryStartTime = currentTime
                    stationaryPosition = LatLng(filteredLat, filteredLng)
                    println("🧍 [LocationSmoother] Stationary detection started")
                } else {
                    // Check if we've been stationary long enough
                    val stationaryDuration = currentTime - stationaryStartTime
                    if (stationaryDuration >= STATIONARY_TIME_MS) {
                        // Lock to stationary position to prevent drift
                        println("🔒 [LocationSmoother] Locked to stationary position (${stationaryDuration}ms)")
                        return createStationaryLocation(currentTime)
                    }
                }
            } else {
                // User is moving, reset stationary detection
                if (stationaryPosition != null) {
                    println("🚶 [LocationSmoother] Movement detected, unlocking position")
                }
                stationaryPosition = null
                stationaryStartTime = 0
            }
        }

        // --- Create Smoothed Location ---

        val smoothedLocation = createSmoothedLocation(currentTime)
        previousLocation = smoothedLocation

        return smoothedLocation
    }

    /**
     * Create a smoothed Location object from Kalman filter state
     */
    private fun createSmoothedLocation(timestamp: Long): Location {
        val location = Location("kalman").apply {
            latitude = kalmanFilter.getLatitude()
            longitude = kalmanFilter.getLongitude()
            accuracy = kalmanFilter.getAccuracy()
            time = timestamp

            // Calculate bearing from previous position
            if (previousLocation != null) {
                val dLon = Math.toRadians(longitude - previousLocation!!.longitude)
                val lat1 = Math.toRadians(previousLocation!!.latitude)
                val lat2 = Math.toRadians(latitude)

                val y = sin(dLon) * cos(lat2)
                val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

                var bearing = Math.toDegrees(atan2(y, x)).toFloat()
                if (bearing < 0) bearing += 360f

                this.bearing = bearing
            }
        }

        return location
    }

    /**
     * Create a stationary location (locked position)
     */
    private fun createStationaryLocation(timestamp: Long): Location {
        val position = stationaryPosition ?: return createSmoothedLocation(timestamp)

        return Location("stationary").apply {
            latitude = position.latitude
            longitude = position.longitude
            accuracy = 1f // Very confident when stationary
            time = timestamp

            // Keep bearing from previous location if available
            if (previousLocation != null && previousLocation!!.hasBearing()) {
                bearing = previousLocation!!.bearing
            }
        }
    }

    /**
     * Reset the smoother (useful when starting a new run)
     */
    fun reset() {
        kalmanFilter.reset()
        previousLocation = null
        lastAcceptedLocation = null
        lastAcceptedTime = 0
        lastAnchorPoint = null
        stationaryPosition = null
        stationaryStartTime = 0
        lastUpdateTime = 0
        consecutiveRejectionsStartTime = 0
        println("🔄 [LocationSmoother] Reset complete")
    }

    /**
     * Get the current smoothed position without adding a new measurement
     */
    fun getCurrentSmoothedLocation(): Location? {
        return previousLocation
    }

    /**
     * Get the current anchor point (for distance calculations)
     */
    fun getAnchorPoint(): LatLng? {
        return lastAnchorPoint
    }
}

