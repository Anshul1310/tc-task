package com.anshul.campuscare.ui.screens

// ──────────────────────────────────────────────
// Home Screen
//
// The main screen of the app. Shows a list of all
// lost and found items with tabs to filter by status.
//
// Features:
//   - Tab bar: All / Lost / Found
//   - List of ItemCard composables
//   - Floating action button to create a new item
//   - Search icon in the top bar
//   - Pull-to-refresh to reload items
//   - Logout button
// ──────────────────────────────────────────────

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.campuscare.data.model.Item
import com.anshul.campuscare.data.model.User
import com.anshul.campuscare.data.repository.ItemRepository
import com.anshul.campuscare.ui.components.ItemCard
import com.anshul.campuscare.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onLogout: () -> Unit
) {
    // ── State ─────────────────────────────────
    var itemList: List<Item> by remember { mutableStateOf(emptyList()) }
    var isLoading: Boolean by remember { mutableStateOf(true) }
    var errorMessage: String? by remember { mutableStateOf(null) }
    var selectedTabIndex: Int by remember { mutableIntStateOf(0) }
    var currentUser: User? by remember { mutableStateOf(null) }

    val coroutineScope = rememberCoroutineScope()

    // Tab options: index 0 = All, 1 = Lost, 2 = Found
    val tabLabels: List<String> = listOf("All", "Lost", "Found")

    // ── Load User Info ────────────────────────
    LaunchedEffect(Unit) {
        val userResult: Result<User?> = ItemRepository.getCurrentUser()
        if (userResult.isSuccess) {
            currentUser = userResult.getOrNull()
        }
    }

    // ── Load Items When Tab Changes ───────────
    LaunchedEffect(selectedTabIndex) {
        isLoading = true
        errorMessage = null

        val statusFilter: String? = when (selectedTabIndex) {
            1 -> "LOST"
            2 -> "FOUND"
            else -> null
        }

        val result: Result<List<Item>> = ItemRepository.getAllItems(status = statusFilter)

        if (result.isSuccess) {
            itemList = result.getOrDefault(emptyList())
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to load items"
        }

        isLoading = false
    }

    // Function to refresh items
    fun refreshItems() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null

            val statusFilter: String? = when (selectedTabIndex) {
                1 -> "LOST"
                2 -> "FOUND"
                else -> null
            }

            val result: Result<List<Item>> = ItemRepository.getAllItems(status = statusFilter)

            if (result.isSuccess) {
                itemList = result.getOrDefault(emptyList())
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to load items"
            }

            isLoading = false
        }
    }

    // ── UI ────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CampusCare",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (currentUser != null) {
                            Text(
                                text = "Hi, ${currentUser!!.name}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // Search button
                    IconButton(onClick = { onNavigateToSearch() }) {
                        Text(text = "🔍", fontSize = 20.sp)
                    }
                    // Logout button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                ItemRepository.logout()
                                onLogout()
                            }
                        }
                    ) {
                        Text(text = "🚪", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToCreate() },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(size = 16.dp)
            ) {
                Text(
                    text = "＋",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding)
        ) {
            // ── Filter Tabs ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                for (index: Int in tabLabels.indices) {
                    val label: String = tabLabels[index]
                    val isSelected: Boolean = (index == selectedTabIndex)

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(weight = 1f))

                // Refresh button
                IconButton(onClick = { refreshItems() }) {
                    Text(text = "🔄", fontSize = 18.sp)
                }
            }

            // ── Content Area ──────────────────
            when {
                isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(height = 16.dp))
                            Text(
                                text = "Loading items...",
                                color = TextSecondary
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    // Error state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "😕",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(height = 16.dp))
                            Text(
                                text = errorMessage ?: "Something went wrong",
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                itemList.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📭",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(height = 16.dp))
                            Text(
                                text = "No items found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(height = 8.dp))
                            Text(
                                text = "Report a lost or found item\nusing the + button",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    // Item list
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
                    ) {
                        items(
                            items = itemList,
                            key = { item: Item -> item.id }
                        ) { item: Item ->
                            ItemCard(
                                item = item,
                                onItemClick = { itemId: Int ->
                                    onNavigateToDetail(itemId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
