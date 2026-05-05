package com.example.pomodoro2

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pomodoro2.ui.theme.LuminousFocusTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val dataStore = remember { AuraDataStore(this) }
            val isStrictMode by dataStore.isStrictModeFlow.collectAsState(initial = false)
            val currentLanguage by dataStore.languageFlow.collectAsState(initial = "vi")
            
            LaunchedEffect(isStrictMode) {
                if (isStrictMode) {
                    if (!hasUsageStatsPermission()) {
                        Toast.makeText(this@MainActivity, "Vui lòng cấp quyền 'Truy cập dữ liệu sử dụng' để dùng Chế độ nghiêm ngặt 🌿", Toast.LENGTH_LONG).show()
                        requestUsageStatsPermission()
                    } else if (!hasOverlayPermission()) {
                        Toast.makeText(this@MainActivity, "Vui lòng cấp quyền 'Hiển thị trên ứng dụng khác' 🌿", Toast.LENGTH_LONG).show()
                        requestOverlayPermission()
                    }
                }
            }

            val isSystemDark = isSystemInDarkTheme()
            LuminousFocusTheme(darkTheme = isSystemDark) {
                val navController = rememberNavController()
                val sharedPref = getSharedPreferences("AuraFlowData", Context.MODE_PRIVATE)
                val isFirstTime = sharedPref.getBoolean("isFirstTime", true)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "splash") {
                        composable("premium") { 
                            PremiumScreen(
                                currentLanguage = currentLanguage,
                                onBack = { navController.popBackStack() },
                                onUpgradeSuccess = {
                                    Toast.makeText(this@MainActivity, if(currentLanguage=="vi") "Nâng cấp Premium thành công! 🌟" else "Upgraded to Premium! 🌟", Toast.LENGTH_LONG).show()
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("whitelist") { WhitelistScreen(onBack = { navController.popBackStack() }) }
                        composable("shop") { ShopScreen(onBack = { navController.popBackStack() }) } 
                        composable("splash") {
                            SplashScreen(onTimeout = {
                                val destination = when {
                                    Firebase.auth.currentUser != null -> "focus"
                                    isFirstTime -> "onboarding"
                                    else -> "login"
                                }
                                navController.navigate(destination) { popUpTo("splash") { inclusive = true } }
                            })
                        }
                        composable("onboarding") {
                            OnboardingScreen(onFinish = {
                                sharedPref.edit().putBoolean("isFirstTime", false).apply()
                                navController.navigate("login") { popUpTo("onboarding") { inclusive = true } }
                            })
                        }
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { navController.navigate("focus") { popUpTo("login") { inclusive = true } } },
                                onGoToRegister = { navController.navigate("register") }
                            )
                        }
                        composable("register") {
                            RegisterScreen(onRegisterSuccess = { navController.navigate("login") }, onGoToLogin = { navController.popBackStack() })
                        }
                        composable("tasks") {
                            TaskScreen(
                                onNavigateToTasks = { },
                                onNavigateToMotivation = { navController.navigate("motivation") { popUpTo(0) } },
                                onNavigateToFocus = { navController.navigate("focus") { popUpTo(0) } },
                                onNavigateToHistory = { navController.navigate("history") { popUpTo(0) } },
                                onNavigateToSettings = { navController.navigate("settings") { popUpTo(0) } }
                            )
                        }
                        composable("motivation") {
                            GardenScreen(
                                onNavigateToTasks = { navController.navigate("tasks") { popUpTo(0) } },
                                onNavigateToMotivation = { },
                                onNavigateToFocus = { navController.navigate("focus") { popUpTo(0) } },
                                onNavigateToHistory = { navController.navigate("history") { popUpTo(0) } },
                                onNavigateToSettings = { navController.navigate("settings") { popUpTo(0) } },
                                onNavigateToShop = { navController.navigate("shop") } 
                            )
                        }
                        composable("focus") {
                            TimerScreen(
                                onNavigateToTasks = { navController.navigate("tasks") { popUpTo(0) } },
                                onNavigateToMotivation = { navController.navigate("motivation") { popUpTo(0) } },
                                onNavigateToHistory = { navController.navigate("history") { popUpTo(0) } },
                                onNavigateToSettings = { navController.navigate("settings") { popUpTo(0) } },
                                onNavigateToWhitelist = { navController.navigate("whitelist") }
                            )
                        }
                        composable("history") {
                            HistoryScreen(
                                onNavigateToTasks = { navController.navigate("tasks") { popUpTo(0) } },
                                onNavigateToMotivation = { navController.navigate("motivation") { popUpTo(0) } },
                                onNavigateToFocus = { navController.navigate("focus") { popUpTo(0) } },
                                onNavigateToHistory = { },
                                onNavigateToSettings = { navController.navigate("settings") { popUpTo(0) } }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                themeViewModel = themeViewModel,
                                onLogout = { navController.navigate("login") { popUpTo(0) } },
                                onNavigateToTasks = { navController.navigate("tasks") { popUpTo(0) } },
                                onNavigateToMotivation = { navController.navigate("motivation") { popUpTo(0) } },
                                onNavigateToFocus = { navController.navigate("focus") { popUpTo(0) } },
                                onNavigateToHistory = { navController.navigate("history") { popUpTo(0) } },
                                onNavigateToSettings = { },
                                onNavigateToPremium = { navController.navigate("premium") },
                                onNavigateToWhitelist = { navController.navigate("whitelist") }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestUsageStatsPermission() {
        try { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (e: Exception) { }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
