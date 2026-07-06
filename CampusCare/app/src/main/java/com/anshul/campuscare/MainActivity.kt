package com.anshul.campuscare

// ──────────────────────────────────────────────
// Main Activity
//
// The single entry point of the app. Sets up the
// Compose theme and launches the navigation graph.
// All screen routing is handled by AppNavigation.
// ──────────────────────────────────────────────

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.anshul.campuscare.ui.navigation.AppNavigation
import com.anshul.campuscare.ui.theme.CampusCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusCareTheme {
                AppNavigation()
            }
        }
    }
}