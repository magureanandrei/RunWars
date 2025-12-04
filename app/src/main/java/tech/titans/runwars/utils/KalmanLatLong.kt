package tech.titans.runwars.utils


/**
 * Kalman Filter implementation for GPS coordinates (latitude/longitude).
 * Uses a 2D state vector [lat, lng] with velocity estimation.
 */
class KalmanLatLong(
    private var processNoise: Float = 0.5f // Q - process noise covariance
) {
    // State vector: [latitude, longitude]
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    // Velocity estimates (degrees per second)
    private var velocityLat: Double = 0.0
    private var velocityLng: Double = 0.0

    // Covariance matrix (variance/uncertainty)
    private var varianceLat: Float = -1f
    private var varianceLng: Float = -1f

    // Time tracking
    private var lastTimestamp: Long = 0

    /**
     * Process (predict) step - predicts state based on time elapsed
     */
    fun predict(timestamp: Long) {
        if (lastTimestamp == 0L) {
            lastTimestamp = timestamp
            return
        }

        val dt = (timestamp - lastTimestamp) / 1000.0 // seconds
        if (dt <= 0 || dt > 60) { // Ignore huge time gaps
            lastTimestamp = timestamp
            return
        }

        // Predict new position based on velocity
        latitude += velocityLat * dt
        longitude += velocityLng * dt

        // Increase uncertainty due to process noise
        if (varianceLat > 0) {
            varianceLat += processNoise * dt.toFloat()
        }
        if (varianceLng > 0) {
            varianceLng += processNoise * dt.toFloat()
        }

        lastTimestamp = timestamp
    }

    /**
     * Update step - corrects prediction with GPS measurement
     */
    fun update(
        measuredLat: Double,
        measuredLng: Double,
        accuracy: Float,
        timestamp: Long
    ) {
        // Initialize state on first measurement
        if (varianceLat < 0) {
            latitude = measuredLat
            longitude = measuredLng
            varianceLat = accuracy
            varianceLng = accuracy
            lastTimestamp = timestamp
            return
        }

        // Measurement variance (GPS accuracy)
        val measurementVariance = accuracy

        // Kalman Gain calculation
        val kalmanGainLat = varianceLat / (varianceLat + measurementVariance)
        val kalmanGainLng = varianceLng / (varianceLng + measurementVariance)

        // Update state estimate
        val innovationLat = measuredLat - latitude
        val innovationLng = measuredLng - longitude

        latitude += kalmanGainLat * innovationLat
        longitude += kalmanGainLng * innovationLng

        // Update velocity estimate (simple differentiation)
        val dt = (timestamp - lastTimestamp) / 1000.0
        if (dt > 0 && dt < 60) {
            velocityLat = innovationLat / dt
            velocityLng = innovationLng / dt
        }

        // Update covariance (reduce uncertainty)
        varianceLat *= (1 - kalmanGainLat)
        varianceLng *= (1 - kalmanGainLng)

        // Prevent variance from getting too small (numerical stability)
        varianceLat = varianceLat.coerceAtLeast(accuracy * 0.001f)
        varianceLng = varianceLng.coerceAtLeast(accuracy * 0.001f)
    }

    /**
     * Decay confidence when GPS signal is lost
     */
    fun decayConfidence(decayRate: Float = 1.5f) {
        varianceLat += decayRate
        varianceLng += decayRate

        // Cap maximum uncertainty
        varianceLat = varianceLat.coerceAtMost(100f)
        varianceLng = varianceLng.coerceAtMost(100f)
    }

    /**
     * Get current filtered position
     */
    fun getLatitude(): Double = latitude
    fun getLongitude(): Double = longitude
    fun getAccuracy(): Float = ((varianceLat + varianceLng) / 2f).coerceAtLeast(1f)

    /**
     * Reset the filter
     */
    fun reset() {
        varianceLat = -1f
        varianceLng = -1f
        velocityLat = 0.0
        velocityLng = 0.0
        lastTimestamp = 0
    }
}

