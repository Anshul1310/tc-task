package com.anshul.campuscare

// ──────────────────────────────────────────────
// Main Activity
//
// The single entry point of the app. Sets up the
// Compose theme and launches the navigation graph.
//
// IMPORTANT: Calls ApiClient.initialize() to load
// saved cookies from SharedPreferences before any
// API calls are made.
// ──────────────────────────────────────────────

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.anshul.campuscare.data.network.ApiClient
import com.anshul.campuscare.ui.navigation.AppNavigation
import com.anshul.campuscare.ui.theme.CampusCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved session cookies from SharedPreferences.
        // This must happen before any API calls so that
        // the auth check in AppNavigation can use them.
        ApiClient.initialize(context = this)

        enableEdgeToEdge()
        setContent {
            CampusCareTheme {
                AppNavigation()
            }
        }
    }
}