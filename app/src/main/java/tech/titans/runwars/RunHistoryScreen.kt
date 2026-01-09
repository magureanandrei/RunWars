package tech.titans.runwars

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import tech.titans.runwars.models.RunSession
import tech.titans.runwars.views.RunHistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RunHistoryScreen(
    navController: NavController,
    viewModel: RunHistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val runSessions by viewModel.runSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val totalRuns by viewModel.totalRuns.collectAsState()

    // State for detail modal
    var selectedRun by remember { mutableStateOf<RunSession?>(null) }
    var selectedRunIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C1E3C))
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Run History",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Statistics Summary Card
        StatsCard(
            totalDistance = viewModel.getTotalDistanceKm(),
            totalRuns = totalRuns,
            totalArea = viewModel.getTotalAreaHectares()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF8E5DFF))
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                }
            }
            runSessions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF8E5DFF),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No runs yet",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start your first run to see it here!",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 48.dp)
                ) {
                    itemsIndexed(runSessions) { index, run ->
                        // Run number is reversed since list is newest first
                        val runNumber = totalRuns - index
                        RunHistoryItem(
                            run = run,
                            runNumber = runNumber,
                            onClick = {
                                selectedRun = run
                                selectedRunIndex = runNumber
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Modal
    selectedRun?.let { run ->
        RunDetailModal(
            run = run,
            runNumber = selectedRunIndex,
            onViewTerritory = {
                val prefs = context.getSharedPreferences("RunWarsPrefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("pendingRunTerritoryView", run.runId)
                    .apply()
                selectedRun = null
                navController.popBackStack()
            },
            onDismiss = { selectedRun = null }
        )
    }
}

/**
 * Generates a thematic title for a run based on time of day and success
 */
private fun getRunThematicTitle(run: RunSession): String {
    if (run.startTime <= 0) return "Unknown Mission"
    
    val calendar = Calendar.getInstance().apply { timeInMillis = run.startTime }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    
    val timeOfDay = when (hour) {
        in 5..11 -> "Morning"
        in 12..16 -> "Afternoon"
        in 17..21 -> "Evening"
        else -> "Night"
    }
    
    val action = if (run.hasTerritory()) "Conquest" else "Patrol"
    
    return "$timeOfDay $action"
}

@Composable
fun StatsCard(
    totalDistance: String,
    totalRuns: Int,
    totalArea: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2C53)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Your Stats",
                color = Color(0xFF8E5DFF),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = totalDistance, label = "Distance")
                StatItem(value = totalRuns.toString(), label = "Runs")
                StatItem(value = totalArea, label = "Area")
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}

@Composable
fun RunHistoryItem(
    run: RunSession,
    runNumber: Int,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val dateStr = if (run.startTime > 0) {
        dateFormat.format(Date(run.startTime))
    } else {
        "Unknown date"
    }

    val timeStr = if (run.startTime > 0) {
        timeFormat.format(Date(run.startTime))
    } else {
        ""
    }
    
    val thematicTitle = getRunThematicTitle(run)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2C53)),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Run icon indicator
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (run.hasTerritory()) Color(0xFF4CAF50) else Color(0xFF8E5DFF),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (run.hasTerritory()) Icons.Default.LocationOn else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Run info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = thematicTitle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Run #$runNumber",
                        color = Color(0xFF8E5DFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (timeStr.isNotEmpty()) "$dateStr, $timeStr" else dateStr,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "%.2f km".format(run.getDistanceKm()),
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }

            // Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(rotationZ = 180f)
            )
        }
    }
}

@Composable
fun RunDetailModal(
    run: RunSession,
    runNumber: Int,
    onViewTerritory: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val dateStr = if (run.startTime > 0) dateFormat.format(Date(run.startTime)) else "Unknown"
    val startTimeStr = if (run.startTime > 0) timeFormat.format(Date(run.startTime)) else "--:--"
    val endTimeStr = if (run.stopTime > 0) timeFormat.format(Date(run.stopTime)) else "--:--"
    
    val thematicTitle = getRunThematicTitle(run)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C1E3C),
        title = {
            Column {
                Text(
                    text = thematicTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mission #$runNumber",
                    color = Color(0xFF8E5DFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Date & Time Section
                DetailSection(title = "Date & Time") {
                    DetailRow(label = "Date", value = dateStr)
                    DetailRow(label = "Started", value = startTimeStr)
                    DetailRow(label = "Ended", value = endTimeStr)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Performance Section
                DetailSection(title = "Performance") {
                    DetailRow(label = "Distance", value = "%.2f km".format(run.getDistanceKm()))
                    DetailRow(label = "Duration", value = run.getFormattedDuration())
                    DetailRow(label = "Pace", value = "${run.getPacePerKm()} /km")
                }

                // Territory Section (if applicable)
                if (run.hasTerritory()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailSection(title = "Territory") {
                        DetailRow(label = "Area", value = "%.2f hectares".format(run.getCapturedAreaHectares()))
                        DetailRow(label = "Points", value = "${run.coordinatesList.size}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onViewTerritory,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E5DFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View on Map")
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A3A60))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This run didn't form a closed loop, so no territory was captured.",
                                color = Color.LightGray,
                                fontSize = 12.sp
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
fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            color = Color(0xFF8E5DFF),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2C53))
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}
