package tech.titans.runwars

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C1E3C))
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
                    unfocusedTextColor = Color.White
                )
            )

            // Câmp Last Name
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name", color = Color(0xFFAAAAAA)) },
                singleLine = true,
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
                    unfocusedTextColor = Color.White
                )
            )

            // Câmp User Name
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("User Name", color = Color(0xFFAAAAAA)) },
                singleLine = true,
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
                    unfocusedTextColor = Color.White
                )
            )

            // Câmp Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color(0xFFAAAAAA)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                    unfocusedTextColor = Color.White
                )
            )

            // Câmp Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color(0xFFAAAAAA)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                    unfocusedTextColor = Color.White
                )
            )

            // Buton Register
            Button(
                onClick = { viewModel.signUp(firstName, lastName, userName, email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Register",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Text de revenire la Login
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "Already have an account? Login",
                    color = Color(0xFF8E5DFF),
                    fontSize = 14.sp,
                    softWrap = true
                )
            }
        }
    }
}
