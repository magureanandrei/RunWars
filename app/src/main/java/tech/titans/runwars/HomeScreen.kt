package tech.titans.runwars

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import tech.titans.runwars.models.Coordinates
import tech.titans.runwars.models.RunSession
import tech.titans.runwars.services.UserService
import tech.titans.runwars.utils.LocationUtils.calculateCapturedArea
import tech.titans.runwars.utils.LocationUtils.createUserMarkerBitmap
import tech.titans.runwars.utils.LocationUtils.isClosedLoop
import java.io.IOException
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes

@Composable
fun HomeScreen(navController: NavController) {
    val userId = FirebaseAuth.getInstance().currentUser!!.uid
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    var currentLocation by remember { mutableStateOf(LatLng(46.7712, 23.6236)) }
    var isRunning by remember { mutableStateOf(false) }
    var pathPoints by remember { mutableStateOf(listOf<LatLng>()) }
    var distanceMeters by remember { mutableStateOf(0.0) }
    var capturedAreaMeters2 by remember { mutableStateOf<Double?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var continueRun by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(currentLocation, 14f)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val newLocation = LatLng(it.latitude, it.longitude)
                            currentLocation = newLocation
                            cameraPositionState.position =
                                com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(newLocation, 15f)
                        }
                    }
                }
            }
        }
    )

    // Location request - update every 3 seconds for real-time tracking during run
    val locationRequest = remember {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .build()
    }

    // Passive location request - for position updates when not running
    val passiveLocationRequest = remember {
        LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
    }

    // Location callback for tracking during run
    val trackingLocationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                println("🔍 Tracking update received. isRunning=$isRunning, pathPoints size=${pathPoints.size}")

                if (!isRunning) {
                    println("❌ Not running, ignoring tracking update")
                    return
                }

                val loc = result.lastLocation
                if (loc == null) {
                    println("❌ Location is null")
                    return
                }

                val newLocation = LatLng(loc.latitude, loc.longitude)
                println("✅ New location: ${newLocation.latitude}, ${newLocation.longitude}")

                // Minimum distance threshold to prevent GPS jitter (8 meters)
                val minDistanceThreshold = 8.0 // meters

                // Check if we should add this point
                val shouldAddPoint = if (pathPoints.isEmpty()) {
                    // Always add the first point
                    true
                } else {
                    // Only add if distance from last point is >= threshold
                    val lastPoint = pathPoints.last()
                    val distanceFromLast = SphericalUtil.computeDistanceBetween(lastPoint, newLocation)
                    println("📏 Distance from last point: $distanceFromLast meters (threshold: $minDistanceThreshold)")
                    distanceFromLast >= minDistanceThreshold
                }

                if (shouldAddPoint) {
                    // Add point to path
                    pathPoints = pathPoints + newLocation
                    println("📍 Point added! Path now has ${pathPoints.size} points")

                    // Calculate distance
                    if (pathPoints.size >= 2) {
                        val oldDistance = distanceMeters
                        distanceMeters = SphericalUtil.computeLength(pathPoints)
                        println("📏 Distance updated: $oldDistance -> $distanceMeters meters")
                    }
                } else {
                    println("⏭️ Point skipped (too close to last point)")
                }

                // Always update camera to follow user (even if point not added)
                currentLocation = newLocation
                cameraPositionState.position =
                    com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(newLocation, 16f)
            }
        }
    }

    // Location callback for passive position updates (when not running)
    val passiveLocationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (isRunning) return // Don't update if we're actively tracking

                val loc = result.lastLocation ?: return
                val newLocation = LatLng(loc.latitude, loc.longitude)
                currentLocation = newLocation
                println("📍 Passive position updated: ${newLocation.latitude}, ${newLocation.longitude}")
            }
        }
    }

    val userMarkerState = rememberMarkerState(position = currentLocation)

    // Update marker position when current location changes
    LaunchedEffect(currentLocation) {
        userMarkerState.position = currentLocation
    }

    // Initialize location services
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        // Start passive location updates for when not running
        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.requestLocationUpdates(
                    passiveLocationRequest,
                    passiveLocationCallback,
                    Looper.getMainLooper()
                )
                println("✅ Passive location updates started")
            }
        } catch (e: SecurityException) {
            println("❌ Could not start passive location updates: ${e.message}")
        }
    }

    // Cleanup location updates when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            fusedLocationClient.removeLocationUpdates(passiveLocationCallback)
            fusedLocationClient.removeLocationUpdates(trackingLocationCallback)
        }
    }

    // Result dialog after run
    if (showResultDialog && capturedAreaMeters2 != null) {
        val isLoop = isClosedLoop(pathPoints)

        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
                continueRun = true
                isRunning = true
                try {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            trackingLocationCallback,
                            Looper.getMainLooper()
                        )
                        println("▶️ Resumed location tracking with ${pathPoints.size} points")
                    }
                } catch (e: SecurityException) {
                    println("❌ Could not resume tracking: ${e.message}")
                }
                               },
            title = {
                Text(
                    if (isLoop) "Territory Captured!" else "Run Completed",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text("Distance: %.2f km".format(distanceMeters / 1000))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoop && capturedAreaMeters2!! > 0) {
                        Text("Territory: %.2f ha".format(
                            capturedAreaMeters2!! / 10_000
                        ))
                    } else {
                        Text(
                            "⚠️ You need to run in a loop to capture territory!",
                            color = Color(0xFFFF9800),
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tip: End your run close to where you started (within 50m)",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    if (isLoop && capturedAreaMeters2!! > 0) {
                        Button(
                            onClick = {
                                showResultDialog = false
                                UserService.addRunSessionToUser(distanceMeters,pathPoints, userId)
                                pathPoints = emptyList()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E6F))
                        ) {
                            Text("Save Territory")
                        }
                        TextButton(onClick = {
                            showResultDialog = false
                            pathPoints = emptyList()
                            capturedAreaMeters2 = null
                            distanceMeters = 0.0
                            continueRun = false
                        }) {
                            Text("Discard")
                        }
                    } else {
                        if(pathPoints.size >= 3) {
                            Button(
                                onClick = {
                                    showResultDialog = false
                                    UserService.addRunSessionToUser(
                                        distanceMeters,
                                        pathPoints,
                                        userId
                                    )
                                    pathPoints = emptyList()
                                    continueRun = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF2D3E6F
                                    )
                                )
                            ) {
                                Text("Save the run anyway")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = {
                                    showResultDialog = false
//                                    pathPoints = emptyList()
//                                    capturedAreaMeters2 = null
//                                    distanceMeters = 0.0
                                    continueRun = true
                                    isRunning = true
                                    try {
                                        if (ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            fusedLocationClient.requestLocationUpdates(
                                                locationRequest,
                                                trackingLocationCallback,
                                                Looper.getMainLooper()
                                            )
                                            println("▶️ Resumed location tracking with ${pathPoints.size} points")
                                        }
                                    } catch (e: SecurityException) {
                                        println("❌ Could not resume tracking: ${e.message}")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF757575
                                    )
                                )
                            ) {
                                Text("OK")
                            }
                            TextButton(onClick = {
                                showResultDialog = false
                                pathPoints = emptyList()
                                capturedAreaMeters2 = null
                                distanceMeters = 0.0
                                continueRun = false
                            }) {
                                Text("Discard")
                            }
                        }

                    }
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1E2A47),
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "RunWars",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.headlineMedium
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                listOf("Profile", "Statistics", "Leaderboard", "Settings").forEach {
                    NavigationDrawerItem(
                        label = { Text(it, color = Color.White) },
                        selected = false,
                        onClick = {},
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color(0xFF2D3E6F)
                        )
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Custom user position marker
                Marker(
                    state = userMarkerState,
                    title = if (isRunning) "Running..." else "You",
                    icon = BitmapDescriptorFactory.fromBitmap(createUserMarkerBitmap(isRunning)),
                    anchor = Offset(0.5f, 0.5f),
                    flat = true
                )

                // Running path (cyan line)
                if (pathPoints.size >= 2) {
                    Polyline(
                        points = pathPoints,
                        color = if (isRunning) Color.Cyan else Color.Blue,
                        width = 8f
                    )
                }

                // Show captured territory when run is finished AND it forms a loop
                if (!isRunning && pathPoints.size >= 3 && isClosedLoop(pathPoints)) {
                    val closedPath = if (pathPoints.first() != pathPoints.last()) {
                        pathPoints + pathPoints.first()
                    } else {
                        pathPoints
                    }

                    Polygon(
                        points = closedPath,
                        fillColor = Color(0x552D3E6F),
                        strokeColor = Color.Blue,
                        strokeWidth = 4f
                    )
                }
            }

            // Menu button (top right)
            Surface(
                onClick = {
                    scope.launch {
                        if (drawerState.isOpen) drawerState.close() else drawerState.open()
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFF1E2A47),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Distance display (top left)
            if (isRunning || pathPoints.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color(0xDD1E2A47), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isRunning) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Green, androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isRunning) "RUNNING" else "FINISHED",
                            color = if (isRunning) Color.Green else Color.White,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%.2f km".format(distanceMeters / 1000),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    if (!isRunning && capturedAreaMeters2 != null && capturedAreaMeters2!! > 0 && isClosedLoop(pathPoints)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Territory: %.2f ha".format(capturedAreaMeters2!! / 10_000),
                            color = Color.Cyan,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Start/Finish button (bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xEE1E2A47))
                    .padding(vertical = 16.dp, horizontal = 24.dp)
            ) {
                Button(
                    onClick = {
                        val fineGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!fineGranted) {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            return@Button
                        }

                        if (!isRunning) {
                            // START RUN
                            println("🏁 Starting run...")

                            // Reset state
                            if (!continueRun) {
                                pathPoints = emptyList()
                                distanceMeters = 0.0
                                capturedAreaMeters2 = null

                                // Get current location and add it as first point
                                try {
                                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                        location?.let {
                                            val startLocation = LatLng(it.latitude, it.longitude)
                                            pathPoints = listOf(startLocation)
                                            currentLocation = startLocation
                                            println("📍 Starting location: ${startLocation.latitude}, ${startLocation.longitude}")
                                        }
                                    }
                                } catch (_: SecurityException) { }
                            } else {
                                // Continuing run - just resume from current state
                                println("▶️ Resuming run with ${pathPoints.size} existing points")
                            }

                            // Start location updates
                            isRunning = true

                            try {
                                fusedLocationClient.requestLocationUpdates(
                                    locationRequest,
                                    trackingLocationCallback,
                                    Looper.getMainLooper()
                                )
                                println("✅ Location updates started")
                            } catch (e: SecurityException) {
                                println("❌ Security exception: ${e.message}")
                            }
                        } else {
                            // FINISH RUN
                                isRunning = false
                                fusedLocationClient.removeLocationUpdates(trackingLocationCallback)

                                // Calculate captured territory only if it's a closed loop
                                capturedAreaMeters2 =
                                    if (pathPoints.size >= 3 && isClosedLoop(pathPoints)) {
                                        calculateCapturedArea(pathPoints)
                                    } else {
                                        0.0
                                    }

                                // Show result dialog
                                showResultDialog = true
                            }

                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFFD32F2F) else Color(0xFF2D3E6F)
                    )
                ) {
                    Text(
                        if (!isRunning) "START RUN" else "FINISH RUN",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
    }
}