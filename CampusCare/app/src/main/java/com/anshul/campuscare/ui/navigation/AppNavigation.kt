package com.anshul.campuscare.ui.navigation

// ──────────────────────────────────────────────
// App Navigation
//
// Defines all the routes (screens) in the app and
// how to navigate between them using Jetpack
// Navigation Compose.
//
// Routes:
//   "login"          → Login screen
//   "home"           → Home screen (item feed)
//   "detail/{itemId}" → Item detail screen
//   "create"         → Create new item
//   "edit/{itemId}"  → Edit existing item
//   "search"         → Text search screen
// ──────────────────────────────────────────────

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anshul.campuscare.data.model.User
import com.anshul.campuscare.data.repository.ItemRepository
import com.anshul.campuscare.ui.screens.CreateItemScreen
import com.anshul.campuscare.ui.screens.DetailScreen
import com.anshul.campuscare.ui.screens.HomeScreen
import com.anshul.campuscare.ui.screens.LoginScreen
import com.anshul.campuscare.ui.screens.SearchScreen
import com.anshul.campuscare.ui.theme.TextSecondary

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Check if user is already logged in
    var startDestination: String by remember { mutableStateOf("login") }
    var isCheckingAuth: Boolean by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result: Result<User?> = ItemRepository.getCurrentUser()
        if (result.isSuccess && result.getOrNull() != null) {
            startDestination = "home"
        }
        isCheckingAuth = false
    }

    // Show a splash screen while checking auth
    // instead of a blank white screen
    if (isCheckingAuth) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🔍",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
            Text(
                text = "CampusCare",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            Text(
                text = "Lost & Found",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(height = 32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(size = 32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ── Login Screen ──────────────────────
        composable(route = "login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(route = "home") {
                        // Remove login from back stack so user can't go back to it
                        popUpTo(route = "login") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // ── Home Screen ───────────────────────
        composable(route = "home") {
            HomeScreen(
                onNavigateToDetail = { itemId: Int ->
                    navController.navigate(route = "detail/$itemId")
                },
                onNavigateToCreate = {
                    navController.navigate(route = "create")
                },
                onNavigateToSearch = {
                    navController.navigate(route = "search")
                },
                onLogout = {
                    navController.navigate(route = "login") {
                        popUpTo(route = "home") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // ── Detail Screen ─────────────────────
        composable(
            route = "detail/{itemId}",
            arguments = listOf(
                navArgument(name = "itemId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val itemId: Int = backStackEntry.arguments?.getInt("itemId") ?: 0
            DetailScreen(
                itemId = itemId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { editItemId: Int ->
                    navController.navigate(route = "edit/$editItemId")
                }
            )
        }

        // ── Create Item Screen ────────────────
        composable(route = "create") {
            CreateItemScreen(
                editItemId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Edit Item Screen ──────────────────
        composable(
            route = "edit/{itemId}",
            arguments = listOf(
                navArgument(name = "itemId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val itemId: Int = backStackEntry.arguments?.getInt("itemId") ?: 0
            CreateItemScreen(
                editItemId = itemId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Search Screen ─────────────────────
        composable(route = "search") {
            SearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { itemId: Int ->
                    navController.navigate(route = "detail/$itemId")
                }
            )
        }
    }
}
