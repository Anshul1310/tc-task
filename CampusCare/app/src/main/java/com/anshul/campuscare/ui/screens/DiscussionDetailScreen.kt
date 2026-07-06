package com.anshul.campuscare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anshul.campuscare.data.model.Comment
import com.anshul.campuscare.data.model.Discussion
import com.anshul.campuscare.data.repository.DiscussionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionDetailScreen(
    discussionId: Int,
    discussionRepository: DiscussionRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var discussion by remember { mutableStateOf<Discussion?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var newCommentText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }

    fun loadDiscussion() {
        isLoading = true
        coroutineScope.launch {
            val result = discussionRepository.getDiscussionById(discussionId)
            result.onSuccess { data ->
                discussion = data
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
            // Optimistic update
            val newHasUpvoted = !current.hasUpvoted
            val newCount = if (newHasUpvoted) current.upvoteCount + 1 else current.upvoteCount - 1
            discussion = current.copy(hasUpvoted = newHasUpvoted, upvoteCount = newCount)
            
            coroutineScope.launch {
                discussionRepository.toggleUpvote(discussionId)
                // In a real app, handle failure and rollback
            }
        }
    }

    fun postComment() {
        if (newCommentText.isBlank()) return
        isPosting = true
        coroutineScope.launch {
            val result = discussionRepository.addComment(discussionId, newCommentText, null)
            isPosting = false
            result.onSuccess { 
                newCommentText = ""
                loadDiscussion() // Reload to get the new comment
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
                // Post Body
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = discussion!!.createdBy?.avatarColor ?: "⬛",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = discussion!!.createdBy?.anonymousUsername ?: "Unknown",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = discussion!!.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = discussion!!.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { toggleUpvote() }) {
                            Icon(
                                imageVector = if (discussion!!.hasUpvoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Upvote",
                                tint = if (discussion!!.hasUpvoted) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        Text("${discussion!!.upvoteCount} Upvotes")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Comments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Comments
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
            
            // Replies (Nested)
            if (!comment.replies.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    comment.replies.forEach { reply ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = reply.author?.avatarColor ?: "⬛",
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "${reply.author?.anonymousUsername ?: "Unknown"}: ",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = reply.text,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
