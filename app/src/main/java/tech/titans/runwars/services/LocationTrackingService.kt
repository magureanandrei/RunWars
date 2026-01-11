package tech.titans.runwars.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.titans.runwars.MainActivity
import tech.titans.runwars.R
import tech.titans.runwars.utils.LocationSmoother
import java.util.Locale

class LocationTrackingService : Service() {

    private val binder = LocationBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var notificationManager: NotificationManager? = null

    // GPS Smoothing system
    private val locationSmoother = LocationSmoother()

    // State flows for reactive updates
    // Multiple segments to support path breaks (e.g., after large pause gaps)
    private val _pathSegments = MutableStateFlow<List<List<LatLng>>>(listOf(emptyList()))
    val pathSegments: StateFlow<List<List<LatLng>>> = _pathSegments.asStateFlow()

    // Flattened path points for backward compatibility
    private val _pathPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val pathPoints: StateFlow<List<LatLng>> = _pathPoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    // Pause/resume tracking
    private var lastLocationBeforePause: LatLng? = null
    private var pauseStartTime: Long? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val MIN_DISTANCE_THRESHOLD = 8.0 // meters
        private const val MAX_PAUSE_GAP_METERS = 50.0 // Maximum gap to continue path seamlessly
    }

    inner class LocationBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (!_isTracking.value) return

                val rawLocation = result.lastLocation ?: return

                // 1. "Null Island" Fix: Ignore 0,0 coordinates (happens during GPS init)
                if (Math.abs(rawLocation.latitude) < 0.0001 && Math.abs(rawLocation.longitude) < 0.0001) {
                    println("🛑 [Service] Ignoring invalid (0,0) location")
                    return
                }

                // 2. "Teleport" Fix: Ignore massive jumps (> 150m instantly)
                // We compare against the last VALID point we added
                val lastValidPoint = _pathPoints.value.lastOrNull()
                if (lastValidPoint != null) {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        lastValidPoint.latitude, lastValidPoint.longitude,
                        rawLocation.latitude, rawLocation.longitude,
                        results
                    )
                    val distanceJump = results[0]

                    // 150m jump in ~1 second is impossible for a runner (that's 540 km/h)
                    if (distanceJump > 150) {
                        println("🛑 [Service] Ignoring teleport jump: ${distanceJump}m")
                        return
                    }
                }
                println("📍 [Service] Raw GPS: ${rawLocation.latitude}, ${rawLocation.longitude}, accuracy: ${rawLocation.accuracy}m")

                // Apply Kalman filtering and smoothing
                val smoothedLocation = locationSmoother.addLocation(rawLocation)
                val newLocation = LatLng(smoothedLocation.latitude, smoothedLocation.longitude)

                println("✨ [Service] Smoothed: ${newLocation.latitude}, ${newLocation.longitude}, accuracy: ${smoothedLocation.accuracy}m")

                // Update current location for UI (even when paused)
                _currentLocation.value = newLocation

                // Skip adding points if paused
                if (_isPaused.value) {
                    println("⏸️ [Service] Paused - location updated but not adding point")
                    return
                }

                // Get current segment (last in the list)
                val currentSegment = _pathSegments.value.lastOrNull() ?: emptyList()

                // Check if we should add this point (distance threshold)
                val shouldAddPoint = if (currentSegment.isEmpty()) {
                    true
                } else {
                    val lastPoint = currentSegment.last()
                    val distanceFromLast = SphericalUtil.computeDistanceBetween(lastPoint, newLocation)
                    println("📏 [Service] Distance from last: $distanceFromLast m (threshold: $MIN_DISTANCE_THRESHOLD)")
                    distanceFromLast >= MIN_DISTANCE_THRESHOLD
                }

                if (shouldAddPoint) {
                    // Add point to current segment
                    val updatedSegment = currentSegment + newLocation
                    val updatedSegments = _pathSegments.value.dropLast(1) + listOf(updatedSegment)
                    _pathSegments.value = updatedSegments

                    // Update flattened pathPoints for backward compatibility
                    _pathPoints.value = updatedSegments.flatten()

                    println("📍 [Service] Point added! Segment: ${updatedSegment.size} points, Total: ${_pathPoints.value.size}")

                    // Calculate total distance across all segments
                    if (_pathPoints.value.size >= 2) {
                        var totalDistance = 0.0
                        for (segment in updatedSegments) {
                            if (segment.size >= 2) {
                                totalDistance += SphericalUtil.computeLength(segment)
                            }
                        }
                        _distanceMeters.value = totalDistance
                        println("📏 [Service] Total distance: ${_distanceMeters.value} m")

                        // Update notification with current stats
                        updateNotification()
                    }
                } else {
                    println("⏭️ [Service] Point skipped (too close)")
                }
            }
        }
    }

    fun startTracking() {
        if (_isTracking.value) {
            println("⚠️ [Service] Already tracking")
            return
        }

        println("🏁 [Service] Starting tracking...")
        _isTracking.value = true

        // Start foreground service
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Start location updates
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            println("✅ [Service] Location updates started")
        } catch (e: SecurityException) {
            println("❌ [Service] Security exception: ${e.message}")
            _isTracking.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    fun stopTracking() {
        if (!_isTracking.value) {
            println("⚠️ [Service] Not tracking")
            return
        }

        println("🛑 [Service] Stopping tracking...")
        _isTracking.value = false
        _isPaused.value = false

        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        println("✅ [Service] Tracking stopped")
    }

    fun pauseTracking() {
        if (!_isTracking.value) {
            println("⚠️ [Service] Not tracking")
            return
        }

        if (_isPaused.value) {
            println("⚠️ [Service] Already paused")
            return
        }

        println("⏸️ [Service] Pausing tracking...")
        _isPaused.value = true
        lastLocationBeforePause = _pathPoints.value.lastOrNull()
        pauseStartTime = System.currentTimeMillis()
        println("📍 [Service] Saved pause location: $lastLocationBeforePause")
        updateNotification()
        println("✅ [Service] Tracking paused")
    }

    fun resumeTracking() {
        if (!_isTracking.value) {
            println("⚠️ [Service] Not tracking")
            return
        }

        if (!_isPaused.value) {
            println("⚠️ [Service] Not paused")
            return
        }

        println("▶️ [Service] Resuming tracking...")

        // Check gap distance if we have both locations
        val lastLoc = lastLocationBeforePause
        val currentLoc = _currentLocation.value

        if (lastLoc != null && currentLoc != null) {
            val gapDistance = SphericalUtil.computeDistanceBetween(lastLoc, currentLoc)
            val pauseDuration = System.currentTimeMillis() - (pauseStartTime ?: 0L)

            println("🔍 [Service] Resume gap analysis:")
            println("   Last location before pause: $lastLoc")
            println("   Current location: $currentLoc")
            println("   Gap distance: ${gapDistance}m")
            println("   Pause duration: ${pauseDuration}ms")

            if (gapDistance > MAX_PAUSE_GAP_METERS) {
                // Gap too large - start a NEW SEGMENT (clearly separated)
                println("⚠️ [Service] Gap > ${MAX_PAUSE_GAP_METERS}m detected!")
                println("   ✂️ BREAKING PATH - Starting new segment")
                println("   ❌ Gap distance NOT added to total")
                println("   ❌ Path will NOT form a closed loop")

                // Start a new empty segment - next location will be added there
                _pathSegments.value = _pathSegments.value + listOf(emptyList())
                println("   📊 Total segments: ${_pathSegments.value.size}")

            } else {
                // Gap is small enough - continue path seamlessly
                println("✅ [Service] Gap ≤ ${MAX_PAUSE_GAP_METERS}m, continuing path seamlessly")
                println("   Adding ${gapDistance}m to total distance")

                // Add the gap distance to total
                _distanceMeters.value += gapDistance

                // Add current location to current segment to connect
                val currentSegment = _pathSegments.value.lastOrNull() ?: emptyList()
                if (currentSegment.lastOrNull() != currentLoc) {
                    val updatedSegment = currentSegment + currentLoc
                    val updatedSegments = _pathSegments.value.dropLast(1) + listOf(updatedSegment)
                    _pathSegments.value = updatedSegments
                    _pathPoints.value = updatedSegments.flatten()
                }
            }
        } else {
            println("⚠️ [Service] Missing location data for gap analysis")
        }

        _isPaused.value = false
        lastLocationBeforePause = null
        pauseStartTime = null
        updateNotification()
        println("✅ [Service] Tracking resumed")
    }

    fun resetTracking() {
        println("🔄 [Service] Resetting tracking data...")
        _pathSegments.value = listOf(emptyList())
        _pathPoints.value = emptyList()
        _distanceMeters.value = 0.0
        _currentLocation.value = null
        _isPaused.value = false
        locationSmoother.reset()
    }

    fun continueTracking(existingPoints: List<LatLng>, existingDistance: Double) {
        println("▶️ [Service] Continuing tracking with ${existingPoints.size} existing points")
        _pathSegments.value = if (existingPoints.isNotEmpty()) {
            listOf(existingPoints)
        } else {
            listOf(emptyList())
        }
        _pathPoints.value = existingPoints
        _distanceMeters.value = existingDistance
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your running location in the background"
                setShowBadge(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distanceKm = _distanceMeters.value / 1000

        // Calculate territory in hectares
        val territoryHa = if (_pathPoints.value.size >= 3) {
            val areaSqMeters = SphericalUtil.computeArea(_pathPoints.value)
            areaSqMeters / 10000.0 // Convert to hectares (1 ha = 10,000 m²)
        } else {
            0.0
        }

        val title = if (_isPaused.value) "RunWars - Run Paused" else "RunWars - Tracking Run"

        val contentText = when {
            _pathPoints.value.isEmpty() -> "Starting run..."
            _isPaused.value -> String.format(Locale.US, "PAUSED · %.2f km", distanceKm)
            else -> String.format(Locale.US, "RUNNING · %.2f km", distanceKm)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        println("🗑️ [Service] Service destroyed")
    }
}

