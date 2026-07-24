package com.anshul.campuscare.ui.navigation

// ──────────────────────────────────────────────
// App Navigation
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anshul.campuscare.data.model.User
import com.anshul.campuscare.data.network.ApiClient
import com.anshul.campuscare.data.repository.AuthRepository
import com.anshul.campuscare.data.repository.DiscussionRepository
import com.anshul.campuscare.ui.screens.CommunityFeedScreen
import com.anshul.campuscare.ui.screens.CreateDiscussionScreen
import com.anshul.campuscare.ui.screens.DiscussionDetailScreen
import com.anshul.campuscare.ui.screens.LoginScreen
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
        val hasSession: Boolean = ApiClient.hasSavedSession()

        if (hasSession) {
            val result: Result<User?> = AuthRepository.getCurrentUser()
            if (result.isSuccess && result.getOrNull() != null) {
                startDestination = "home"
            }
        }

        isCheckingAuth = false
    }

    if (isCheckingAuth) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "💬",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CampusCare",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Campus Community",
                fontSize = 14.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
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
                        popUpTo(route = "login") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // ── Home Screen (Community Feed) ──────
        composable(route = "home") {
            CommunityFeedScreen(
                discussionRepository = discussionRepository,
                onNavigateToDetail = { id: Int ->
                    navController.navigate(route = "community/detail/$id")
                },
                onNavigateToCreate = {
                    navController.navigate(route = "community/create")
                },
                onNavigateToSearch = {
                    navController.navigate(route = "community/search")
                }
            )
        }

        // ── RAG Search Screen ─────────────────
        composable(route = "community/search") {
            com.anshul.campuscare.ui.screens.SearchScreen(
                discussionRepository = discussionRepository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { id: Int ->
                    navController.navigate(route = "community/detail/$id")
                }
            )
        }

        // ── Create Discussion Screen ──────────
        composable(route = "community/create") {
            CreateDiscussionScreen(
                discussionRepository = discussionRepository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { id: Int ->
                    navController.navigate(route = "community/detail/$id") {
                        popUpTo("home")
                    }
                }
            )
        }

        // ── Discussion Detail Screen ──────────
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
                },
                onNavigateToDetail = { newId ->
                    navController.navigate("community/detail/$newId")
                }
            )
        }
    }
}
