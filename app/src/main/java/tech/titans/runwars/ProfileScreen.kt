package tech.titans.runwars

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import tech.titans.runwars.repo.UserRepo

@Composable
fun ProfileScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var firstName by remember { mutableStateOf<String?>(null) }
    var lastName by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var runsCompleted by remember { mutableStateOf(0) }
    var territoriesOwned by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser == null) {
            error = "Kein Nutzer angemeldet."
            isLoading = false
            return@LaunchedEffect
        }

        UserRepo.getUser(currentUser.uid) { user, errorMsg ->
            if (user != null) {
                firstName = user.firstName
                lastName = user.lastName
                email = user.email
                runsCompleted = user.runSessionList.size
                territoriesOwned = user.territoryList.size
            } else {
                error = errorMsg ?: "Fehler beim Laden der Profildaten"
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C1E3C))
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF8E5DFF)
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fehler: $error",
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header with back button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp, top = 8.dp)
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zurück",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Profil",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Profile info cards
                    ProfileInfoCard("First Name", firstName ?: "")
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileInfoCard("Last Name", lastName ?: "")
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileInfoCard("E-Mail", email ?: "")
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileInfoCard("Runs Completed", runsCompleted.toString())
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileInfoCard("Territories Owned", territoriesOwned.toString())
                }
            }
        }
    }
}

@Composable
fun ProfileInfoCard(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2C53)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label,
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
