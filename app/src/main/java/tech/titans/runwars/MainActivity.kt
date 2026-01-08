package tech.titans.runwars

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import tech.titans.runwars.ui.theme.RunWarsTheme

class MainActivity : ComponentActivity() {
  /*  override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RunWarsTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        BottomAppBar(modifier = Modifier.height(80.dp),
                            containerColor = Color(0xFF272244)
                        )
                        {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                                )
                                {
                                    RunButton()
                                }
                        }
                    }
                    )
                { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                    MapImage()
                    PersonIcon()
                    MenuButton()
                }
            }
        }
    }*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Disable edge-to-edge to prevent transparent system bars on Android 14+
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            RunWarsTheme {
                Navigation()
            }
        }
    }
}

@Composable
fun Navigation(){
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("RunWarsPrefs", Context.MODE_PRIVATE)
    val stayLoggedIn = prefs.getBoolean("stayLoggedIn", false)
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Determine start destination based on auth state
    val startDestination = if (currentUser != null && stayLoggedIn) {
        "home"
    } else {
        if (currentUser != null && !stayLoggedIn) {
            // User is logged in but didn't check "stay logged in", so log them out
            FirebaseAuth.getInstance().signOut()
        }
        "login"
    }

    NavHost(navController = navController, startDestination = startDestination){
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("home") { HomeScreen(navController) }
        composable("friends") { FriendsListScreen(navController) }
    }
}

@Preview(showBackground = true)
@Composable
fun MapImage(){
    Image(
        painter = painterResource(id = R.drawable.map),
        contentDescription = "Map",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
    )
}

@Composable
fun MenuButton(){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 15.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.End
    )
    {
        Button(onClick = {print("Clicked")}){
            Image(
                painter = painterResource(id = R.drawable.outline_dehaze_24),
                contentDescription = "Menu Button"
            )
        }
    }
}

@Composable
fun PersonIcon(){
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.baseline_circle_24),
            contentDescription = "Person Icon",
            colorFilter = ColorFilter.tint(Color.Blue)
        )
    }
}

@Composable
fun RunButton(){
    Button(onClick = {print("Clicked")}){
        Text("Start Run")
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RunWarsTheme {
        Greeting("Android")
    }
}