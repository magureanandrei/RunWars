package tech.titans.runwars

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
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
import tech.titans.runwars.models.User
import tech.titans.runwars.repo.UserRepo

@Composable
fun ProfileScreen(
    navController: NavController,
    userId: String? = null
) {
    val currentUserId = userId ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isOwnProfile = userId == null || userId == FirebaseAuth.getInstance().currentUser?.uid

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        UserRepo.getUser(currentUserId) { fetchedUser, _ ->
            user = fetchedUser
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C1E3C))
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp) // Extra bottom for OneUI 7.0
            .verticalScroll(rememberScrollState())
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
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = if (isOwnProfile) "My Profile" else "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF8E5DFF))
            }
        } else if (user != null) {
            // Profile avatar - more compact
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally),
                shape = MaterialTheme.shapes.medium,
                color = Color(0xFF8E5DFF)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Username
            Text(
                text = user!!.userName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Info cards - more compact spacing
            ProfileInfoCard(
                label = "First Name",
                value = user!!.firstName
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileInfoCard(
                label = "Last Name",
                value = user!!.lastName
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileInfoCard(
                label = "Email",
                value = user!!.email,
                icon = Icons.Default.Email
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileInfoCard(
                label = "Total Runs",
                value = user!!.runSessionList.size.toString()
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileInfoCard(
                label = "Friends",
                value = user!!.friendsList.size.toString()
            )
        } else {
            Text(
                text = "User not found",
                color = Color.Red,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ProfileInfoCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2C53)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF8E5DFF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
