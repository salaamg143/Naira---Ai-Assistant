package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TranscriptScreen
import com.example.ui.screens.UpdatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.voice.VoiceAssistantManager

class MainActivity : ComponentActivity() {
    private lateinit var assistantManager: VoiceAssistantManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        assistantManager = VoiceAssistantManager(applicationContext, lifecycleScope)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val sharedPrefs = remember { getSharedPreferences("naira_prefs", Context.MODE_PRIVATE) }
                val useAppLock = sharedPrefs.getBoolean("use_app_lock", false)
                val pinCode = sharedPrefs.getString("pin_code", "") ?: ""

                var isUnlocked by remember { mutableStateOf(!useAppLock || pinCode.isEmpty()) }

                if (!isUnlocked) {
                    AppLockScreen(onUnlockSuccess = { isUnlocked = true })
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("home") {
                            HomeScreen(
                                navController = navController,
                                assistantManager = assistantManager
                            )
                        }
                        composable("settings") {
                            SettingsScreen(navController = navController)
                        }
                        composable("updates") {
                            UpdatesScreen(navController = navController)
                        }
                        composable(
                            route = "transcript/{sessionId}",
                            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
                            TranscriptScreen(
                                sessionId = sessionId,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        assistantManager.onDestroy()
    }
}

