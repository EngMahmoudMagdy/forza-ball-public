package com.forzaball.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.forzaball.app.ui.theme.ForzaBallTheme
import com.forzaball.app.feature.home.HomeRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForzaBallTheme {
                ForzaBallApp()
            }
        }
    }
}

@Composable
fun ForzaBallApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeRoute()
        }
        // TODO: add auth, news, feed, profile graphs here.
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ForzaBallTheme {
        ForzaBallApp()
    }
}