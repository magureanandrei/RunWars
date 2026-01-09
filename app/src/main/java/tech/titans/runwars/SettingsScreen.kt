// SettingsScreen.kt
package tech.titans.runwars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import tech.titans.runwars.models.User
import tech.titans.runwars.repo.UserRepo
import tech.titans.runwars.services.UserService

@Composable
fun SettingsScreen(navController: NavController) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Success/Error messages
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showFirstNameDialog by remember { mutableStateOf(false) }
    var showLastNameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        UserRepo.getUser(userId) { fetchedUser, _ ->
            user = fetchedUser
            isLoading = false
        }
    }

    // Auto-dismiss messages after 3 seconds
    LaunchedEffect(successMessage, errorMessage) {
        if (successMessage != null || errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            successMessage = null
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C1E3C),
                        Color(0xFF1A0F2E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3D2C53))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Account Settings",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (isLoading || user == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF8E5DFF),
                        strokeWidth = 3.dp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Profile Section
                    Text(
                        text = "PROFILE INFORMATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E5DFF),
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp, top = 8.dp)
                    )

                    ModernSettingsCard(
                        icon = Icons.Default.Person,
                        iconColor = Color(0xFF6C63FF),
                        title = "First Name",
                        subtitle = user!!.firstName.ifEmpty { "Not set" },
                        onClick = { showFirstNameDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ModernSettingsCard(
                        icon = Icons.Default.Person,
                        iconColor = Color(0xFF5B8DEE),
                        title = "Last Name",
                        subtitle = user!!.lastName.ifEmpty { "Not set" },
                        onClick = { showLastNameDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ModernSettingsCard(
                        icon = Icons.Default.AccountCircle,
                        iconColor = Color(0xFF00D4AA),
                        title = "Username",
                        subtitle = "@${user!!.userName}",
                        onClick = { showUsernameDialog = true }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Security Section
                    Text(
                        text = "SECURITY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E5DFF),
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )

                    ModernSettingsCard(
                        icon = Icons.Default.Lock,
                        iconColor = Color(0xFFFF6B9D),
                        title = "Change Password",
                        subtitle = "Keep your account secure",
                        onClick = { showPasswordDialog = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Account Info
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF3D2C53).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Color(0xFF8E5DFF),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Email Address",
                                    color = Color.LightGray,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = user!!.email,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Success/Error Messages
        AnimatedVisibility(
            visible = successMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF00C853)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = successMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = errorMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Dialogs
    if (showFirstNameDialog && user != null) {
        ModernEditDialog(
            title = "Update First Name",
            icon = Icons.Default.Person,
            currentValue = user!!.firstName,
            label = "First Name",
            hint = "Enter your first name",
            isPassword = false,
            onDismiss = { showFirstNameDialog = false },
            onSave = { newValue ->
                UserService.updateFirstName(userId, newValue) { success, error ->
                    if (success) {
                        user = user!!.copy(firstName = newValue)
                        showFirstNameDialog = false
                        successMessage = "First name updated successfully!"
                    } else {
                        errorMessage = error ?: "Failed to update first name"
                    }
                }
            }
        )
    }

    if (showLastNameDialog && user != null) {
        ModernEditDialog(
            title = "Update Last Name",
            icon = Icons.Default.Person,
            currentValue = user!!.lastName,
            label = "Last Name",
            hint = "Enter your last name",
            isPassword = false,
            onDismiss = { showLastNameDialog = false },
            onSave = { newValue ->
                UserService.updateLastName(userId, newValue) { success, error ->
                    if (success) {
                        user = user!!.copy(lastName = newValue)
                        showLastNameDialog = false
                        successMessage = "Last name updated successfully!"
                    } else {
                        errorMessage = error ?: "Failed to update last name"
                    }
                }
            }
        )
    }

    if (showUsernameDialog && user != null) {
        ModernEditDialog(
            title = "Update Username",
            icon = Icons.Default.AccountCircle,
            currentValue = user!!.userName,
            label = "Username",
            hint = "3-20 characters, letters, numbers and _",
            isPassword = false,
            onDismiss = { showUsernameDialog = false },
            onSave = { newValue ->
                UserService.updateUsername(userId, newValue) { success, error ->
                    if (success) {
                        user = user!!.copy(userName = newValue)
                        showUsernameDialog = false
                        successMessage = "Username updated successfully!"
                    } else {
                        errorMessage = error ?: "Failed to update username"
                    }
                }
            }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onSave = { currentPassword, newPassword ->
                UserService.updatePassword(currentPassword, newPassword) { success, error ->
                    if (success) {
                        showPasswordDialog = false
                        successMessage = "Password changed successfully!"
                    } else {
                        errorMessage = error ?: "Failed to change password"
                    }
                }
            }
        )
    }
}

@Composable
fun ModernSettingsCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D2C53)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFFB0B0B0),
                    fontSize = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF8E5DFF),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ModernEditDialog(
    title: String,
    icon: ImageVector,
    currentValue: String,
    label: String,
    hint: String,
    isPassword: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF3D2C53),
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF8E5DFF).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF8E5DFF),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label, color = Color.LightGray) },
                    placeholder = { Text(hint, color = Color.Gray) },
                    singleLine = true,
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8E5DFF),
                        unfocusedBorderColor = Color(0xFF5D4E6E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF8E5DFF),
                        focusedLabelColor = Color(0xFF8E5DFF),
                        unfocusedLabelColor = Color.LightGray
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (hint.isNotEmpty() && !isPassword) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = hint,
                        color = Color(0xFFB0B0B0),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (value.isNotBlank()) {
                        onSave(value)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8E5DFF)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Save Changes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = Color(0xFFB0B0B0),
                    fontSize = 15.sp
                )
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF3D2C53),
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFF6B9D).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFFF6B9D),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = "Change Password",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Current Password
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        validationError = null
                    },
                    label = { Text("Current Password", color = Color.LightGray) },
                    singleLine = true,
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                imageVector = if (currentPasswordVisible) Icons.Default.Clear else Icons.Default.Info,
                                contentDescription = "Toggle password visibility",
                                tint = Color.LightGray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8E5DFF),
                        unfocusedBorderColor = Color(0xFF5D4E6E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF8E5DFF),
                        focusedLabelColor = Color(0xFF8E5DFF)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // New Password
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        validationError = null
                    },
                    label = { Text("New Password", color = Color.LightGray) },
                    singleLine = true,
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Default.Clear else Icons.Default.Info,
                                contentDescription = "Toggle password visibility",
                                tint = Color.LightGray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8E5DFF),
                        unfocusedBorderColor = Color(0xFF5D4E6E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF8E5DFF),
                        focusedLabelColor = Color(0xFF8E5DFF)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        validationError = null
                    },
                    label = { Text("Confirm New Password", color = Color.LightGray) },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Clear else Icons.Default.Info,
                                contentDescription = "Toggle password visibility",
                                tint = Color.LightGray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8E5DFF),
                        unfocusedBorderColor = Color(0xFF5D4E6E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF8E5DFF),
                        focusedLabelColor = Color(0xFF8E5DFF)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Requirements
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2C1E3C)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Password must contain:",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        PasswordRequirement("• At least 8 characters", newPassword.length >= 8)
                        PasswordRequirement("• One uppercase letter", newPassword.any { it.isUpperCase() })
                        PasswordRequirement("• One lowercase letter", newPassword.any { it.isLowerCase() })
                        PasswordRequirement("• One number", newPassword.any { it.isDigit() })
                        PasswordRequirement("• One special character", newPassword.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) })
                    }
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF5252).copy(alpha = 0.2f))
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = validationError!!,
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPassword.isEmpty() -> validationError = "Please enter your current password"
                        newPassword.isEmpty() -> validationError = "Please enter a new password"
                        newPassword != confirmPassword -> validationError = "Passwords do not match"
                        else -> onSave(currentPassword, newPassword)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8E5DFF)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Update Password",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = Color(0xFFB0B0B0),
                    fontSize = 15.sp
                )
            }
        }
    )
}

@Composable
fun PasswordRequirement(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Clear,
            contentDescription = null,
            tint = if (isMet) Color(0xFF00D4AA) else Color(0xFF6D6D6D),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (isMet) Color(0xFF00D4AA) else Color(0xFFB0B0B0),
            fontSize = 11.sp
        )
    }
}

