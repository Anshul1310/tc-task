package com.anshul.campuscare


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
        ApiClient.initialize(context = this)

        enableEdgeToEdge()
        setContent {
            CampusCareTheme {
                AppNavigation()
            }
        }
    }
}