package com.anshul.campuscare.ui.screens

// ──────────────────────────────────────────────
// Detail Screen
//
// Shows full details of a single lost or found item:
//   - Image carousel (swipe through images)
//   - Title, description, category, location, date
//   - Reporter info (name, email)
//   - Status chip
//   - Owner actions: Edit, Delete, Mark as Claimed
// ──────────────────────────────────────────────

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import coil3.compose.AsyncImage
import com.anshul.campuscare.data.model.Item
import com.anshul.campuscare.data.model.SearchMatch
import com.anshul.campuscare.data.model.User
import com.anshul.campuscare.data.network.ApiClient
import com.anshul.campuscare.data.repository.ItemRepository
import com.anshul.campuscare.ui.components.StatusChip
import com.anshul.campuscare.ui.theme.FoundColor
import com.anshul.campuscare.ui.theme.LostColor
import com.anshul.campuscare.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    // ── State ─────────────────────────────────
    var item: Item? by remember { mutableStateOf(null) }
    var currentUser: User? by remember { mutableStateOf(null) }
    var similarItems: List<SearchMatch> by remember { mutableStateOf(emptyList()) }
    var isLoading: Boolean by remember { mutableStateOf(true) }
    var errorMessage: String? by remember { mutableStateOf(null) }
    var showDeleteDialog: Boolean by remember { mutableStateOf(false) }
    var isDeleting: Boolean by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // ── Load Item and User ────────────────────
    LaunchedEffect(itemId) {
        isLoading = true

        // Load item
        val itemResult: Result<Item> = ItemRepository.getItemById(itemId = itemId)
        if (itemResult.isSuccess) {
            item = itemResult.getOrNull()
        } else {
            errorMessage = itemResult.exceptionOrNull()?.message ?: "Failed to load item"
        }

        // Load current user
        val userResult: Result<User?> = ItemRepository.getCurrentUser()
        if (userResult.isSuccess) {
            currentUser = userResult.getOrNull()
        }

        // Load similar items
        val similarResult = ItemRepository.getSimilarItems(itemId = itemId)
        if (similarResult.isSuccess) {
            similarItems = similarResult.getOrNull() ?: emptyList()
        }

        isLoading = false
    }

    // ── Delete Confirmation Dialog ────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Delete Item") },
            text = { Text(text = "Are you sure you want to delete this item? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        isDeleting = true
                        coroutineScope.launch {
                            val deleteResult: Result<String> = ItemRepository.deleteItem(itemId = itemId)
                            if (deleteResult.isSuccess) {
                                onNavigateBack()
                            } else {
                                errorMessage = deleteResult.exceptionOrNull()?.message
                            }
                            isDeleting = false
                        }
                    }
                ) {
                    Text(text = "Delete", color = LostColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // ── UI ────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Item Details") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Text(text = "←", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Something went wrong",
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item != null -> {
                val currentItem: Item = item!!
                val isOwner: Boolean = (currentUser != null && currentUser!!.id == currentItem.userId)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = innerPadding)
                        .verticalScroll(state = rememberScrollState())
                ) {
                    // ── Image Carousel ────────
                    if (currentItem.images.isNotEmpty()) {
                        val pagerState = rememberPagerState(
                            initialPage = 0,
                            pageCount = { currentItem.images.size }
                        )

                        Box {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(height = 280.dp)
                            ) { pageIndex: Int ->
                                val imageUrl: String = ApiClient.BASE_URL + currentItem.images[pageIndex]
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Image ${pageIndex + 1} of ${currentItem.title}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Page indicator dots
                            if (currentItem.images.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(alignment = Alignment.BottomCenter)
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    for (dotIndex: Int in currentItem.images.indices) {
                                        val isActive: Boolean = (dotIndex == pagerState.currentPage)
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .size(size = if (isActive) 8.dp else 6.dp)
                                                .clip(shape = CircleShape)
                                                .background(
                                                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // No image placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height = 200.dp)
                                .background(color = MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📦", fontSize = 64.sp)
                        }
                    }

                    // ── Item Info ─────────────
                    Column(
                        modifier = Modifier.padding(all = 20.dp)
                    ) {
                        // Status chip and date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusChip(status = currentItem.status)
                            Text(
                                text = currentItem.dateLostOrFound.take(n = 10),
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(height = 16.dp))

                        // Title
                        Text(
                            text = currentItem.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(height = 12.dp))

                        // Description
                        Text(
                            text = currentItem.description,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(height = 20.dp))

                        // Category
                        DetailInfoRow(
                            icon = "🏷️",
                            label = "Category",
                            value = currentItem.category
                        )

                        Spacer(modifier = Modifier.height(height = 12.dp))

                        // Location
                        DetailInfoRow(
                            icon = "📍",
                            label = "Location",
                            value = currentItem.location
                        )

                        Spacer(modifier = Modifier.height(height = 12.dp))

                        // Reporter
                        if (currentItem.user != null) {
                            DetailInfoRow(
                                icon = "👤",
                                label = "Reported by",
                                value = "${currentItem.user.name} (${currentItem.user.email})"
                            )
                        }

                        // ── Owner Actions ─────
                        if (isOwner) {
                            Spacer(modifier = Modifier.height(height = 24.dp))

                            Text(
                                text = "Your Actions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(height = 12.dp))

                            // Edit button
                            Button(
                                onClick = { onNavigateToEdit(itemId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(size = 12.dp)
                            ) {
                                Text(text = "✏️ Edit Item")
                            }

                            Spacer(modifier = Modifier.height(height = 8.dp))

                            // Mark as Claimed button (only if not already claimed)
                            if (currentItem.status != "CLAIMED") {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val result: Result<Item> = ItemRepository.markAsClaimed(itemId = itemId)
                                            if (result.isSuccess) {
                                                item = result.getOrNull()
                                            } else {
                                                errorMessage = result.exceptionOrNull()?.message
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(size = 12.dp)
                                ) {
                                    Text(text = "✅ Mark as Claimed")
                                }

                                Spacer(modifier = Modifier.height(height = 8.dp))
                            }

                            // Delete button
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(size = 12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = LostColor
                                ),
                                enabled = !isDeleting
                            ) {
                                if (isDeleting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(size = 20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(text = "🗑️ Delete Item")
                                }
                            }
                        }

                        // ── Similar Items ─────
                        if (similarItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(height = 24.dp))

                            Text(
                                text = "Similar Items",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            
                            Spacer(modifier = Modifier.height(height = 12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(similarItems) { match ->
                                    Card(
                                        modifier = Modifier
                                            .width(160.dp)
                                            .clickable { onNavigateToDetail(match.itemId) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            StatusChip(status = match.status)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = match.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val percent = (match.similarity * 100).toInt()
                                            Text(
                                                text = "$percent% Match",
                                                fontSize = 12.sp,
                                                color = if (percent > 70) FoundColor else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(height = 32.dp))
                    }
                }
            }
        }
    }
}

/**
 * A row showing an icon, label, and value.
 * Used for category, location, reporter info, etc.
 */
@Composable
fun DetailInfoRow(
    icon: String,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(width = 8.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
