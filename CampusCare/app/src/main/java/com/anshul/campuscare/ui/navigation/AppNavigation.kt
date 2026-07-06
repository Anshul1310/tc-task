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
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anshul.campuscare.data.model.User
import com.anshul.campuscare.data.network.ApiClient
import com.anshul.campuscare.data.repository.DiscussionRepository
import com.anshul.campuscare.data.repository.ItemRepository
import com.anshul.campuscare.ui.screens.CommunityFeedScreen
import com.anshul.campuscare.ui.screens.CreateDiscussionScreen
import com.anshul.campuscare.ui.screens.CreateItemScreen
import com.anshul.campuscare.ui.screens.DetailScreen
import com.anshul.campuscare.ui.screens.DiscussionDetailScreen
import com.anshul.campuscare.ui.screens.HomeScreen
import com.anshul.campuscare.ui.screens.LoginScreen
import com.anshul.campuscare.ui.screens.NotificationsScreen
import com.anshul.campuscare.ui.screens.SearchScreen
import com.anshul.campuscare.ui.screens.LoginScreen
import com.anshul.campuscare.ui.screens.SearchScreen
import com.anshul.campuscare.ui.theme.TextSecondary

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Setup User state
    var startDestination: String by remember { mutableStateOf("login") }
    var isCheckingAuth: Boolean by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val discussionRepository = remember { DiscussionRepository(ApiClient.apiService, context) }

    LaunchedEffect(Unit) {
        val hasCookies: Boolean = ApiClient.cookieJar.hasSavedCookies()

        if (hasCookies) {
            // We have saved cookies — verify they are still valid
            // by calling the server
            val result: Result<User?> = ItemRepository.getCurrentUser()
            if (result.isSuccess && result.getOrNull() != null) {
                startDestination = "home"
            }
            // If the check fails (expired session), fall through to "login"
        }
        // If no cookies, skip network call — go straight to login (instant)

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
                onNavigateToCommunity = {
                    navController.navigate(route = "community")
                },
                onNavigateToNotifications = {
                    navController.navigate(route = "notifications")
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
                },
                onNavigateToDetail = { similarItemId: Int ->
                    navController.navigate(route = "detail/$similarItemId")
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
        
        // ── Community ─────────────────────────
        composable(route = "community") {
            CommunityFeedScreen(
                discussionRepository = discussionRepository,
                onNavigateToDetail = { id: Int ->
                    navController.navigate(route = "community/detail/$id")
                },
                onNavigateToCreate = {
                    navController.navigate(route = "community/create")
                }
            )
        }
        
        composable(route = "community/create") {
            CreateDiscussionScreen(
                discussionRepository = discussionRepository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { id: Int ->
                    navController.navigate(route = "community/detail/$id") {
                        popUpTo("community")
                    }
                }
            )
        }
        
        composable(
            route = "community/detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            DiscussionDetailScreen(
                discussionId = id,
                discussionRepository = discussionRepository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // ── Notifications ───────────────────────
        composable(route = "notifications") {
            NotificationsScreen(
                discussionRepository = discussionRepository,
                onNavigateToDetail = { id: Int ->
                    navController.navigate(route = "community/detail/$id")
                }
            )
        }
    }
}
