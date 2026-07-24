package com.anshul.campuscare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.anshul.campuscare.data.model.Comment
import com.anshul.campuscare.data.model.Discussion
import com.anshul.campuscare.data.network.ApiClient
import com.anshul.campuscare.data.repository.DiscussionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionDetailScreen(
    discussionId: Int,
    discussionRepository: DiscussionRepository,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var discussion by remember { mutableStateOf<Discussion?>(null) }
    var relatedDiscussions by remember { mutableStateOf<List<Discussion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var newCommentText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }

    fun loadDiscussion() {
        isLoading = true
        coroutineScope.launch {
            val result = discussionRepository.getDiscussionById(discussionId)
            result.onSuccess { data ->
                discussion = data.discussion
                relatedDiscussions = data.relatedDiscussions
                isLoading = false
            }.onFailure { error ->
                errorMessage = error.message
                isLoading = false
            }
        }
    }

    LaunchedEffect(discussionId) {
        loadDiscussion()
    }

    fun toggleUpvote() {
        discussion?.let { current ->
            val newHasUpvoted = !current.hasUpvoted
            val newCount = if (newHasUpvoted) current.upvoteCount + 1 else current.upvoteCount - 1
            discussion = current.copy(hasUpvoted = newHasUpvoted, upvoteCount = newCount)

            coroutineScope.launch {
                discussionRepository.toggleUpvote(discussionId)
            }
        }
    }

    fun postComment() {
        if (newCommentText.isBlank()) return
        isPosting = true
        coroutineScope.launch {
            val result = discussionRepository.addComment(discussionId, newCommentText)
            isPosting = false
            result.onSuccess {
                newCommentText = ""
                loadDiscussion()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a comment...") },
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { postComment() }, enabled = !isPosting && newCommentText.isNotBlank()) {
                        Text("Post")
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            }
        } else if (discussion != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = discussion!!.createdBy?.avatarColor ?: "⬛",
                            modifier = Modifier.padding(end = 8.dp),
                            fontSize = 18.sp
                        )
                        Text(
                            text = discussion!!.createdBy?.anonymousUsername ?: "Unknown",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = discussion!!.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (!discussion!!.buildingName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = discussion!!.buildingName!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = discussion!!.description,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (discussion!!.images.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(discussion!!.images) { imagePath ->
                                val imageUrl = ApiClient.BASE_URL + imagePath
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Discussion photo",
                                        modifier = Modifier
                                            .width(260.dp)
                                            .height(200.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { toggleUpvote() }) {
                            Icon(
                                imageVector = if (discussion!!.hasUpvoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Upvote",
                                tint = if (discussion!!.hasUpvoted) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        Text("${discussion!!.upvoteCount} Upvotes", fontWeight = FontWeight.SemiBold)
                    }

                    if (relatedDiscussions.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text(
                            text = "💡 Related Discussions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(relatedDiscussions) { related ->
                                Card(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clickable { onNavigateToDetail(related.id) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = related.createdBy?.avatarColor ?: "⬛",
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            Text(
                                                text = related.createdBy?.anonymousUsername ?: "Unknown",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = related.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "👍 ${related.upvoteCount}  💬 ${related.count?.comments ?: 0}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Text("Comments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(discussion!!.comments ?: emptyList()) { comment ->
                    CommentItem(comment)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author?.avatarColor ?: "⬛",
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = comment.author?.anonymousUsername ?: "Unknown",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
