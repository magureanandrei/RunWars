package tech.titans.runwars

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import tech.titans.runwars.repo.UserRepo
import tech.titans.runwars.services.LocationTrackingService
import tech.titans.runwars.services.UserService
import tech.titans.runwars.utils.BatteryOptimizationHelper
import tech.titans.runwars.utils.LocationUtils.calculateCapturedArea
import tech.titans.runwars.utils.LocationUtils.createUserMarkerBitmap
import tech.titans.runwars.utils.LocationUtils.isClosedLoop
import tech.titans.runwars.models.FriendTerritory
import tech.titans.runwars.models.FriendTerritoryColors

@Composable
fun HomeScreen(navController: NavController) {
    val userId = FirebaseAuth.getInstance().currentUser!!.uid
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Service binding
    var locationService by remember { mutableStateOf<LocationTrackingService?>(null) }
    var serviceBound by remember { mutableStateOf(false) }

    // UI state from service
    var currentLocation by remember { mutableStateOf(LatLng(46.7712, 23.6236)) }
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var pathPoints by remember { mutableStateOf(listOf<LatLng>()) }
    var pathSegments by remember { mutableStateOf(listOf<List<LatLng>>()) }
    var distanceMeters by remember { mutableStateOf(0.0) }
    var capturedAreaMeters2 by remember { mutableStateOf<Double?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var continueRun by remember { mutableStateOf(false) }

    // Permission dialogs
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }

    // Saved territories from database
    var savedTerritories by remember { mutableStateOf<List<List<LatLng>>>(emptyList()) }

    // Friend territories
    var friendTerritories by remember { mutableStateOf<List<FriendTerritory>>(emptyList()) }
    var friendsWithVisibleTerritories by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Kingdom viewer modal
    var showKingdomViewerModal by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf("") }
    var friendsList by remember { mutableStateOf<List<tech.titans.runwars.models.User>>(emptyList()) }
    var viewingKingdomOf by remember { mutableStateOf<String?>(null) } // userId of person whose kingdom we're viewing
    var viewingKingdomTerritories by remember { mutableStateOf<List<List<LatLng>>>(emptyList()) }

    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(currentLocation, 14f)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Service connection
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val locationBinder = binder as? LocationTrackingService.LocationBinder
                locationService = locationBinder?.getService()
                serviceBound = true
                println("✅ Service connected")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                locationService = null
                serviceBound = false
                println("❌ Service disconnected")
            }
        }
    }

    // Background location permission launcher (for Android 10+)
    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                println("✅ Background location permission granted")
                // Check battery optimization after background permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!BatteryOptimizationHelper.isBatteryOptimizationDisabled(context)) {
                        showBatteryOptimizationDialog = true
                    }
                }
            } else {
                println("❌ Background location permission denied")
            }
        }
    )

    // Notification permission launcher (for Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                println("✅ Notification permission granted")
            }
        }
    )

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

                    // Request background location permission for Android 10+ (only if not already granted)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val backgroundLocationGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!backgroundLocationGranted) {
                            showBackgroundLocationDialog = true
                        } else {
                            // Background permission already granted, check battery optimization
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (!BatteryOptimizationHelper.isBatteryOptimizationDisabled(context)) {
                                    showBatteryOptimizationDialog = true
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    // Passive location request - for position updates when not running
    val passiveLocationRequest = remember {
        LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
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

    // Sync state from service
    LaunchedEffect(serviceBound, locationService) {
        if (serviceBound && locationService != null) {
            locationService!!.isTracking.collect { tracking ->
                isRunning = tracking
            }
        }
    }

    LaunchedEffect(serviceBound, locationService) {
        if (serviceBound && locationService != null) {
            locationService!!.isPaused.collect { paused ->
                isPaused = paused
            }
        }
    }

    LaunchedEffect(serviceBound, locationService) {
        if (serviceBound && locationService != null) {
            locationService!!.pathPoints.collect { points ->
                pathPoints = points
            }
        }
    }

    LaunchedEffect(serviceBound, locationService) {
        if (serviceBound && locationService != null) {
            locationService!!.pathSegments.collect { segments ->
                pathSegments = segments
            }
        }
    }

    LaunchedEffect(serviceBound, locationService) {
        if (serviceBound && locationService != null) {
            locationService!!.distanceMeters.collect { distance ->
                distanceMeters = distance
            }
        }
    }

    LaunchedEffect(serviceBound, locationService) {
        if (serviceBound && locationService != null) {
            locationService!!.currentLocation.collect { location ->
                location?.let {
                    currentLocation = it
                    // Don't auto-center camera during run - let user control zoom/pan
                }
            }
        }
    }

    val userMarkerState = rememberMarkerState(position = currentLocation)

    // Update marker position when current location changes
    LaunchedEffect(currentLocation) {
        userMarkerState.position = currentLocation
    }

    // Fetch saved territories from Firebase
    LaunchedEffect(userId) {
        println("🚀 HomeScreen: Starting territory fetch for user: $userId")

        UserRepo.getUserWithRunSessions(userId) { user, runSessions, error ->
            if (error != null) {
                println("❌ HomeScreen: Error fetching user: $error")
                return@getUserWithRunSessions
            }

            if (user == null) {
                println("❌ HomeScreen: User is null")
                return@getUserWithRunSessions
            }

            println("📊 HomeScreen: Received ${runSessions.size} run sessions")

            val territories = runSessions.mapNotNull { runSession ->
                if (runSession.coordinatesList.size >= 3) {
                    val points = runSession.coordinatesList.map { coord ->
                        LatLng(coord.latitude, coord.longitude)
                    }
                    // Print first coordinate of each territory for debugging
                    val firstPoint = points.first()
                    println("  ✅ Territory with ${points.size} points at (${firstPoint.latitude}, ${firstPoint.longitude})")
                    points
                } else {
                    println("  ⚠️ Run ${runSession.runId} has only ${runSession.coordinatesList.size} coordinates, skipping")
                    null
                }
            }

            savedTerritories = territories
            println("🗺️ HomeScreen: Displaying ${territories.size} territories on map")

            // Save user info for kingdom viewer
            currentUserName = user.userName
            friendsList = user.friendsList

            // If we have territories, print their locations
            if (territories.isNotEmpty()) {
                println("📍 Territory locations:")
                territories.forEachIndexed { index, points ->
                    val center = points.first()
                    println("   Territory $index: center around (${center.latitude}, ${center.longitude})")
                }
                println("📍 Your current location: (${currentLocation.latitude}, ${currentLocation.longitude})")
            }
        }
    }

    // Load friend territory visibility preferences
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("RunWarsPrefs", Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet("visibleFriendTerritories", emptySet()) ?: emptySet()
        friendsWithVisibleTerritories = savedSet
        println("📂 HomeScreen: Loaded ${savedSet.size} visible friend territories from prefs")

        // Check for pending kingdom view (from FriendsListScreen)
        val pendingKingdomUserId = prefs.getString("pendingKingdomView", null)
        val pendingKingdomUserName = prefs.getString("pendingKingdomViewName", null)
        if (pendingKingdomUserId != null && pendingKingdomUserName != null) {
            // Clear the pending view
            prefs.edit()
                .remove("pendingKingdomView")
                .remove("pendingKingdomViewName")
                .apply()

            // Trigger kingdom view
            viewingKingdomOf = pendingKingdomUserId

            // Fetch territories for the selected person
            UserRepo.getUserWithRunSessions(pendingKingdomUserId) { _, runSessions, error ->
                if (error != null) {
                    println("❌ Error fetching kingdom for $pendingKingdomUserName: $error")
                    return@getUserWithRunSessions
                }

                val territories = runSessions.mapNotNull { runSession ->
                    if (runSession.coordinatesList.size >= 3) {
                        runSession.coordinatesList.map { coord ->
                            LatLng(coord.latitude, coord.longitude)
                        }
                    } else null
                }

                viewingKingdomTerritories = territories
                println("👑 Loaded ${territories.size} territories for $pendingKingdomUserName's kingdom")

                // Zoom to fit all territories
                if (territories.isNotEmpty()) {
                    val allPoints = territories.flatten()
                    val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                    allPoints.forEach { boundsBuilder.include(it) }
                    val bounds = boundsBuilder.build()

                    scope.launch {
                        cameraPositionState.animate(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 100),
                            durationMs = 1000
                        )
                    }
                }
            }
        }
    }

    // Fetch friend territories when visibility preferences change
    LaunchedEffect(friendsWithVisibleTerritories) {
        if (friendsWithVisibleTerritories.isEmpty()) {
            friendTerritories = emptyList()
            println("ℹ️ HomeScreen: No friends selected to show territories")
            return@LaunchedEffect
        }

        println("🔄 HomeScreen: Fetching friend territories for ${friendsWithVisibleTerritories.size} friends")

        UserRepo.getFriendsTerritories(userId, friendsWithVisibleTerritories) { territories, error ->
            if (error != null) {
                println("❌ HomeScreen: Error fetching friend territories: $error")
            } else {
                friendTerritories = territories
                println("✅ HomeScreen: Loaded territories for ${territories.size} friends")
                territories.forEach { ft ->
                    println("   👤 ${ft.friendName}: ${ft.territories.size} territories")
                }
            }
        }
    }    // Background location permission dialog
    if (showBackgroundLocationDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationDialog = false },
            title = { Text("Background Location Permission") },
            text = {
                Text(
                    "To track your runs accurately even when the app is in the background or closed, " +
                    "RunWars needs access to your location all the time.\n\n" +
                    "Please select 'Allow all the time' on the next screen."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackgroundLocationDialog = false
                        backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundLocationDialog = false }) {
                    Text("Skip")
                }
            }
        )
    }

    // Battery optimization dialog
    if (showBatteryOptimizationDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        AlertDialog(
            onDismissRequest = { showBatteryOptimizationDialog = false },
            title = { Text("Disable Battery Optimization") },
            text = {
                Text(
                    "To ensure continuous GPS tracking during your runs, please disable " +
                    "battery optimization for RunWars.\n\n" +
                    "This prevents Android from stopping location updates to save battery."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryOptimizationDialog = false
                        BatteryOptimizationHelper.requestDisableBatteryOptimization(context)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryOptimizationDialog = false }) {
                    Text("Skip")
                }
            }
        )
    }

    // Kingdom Viewer Modal
    if (showKingdomViewerModal) {
        KingdomViewerModal(
            currentUserId = userId,
            currentUserName = currentUserName,
            friendsList = friendsList,
            onSelectPerson = { selectedUserId, selectedUserName ->
                showKingdomViewerModal = false
                viewingKingdomOf = selectedUserId

                // Fetch territories for the selected person
                UserRepo.getUserWithRunSessions(selectedUserId) { _, runSessions, error ->
                    if (error != null) {
                        println("❌ Error fetching kingdom for $selectedUserName: $error")
                        return@getUserWithRunSessions
                    }

                    val territories = runSessions.mapNotNull { runSession ->
                        if (runSession.coordinatesList.size >= 3) {
                            runSession.coordinatesList.map { coord ->
                                LatLng(coord.latitude, coord.longitude)
                            }
                        } else null
                    }

                    viewingKingdomTerritories = territories
                    println("👑 Loaded ${territories.size} territories for $selectedUserName's kingdom")

                    // Zoom to fit all territories
                    if (territories.isNotEmpty()) {
                        val allPoints = territories.flatten()
                        val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
                        allPoints.forEach { boundsBuilder.include(it) }
                        val bounds = boundsBuilder.build()

                        scope.launch {
                            cameraPositionState.animate(
                                com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 100),
                                durationMs = 1000
                            )
                        }
                    }
                }
            },
            onDismiss = { showKingdomViewerModal = false }
        )
    }

    // Result dialog after run
    LaunchedEffect(Unit) {
        // Bind to location service
        val intent = Intent(context, LocationTrackingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Request permissions
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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
            context.unbindService(serviceConnection)
            println("🗑️ Service unbound")
        }
    }

    // Result dialog after run
    if (showResultDialog && capturedAreaMeters2 != null) {
        val isLoop = isClosedLoop(pathPoints)

        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
                continueRun = true
                // Resume tracking through service
                if (serviceBound && locationService != null) {
                    locationService!!.continueTracking(pathPoints, distanceMeters)
                    locationService!!.startTracking()
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
                                UserService.addRunSessionToUser(distanceMeters, pathPoints, userId)

                                // Immediately add the newly captured territory to the map
                                // Use the same sampling logic as UserService (every 2nd point + first and last)
                                val newTerritoryPoints = pathPoints.filterIndexed { index, _ ->
                                    index == 0 || index % 2 == 0 || index == pathPoints.size - 1
                                }
                                if (newTerritoryPoints.size >= 3) {
                                    savedTerritories = savedTerritories + listOf(newTerritoryPoints)
                                    println("✅ Added new territory with ${newTerritoryPoints.size} points to map")
                                }

                                // Reset service
                                if (serviceBound && locationService != null) {
                                    locationService!!.resetTracking()
                                }
                                continueRun = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E6F))
                        ) {
                            Text("Save Territory")
                        }
                        TextButton(onClick = {
                            showResultDialog = false
                            // Reset service
                            if (serviceBound && locationService != null) {
                                locationService!!.resetTracking()
                            }
                            capturedAreaMeters2 = null
                            continueRun = false
                        }) {
                            Text("Discard")
                        }
                    } else {
                        if(pathPoints.size >= 3) {
                            Button(
                                onClick = {
                                    showResultDialog = false
                                    UserService.addRunSessionToUser(distanceMeters, pathPoints, userId)
                                    // Note: Non-loop runs are saved but not displayed as territories
                                    // Reset service
                                    if (serviceBound && locationService != null) {
                                        locationService!!.resetTracking()
                                    }
                                    continueRun = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E6F))
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
                                    continueRun = true
                                    // Resume tracking through service
                                    if (serviceBound && locationService != null) {
                                        locationService!!.continueTracking(pathPoints, distanceMeters)
                                        locationService!!.startTracking()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
                            ) {
                                Text("Continue")
                            }
                            TextButton(onClick = {
                                showResultDialog = false
                                // Reset service
                                if (serviceBound && locationService != null) {
                                    locationService!!.resetTracking()
                                }
                                capturedAreaMeters2 = null
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
                Column(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Header
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "RunWars",
                        modifier = Modifier.padding(16.dp),
                        color = Color.White,
                        fontSize = 24.sp,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    // Menu items
                    listOf("Profile", "Run History", "Settings", "Friends").forEach {
                        NavigationDrawerItem(
                            label = { Text(it, color = Color.White) },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close() // Close drawer first

                                    when (it) {
                                        "Profile" -> navController.navigate("profile")
                                        "Run History" -> navController.navigate("runHistory")
                                        "Settings" -> navController.navigate("settings")
                                        "Friends" -> navController.navigate("friends")
                                    }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = Color(0xFF2D3E6F)
                            )
                        )
                    }

                    // Spacer to push logout to bottom
                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    // Logout button
                    NavigationDrawerItem(
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                    contentDescription = "Logout",
                                    tint = Color(0xFFFF6B6B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Logout",
                                    color = Color(0xFFFF6B6B),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()

                                // Navigate to login screen
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            selectedContainerColor = Color(0xFF2D3E6F)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                // Custom user position marker
                Marker(
                    state = userMarkerState,
                    title = if (isRunning) "Running..." else "You",
                    icon = BitmapDescriptorFactory.fromBitmap(createUserMarkerBitmap(isRunning)),
                    anchor = Offset(0.5f, 0.5f),
                    flat = true
                )

                // Running path (multiple polylines for segments)
                // Each segment is drawn separately to show breaks when pause gap > 50m
                for (segment in pathSegments) {
                    if (segment.size >= 2) {
                        Polyline(
                            points = segment,
                            color = if (isRunning) Color.Cyan else Color(0xFF2D3E6F),
                            width = 8f
                        )
                    }
                }

                // Draw saved territories from database
                savedTerritories.forEach { territoryPoints ->
                    if (territoryPoints.size >= 3) {
                        val closedTerritoryPath = if (territoryPoints.first() != territoryPoints.last()) {
                            territoryPoints + territoryPoints.first()
                        } else {
                            territoryPoints
                        }
                        Polygon(
                            points = closedTerritoryPath,
                            fillColor = Color(0x4000FF00), // Semi-transparent green for saved territories
                            strokeColor = Color(0xFF00AA00),
                            strokeWidth = 3f
                        )
                    }
                }

                // Draw friend territories
                friendTerritories.forEach { friendTerritory ->
                    val fillColor = Color(FriendTerritoryColors.getFillColor(friendTerritory.colorIndex))
                    val strokeColor = Color(FriendTerritoryColors.getStrokeColor(friendTerritory.colorIndex))

                    friendTerritory.territories.forEach { territoryPoints ->
                        if (territoryPoints.size >= 3) {
                            val closedTerritoryPath = if (territoryPoints.first() != territoryPoints.last()) {
                                territoryPoints + territoryPoints.first()
                            } else {
                                territoryPoints
                            }
                            Polygon(
                                points = closedTerritoryPath,
                                fillColor = fillColor,
                                strokeColor = strokeColor,
                                strokeWidth = 3f
                            )
                        }
                    }
                }

                // Draw viewed kingdom territories (highlighted in purple/magenta)
                if (viewingKingdomOf != null) {
                    viewingKingdomTerritories.forEach { territoryPoints ->
                        if (territoryPoints.size >= 3) {
                            val closedTerritoryPath = if (territoryPoints.first() != territoryPoints.last()) {
                                territoryPoints + territoryPoints.first()
                            } else {
                                territoryPoints
                            }
                            Polygon(
                                points = closedTerritoryPath,
                                fillColor = Color(0x60FF00FF), // Highlighted magenta
                                strokeColor = Color(0xFFCC00CC),
                                strokeWidth = 4f
                            )
                        }
                    }
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
                        strokeColor = Color(0xFF2D3E6F),
                        strokeWidth = 4f
                    )
                }
            }

            // Dimming overlay when paused
            if (isPaused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
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
                    .windowInsetsPadding(WindowInsets.systemBars)
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

            // Kingdom viewer button (below menu button)
            Surface(
                onClick = { showKingdomViewerModal = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(top = 80.dp, end = 16.dp)
                    .size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFF1E2A47),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "View Kingdoms",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Kingdom viewing banner (top center)
            if (viewingKingdomOf != null) {
                Surface(
                    onClick = {
                        viewingKingdomOf = null
                        viewingKingdomTerritories = emptyList()
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFCC00CC).copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Viewing Territory · Tap to close",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            }


            // Distance display (top left)
            if (isRunning || pathPoints.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(16.dp)
                        .background(Color(0xDD1E2A47), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isRunning && !isPaused) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Green, androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = when {
                                isPaused -> "PAUSED"
                                isRunning -> "RUNNING"
                                else -> "FINISHED"
                            },
                            color = when {
                                isPaused -> Color(0xFFFF9800)
                                isRunning -> Color.Green
                                else -> Color.White
                            },
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

            // Recenter map FAB - show when running and (zoomed out OR user panned away)
            // Calculate distance between camera center and user's actual location
            val cameraCenterLat = cameraPositionState.position.target.latitude
            val cameraCenterLng = cameraPositionState.position.target.longitude
            val userLat = currentLocation.latitude
            val userLng = currentLocation.longitude

            // Calculate distance in meters using Haversine formula
            val earthRadius = 6371000.0 // meters
            val dLat = Math.toRadians(userLat - cameraCenterLat)
            val dLng = Math.toRadians(userLng - cameraCenterLng)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(cameraCenterLat)) * Math.cos(Math.toRadians(userLat)) *
                    Math.sin(dLng / 2) * Math.sin(dLng / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            val distanceFromCenter = earthRadius * c

            // Show button if zoomed out (< 15) OR if user is more than 100 meters away from their location
            val shouldShowRecenterButton = isRunning && (cameraPositionState.position.zoom < 15f || distanceFromCenter > 100)

            if (shouldShowRecenterButton) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(
                                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                    currentLocation,
                                    16f
                                ),
                                durationMs = 500
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(end = 16.dp, bottom = 100.dp),
                    containerColor = Color(0xFF1E2A47),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Recenter Map",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Start/Finish button (bottom)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xEE1E2A47))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                if (isRunning) {
                    // When running, show two buttons: Pause/Resume and Finish
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Pause/Resume button
                        Button(
                            onClick = {
                                if (serviceBound && locationService != null) {
                                    if (isPaused) {
                                        println("▶️ Resuming run...")
                                        locationService!!.resumeTracking()
                                    } else {
                                        println("⏸️ Pausing run...")
                                        locationService!!.pauseTracking()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaused) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        ) {
                            Text(
                                if (isPaused) "RESUME" else "PAUSE",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }

                        // Finish button
                        Button(
                            onClick = {
                                println("🛑 Finishing run via service...")

                                // Stop service tracking
                                if (serviceBound && locationService != null) {
                                    locationService!!.stopTracking()
                                }

                                // Calculate captured territory only if it's a closed loop
                                capturedAreaMeters2 =
                                    if (pathPoints.size >= 3 && isClosedLoop(pathPoints)) {
                                        calculateCapturedArea(pathPoints)
                                    } else {
                                        0.0
                                    }

                                // Show result dialog
                                showResultDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            )
                        ) {
                            Text(
                                "FINISH RUN",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // When not running, show only Start button
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

                            // START RUN
                            println("🏁 Starting run via service...")

                            if (!continueRun) {
                                // Starting fresh run
                                capturedAreaMeters2 = null

                                // Start service and tracking
                                if (serviceBound && locationService != null) {
                                    locationService!!.resetTracking()
                                    locationService!!.startTracking()
                                } else {
                                    // Service not ready, start it directly
                                    val intent = Intent(context, LocationTrackingService::class.java)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                    context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                                }
                            } else {
                                // Continuing existing run
                                println("▶️ Resuming run with ${pathPoints.size} existing points")
                                if (serviceBound && locationService != null) {
                                    locationService!!.continueTracking(pathPoints, distanceMeters)
                                    locationService!!.startTracking()
                                }
                            }

                            continueRun = false
                        },
                        modifier = Modifier
                            .padding(vertical = 12.dp, horizontal = 24.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2D3E6F)
                        )
                    ) {
                        Text(
                            "START RUN",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modal for selecting a person to view their entire kingdom (all territories)
 */
@Composable
fun KingdomViewerModal(
    currentUserId: String,
    currentUserName: String,
    friendsList: List<tech.titans.runwars.models.User>,
    onSelectPerson: (userId: String, userName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter friends based on search
    val filteredFriends = remember(searchQuery, friendsList) {
        if (searchQuery.isEmpty()) {
            friendsList
        } else {
            friendsList.filter { friend ->
                friend.userName.contains(searchQuery, ignoreCase = true) ||
                friend.firstName.contains(searchQuery, ignoreCase = true) ||
                friend.lastName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Check if "You" should be shown based on search
    val showSelf = searchQuery.isEmpty() ||
                   currentUserName.contains(searchQuery, ignoreCase = true) ||
                   "you".contains(searchQuery, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C1E3C),
        title = {
            Text(
                "View Territories",
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search", color = Color.LightGray) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8E5DFF),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF8E5DFF)
                    )
                )

                // Scrollable list
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "You" item - always first
                    if (showSelf) {
                        item {
                            KingdomListItem(
                                userName = currentUserName,
                                subtitle = "(You)",
                                color = Color(0xFF00AA00), // Green for own
                                onClick = { onSelectPerson(currentUserId, currentUserName) }
                            )
                        }
                    }

                    // Friends
                    items(filteredFriends) { friend ->
                        KingdomListItem(
                            userName = friend.userName,
                            subtitle = "${friend.firstName} ${friend.lastName}",
                            color = Color(0xFF8E5DFF), // Purple for friends
                            onClick = { onSelectPerson(friend.userId, friend.userName) }
                        )
                    }

                    // Empty state
                    if (filteredFriends.isEmpty() && !showSelf) {
                        item {
                            Text(
                                "No results found",
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    if (friendsList.isEmpty()) {
                        item {
                            Text(
                                "Add friends to view their kingdoms!",
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF8E5DFF))
            }
        }
    )
}

@Composable
fun KingdomListItem(
    userName: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2C53)),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = color,
                modifier = Modifier.size(12.dp)
            ) {}

            Spacer(modifier = Modifier.width(12.dp))

            // Name info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            // Arrow indicator
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "View Kingdom",
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

