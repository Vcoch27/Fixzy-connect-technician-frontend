package com.example.fixzy_ketnoikythuatvien

import androidx.compose.runtime.Composable
import android.app.Activity
import android.content.Intent
import android.os.Build
import com.google.firebase.database.FirebaseDatabase
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.fixzy_ketnoikythuatvien.ui.theme.AppTheme
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager
import com.example.fixzy_ketnoikythuatvien.ui.navigation.AppNavigation
import com.example.fixzy_ketnoikythuatvien.utils.NotificationHelper

import com.google.accompanist.systemuicontroller.rememberSystemUiController


//ẩn thanh trạng thái và điều hướng
@Composable
fun HideSystemUI() {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        systemUiController.isSystemBarsVisible = false 
    }
}
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavController

    private fun testFirebaseConnection() {
        val db = FirebaseDatabase.getInstance()
        val ref = db.getReference("test_connection")
        ref.setValue("Hello from Fixzy App!")
            .addOnSuccessListener {
                Log.d("FIREBASE_TEST", "✅ Gửi dữ liệu thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE_TEST", "❌ Lỗi kết nối Firebase", e)
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        val config = mapOf(
            "cloud_name" to "dlkrskgwq",
            "api_key" to "249495825631952",
            "api_secret" to "OEdRpYtD-ZpeCervTbnsHclA5QE"
        )
        MediaManager.init(this, config)

        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                HideSystemUI()
                navController = rememberNavController()
                AppNavigation()
                handleDeepLink(intent, navController)
            }
        }

        testFirebaseConnection()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent, navController)
    }

    private fun handleDeepLink(intent: Intent?, navController: NavController) {
        Log.d("DeepLink", "handleDeepLink called with intent: $intent")

        intent?.data?.let { uri ->
            Log.d("DeepLink", "Received URI: $uri")

            if (uri.scheme == "fixzy" && uri.host == "payment-callback") {
                Log.d("DeepLink", "Matched fixzy://payment-callback")

                try {
                    val currentDestinationId = navController.currentDestination?.id
                    Log.d("DeepLink", "Current destination ID: $currentDestinationId")

                    if (currentDestinationId != null) {
                        val popped = navController.popBackStack(currentDestinationId, true)
                        Log.d("DeepLink", "PopBackStack result: $popped")
                    }

                    navController.navigate("home_page") {
                        launchSingleTop = true
                    }

                    Log.d("DeepLink", "Navigated to home_page")
                } catch (e: Exception) {
                    Log.e("DeepLink", "Navigation error: ${e.message}", e)
                }
            } else {
                Log.d("DeepLink", "URI does not match expected scheme/host")
            }
        } ?: Log.d("DeepLink", "Intent data is null")
    }
}
