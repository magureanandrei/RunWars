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
import java.util.Locale

class LocationTrackingService : Service() {

    private val binder = LocationBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var notificationManager: NotificationManager? = null

    // State flows for reactive updates
    private val _pathPoints = MutableStateFlow<List<LatLng>>(emptyList())
    val pathPoints: StateFlow<List<LatLng>> = _pathPoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val MIN_DISTANCE_THRESHOLD = 8.0 // meters
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

                val location = result.lastLocation ?: return
                val newLocation = LatLng(location.latitude, location.longitude)

                println("🔍 [Service] Location update: ${newLocation.latitude}, ${newLocation.longitude}")

                // Update current location for UI
                _currentLocation.value = newLocation

                // Check if we should add this point (distance threshold)
                val shouldAddPoint = if (_pathPoints.value.isEmpty()) {
                    true
                } else {
                    val lastPoint = _pathPoints.value.last()
                    val distanceFromLast = SphericalUtil.computeDistanceBetween(lastPoint, newLocation)
                    println("📏 [Service] Distance from last: $distanceFromLast m (threshold: $MIN_DISTANCE_THRESHOLD)")
                    distanceFromLast >= MIN_DISTANCE_THRESHOLD
                }

                if (shouldAddPoint) {
                    // Add point to path
                    _pathPoints.value = _pathPoints.value + newLocation
                    println("📍 [Service] Point added! Total points: ${_pathPoints.value.size}")

                    // Calculate total distance
                    if (_pathPoints.value.size >= 2) {
                        _distanceMeters.value = SphericalUtil.computeLength(_pathPoints.value)
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

        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        println("✅ [Service] Tracking stopped")
    }

    fun resetTracking() {
        println("🔄 [Service] Resetting tracking data...")
        _pathPoints.value = emptyList()
        _distanceMeters.value = 0.0
        _currentLocation.value = null
    }

    fun continueTracking(existingPoints: List<LatLng>, existingDistance: Double) {
        println("▶️ [Service] Continuing tracking with ${existingPoints.size} existing points")
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
        val contentText = if (_pathPoints.value.isEmpty()) {
            "Starting run..."
        } else {
            String.format(Locale.US, "%.2f km · %d points", distanceKm, _pathPoints.value.size)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RunWars - Tracking Run")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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

