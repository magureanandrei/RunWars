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
      //tech.titans.runwars.utils.DataRestorer.executeRestoration()

//      tech.titans.runwars.utils.TerritoryGenerator.createBot(
//          centerLat = 46.7713,
//          centerLng = 23.5899,
//          radiusMeters = 300.0,
//          botName = "Cluj Warlord"
//      )
//
//// 2. Create a "Guardian" in Timisoara (Union Square)
//// Radius 200m
//      tech.titans.runwars.utils.TerritoryGenerator.createBot(
//          centerLat = 45.7580,
//          centerLng = 21.2290,
//          radiusMeters = 200.0,
//          botName = "Timisoara Guardian"
//      )
//
//// 3. Create a small enemy near "Iulius Mall Cluj" for testing
//      tech.titans.runwars.utils.TerritoryGenerator.createBot(
//          centerLat = 46.7712,
//          centerLng = 23.6323,
//          radiusMeters = 150.0,
//          botName = "Mall Rat"
//      )

//      tech.titans.runwars.utils.TerritoryGenerator.createBot(
//          centerLat = 46.7585,
//          centerLng = 23.5446,
//          radiusMeters = 180.0,
//          botName = "River Runner"
//      )
//
//// 2. The "King of the Hill" (Cetatuie)
//// High ground! Hard to physically run this, so it's a prestige target.
//      tech.titans.runwars.utils.TerritoryGenerator.createBot(
//          centerLat = 46.7725,
//          centerLng = 23.5850,
//          radiusMeters = 120.0,
//          botName = "Cetatuie King"
//      )
//
//      // 4. The "Zorilor Sprinter" (Observatorului)
//// A standard residential runner.
//      tech.titans.runwars.utils.TerritoryGenerator.createBot(
//          centerLat = 46.7533,
//          centerLng = 23.5936,
//          radiusMeters = 200.0,
//          botName = "Zorilor Sprinter"
//      )
//
//// 5. The "Base Boss" (Baza Sportiva Gheorgheni)
//// This is a huge territory covering the sports complex. A raid boss.
//      tech.titans.runwars.utils.TerritoryGenerator.createBot(
//          centerLat = 46.7688,
//          centerLng = 23.6395,
//          radiusMeters = 350.0, // Massive!
//          botName = "Gheorgheni Boss"
//      )
//
//// 6. The "Airport Security" (Outskirts)
//// Someone has to guard the entrance to the city.
//      tech.titans.runwars.utils.TerritoryGenerator.createBot(
//          centerLat = 46.7806,
//          centerLng = 23.6766,
//          radiusMeters = 400.0,
//          botName = "Airport Security"
//      )

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
        composable("profile") { ProfileScreen(navController) }
        composable("profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            ProfileScreen(navController, userId)
        }
        composable("runHistory") { RunHistoryScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
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