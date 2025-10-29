package tech.titans.runwars

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.database
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
          val database = Firebase.database("https://runwars-13db0-default-rtdb.europe-west1.firebasedatabase.app/")
          val myRef = database.getReference("message")
      Log.d("FirebaseCheck", "Apps: ${FirebaseApp.getApps(this)}")
          // Write data
          myRef.setValue("Hello, Raul!")
              .addOnSuccessListener {
                  Log.d("Firebase", "Data written successfully!")
              }
              .addOnFailureListener { e ->
                  Log.e("Firebase", "Failed to write data", e)
              }
      Log.d("Firebase", "Writing to: ${database.reference.root}")
      myRef.get().addOnSuccessListener {
          val value = it.value
          Log.d("Firebase", "Value is: $value")
      }
        setContent {
            RunWarsTheme {
                LoginRegisterNavigation()
            }
        }
    }
}

@Composable
fun LoginRegisterNavigation(){
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login"){
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
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