package tech.titans.runwars

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.titans.runwars.views.SignUpViewModel

@Composable
fun RegisterScreen(navController: androidx.navigation.NavController,
                   viewModel: SignUpViewModel = viewModel()) {

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Extract register logic into a function for reuse
    val performRegister: () -> Unit = {
        if(firstName.isEmpty() || lastName.isEmpty() || userName.isEmpty() || email.isEmpty() || password.isEmpty()){
            errorMessage = "All fields are required"
        } else {
            errorMessage = null
            isLoading = true
            viewModel.signUp(firstName, lastName, userName, email, password, { success, error ->
                isLoading = false
                if(success){
                    navController.navigate("home")
                }
                else{
                    errorMessage = error ?: "Sign Up failed"
                }
            })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C1E3C))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            // Titlul aplicației
            Text(
                text = "Register",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // Câmp First Name
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name", color = Color(0xFFAAAAAA)) },
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF8E5DFF),
                    unfocusedBorderColor = Color(0xFF8E5DFF),
                    cursorColor = Color.White,
                    focusedLabelColor = Color(0xFF8E5DFF),
                    unfocusedLabelColor = Color(0xFFAAAAAA),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledBorderColor = Color(0xFF8E5DFF).copy(alpha = 0.5f),
                    disabledLabelColor = Color(0xFFAAAAAA).copy(alpha = 0.5f),
                    disabledTextColor = Color.White.copy(alpha = 0.5f)
                )
            )

            // Câmp Last Name
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name", color = Color(0xFFAAAAAA)) },
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF8E5DFF),
                    unfocusedBorderColor = Color(0xFF8E5DFF),
                    cursorColor = Color.White,
                    focusedLabelColor = Color(0xFF8E5DFF),
                    unfocusedLabelColor = Color(0xFFAAAAAA),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledBorderColor = Color(0xFF8E5DFF).copy(alpha = 0.5f),
                    disabledLabelColor = Color(0xFFAAAAAA).copy(alpha = 0.5f),
                    disabledTextColor = Color.White.copy(alpha = 0.5f)
                )
            )

            // Câmp User Name
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("User Name", color = Color(0xFFAAAAAA)) },
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF8E5DFF),
                    unfocusedBorderColor = Color(0xFF8E5DFF),
                    cursorColor = Color.White,
                    focusedLabelColor = Color(0xFF8E5DFF),
                    unfocusedLabelColor = Color(0xFFAAAAAA),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledBorderColor = Color(0xFF8E5DFF).copy(alpha = 0.5f),
                    disabledLabelColor = Color(0xFFAAAAAA).copy(alpha = 0.5f),
                    disabledTextColor = Color.White.copy(alpha = 0.5f)
                )
            )

            // Câmp Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color(0xFFAAAAAA)) },
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF8E5DFF),
                    unfocusedBorderColor = Color(0xFF8E5DFF),
                    cursorColor = Color.White,
                    focusedLabelColor = Color(0xFF8E5DFF),
                    unfocusedLabelColor = Color(0xFFAAAAAA),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledBorderColor = Color(0xFF8E5DFF).copy(alpha = 0.5f),
                    disabledLabelColor = Color(0xFFAAAAAA).copy(alpha = 0.5f),
                    disabledTextColor = Color.White.copy(alpha = 0.5f)
                )
            )

            // Câmp Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color(0xFFAAAAAA)) },
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { performRegister() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF8E5DFF),
                    unfocusedBorderColor = Color(0xFF8E5DFF),
                    cursorColor = Color.White,
                    focusedLabelColor = Color(0xFF8E5DFF),
                    unfocusedLabelColor = Color(0xFFAAAAAA),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledBorderColor = Color(0xFF8E5DFF).copy(alpha = 0.5f),
                    disabledLabelColor = Color(0xFFAAAAAA).copy(alpha = 0.5f),
                    disabledTextColor = Color.White.copy(alpha = 0.5f)
                )
            )

            // Buton Register
            Button(
                onClick = performRegister,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Black.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Register",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if(!errorMessage.isNullOrEmpty()){
                Spacer(Modifier.height(8.dp))
                Text(text = errorMessage ?: "", color = Color.Red)
            }

            // Text de revenire la Login
            TextButton(
                onClick = { navController.popBackStack() },
                enabled = !isLoading,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "Already have an account? Login",
                    color = if (isLoading) Color(0xFF8E5DFF).copy(alpha = 0.5f) else Color(0xFF8E5DFF),
                    fontSize = 14.sp,
                    softWrap = true
                )
            }
        }
    }
}
