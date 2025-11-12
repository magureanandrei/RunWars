package tech.titans.runwars

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    var currentLocation by remember { mutableStateOf(LatLng(46.7712, 23.6236)) }

    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(currentLocation, 14f)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Request location permission
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
                            // Animate camera to new location
                            cameraPositionState.position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(newLocation, 15f)
                        }
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Drawer with sidebar menu
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

                // Menu items
                NavigationDrawerItem(
                    label = { Text("Profile", color = Color.White) },
                    selected = false,
                    onClick = { /* TODO */ },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = Color(0xFF2D3E6F)
                    )
                )

                NavigationDrawerItem(
                    label = { Text("Statistics", color = Color.White) },
                    selected = false,
                    onClick = { /* TODO */ },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = Color(0xFF2D3E6F)
                    )
                )

                NavigationDrawerItem(
                    label = { Text("Leaderboard", color = Color.White) },
                    selected = false,
                    onClick = { /* TODO */ },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = Color(0xFF2D3E6F)
                    )
                )

                NavigationDrawerItem(
                    label = { Text("Settings", color = Color.White) },
                    selected = false,
                    onClick = { /* TODO */ },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = Color(0xFF2D3E6F)
                    )
                )
            }
        }
    ) {
        // Whole layout
        Box(modifier = Modifier.fillMaxSize()) {

            // 1️⃣ Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(state = MarkerState(position = currentLocation), title = "You are here")
            }

        // 2️⃣ Top bar with hamburger menu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopEnd)
        ) {
            Surface(
                onClick = {
                    scope.launch {
                        if (drawerState.isOpen) {
                            drawerState.close()
                        } else {
                            drawerState.open()
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFF1E2A47),
                shadowElevation = 4.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // 3️⃣ Bottom bar with button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xFF1E2A47))
                .padding(vertical = 12.dp)
        ) {
            Button(
                onClick = { /* TODO: start run logic */ },
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(180.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3E6F))
            ) {
                Text("Start Run", color = Color.White, fontSize = 18.sp)
            }
        }
    }
    }
}
