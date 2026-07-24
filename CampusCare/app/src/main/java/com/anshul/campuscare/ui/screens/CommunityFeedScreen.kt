package com.anshul.campuscare.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.anshul.campuscare.data.model.Discussion
import com.anshul.campuscare.data.network.ApiClient
import com.anshul.campuscare.data.repository.DiscussionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(
    discussionRepository: DiscussionRepository,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToSearch: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var discussions by remember { mutableStateOf<List<Discussion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Trending")

    fun loadDiscussions() {
        isLoading = true
        coroutineScope.launch {
            val result = if (selectedTab == 0) {
                discussionRepository.getAllDiscussions()
            } else {
                discussionRepository.getTrendingDiscussions()
            }

            result.onSuccess { data ->
                discussions = data
                isLoading = false
            }.onFailure { error ->
                errorMessage = error.message
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedTab) {
        loadDiscussions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Discussion", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                }
            } else if (discussions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No discussions found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(discussions) { discussion ->
                        DiscussionCard(discussion = discussion, onClick = { onNavigateToDetail(discussion.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun DiscussionCard(discussion: Discussion, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Author & Avatar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = discussion.createdBy?.avatarColor ?: "⬛",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = discussion.createdBy?.anonymousUsername ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Text(
                text = discussion.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Location tag if available
            if (!discussion.buildingName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = discussion.buildingName!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Description
            Text(
                text = discussion.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            // Image Preview Thumbnail
            if (discussion.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = ApiClient.BASE_URL + discussion.images[0],
                    contentDescription = "Post Thumbnail",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Footer: Upvotes & Comments Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👍 ${discussion.upvoteCount}  💬 ${discussion.count?.comments ?: 0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "View Discussion",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
