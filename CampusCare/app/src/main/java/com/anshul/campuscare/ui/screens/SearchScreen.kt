package com.anshul.campuscare.ui.screens

// ──────────────────────────────────────────────
// Search Screen
//
// Allows users to search for items by text description.
// The backend generates an AI embedding from the query
// and finds similar items using cosine similarity.
//
// Features:
//   - Text search field
//   - Search button
//   - Results list with similarity scores
//   - Tap a result to see full details
// ──────────────────────────────────────────────

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anshul.campuscare.data.model.SearchMatch
import com.anshul.campuscare.data.repository.ItemRepository
import com.anshul.campuscare.ui.components.StatusChip
import com.anshul.campuscare.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit
) {
    // ── State ─────────────────────────────────
    var query: String by remember { mutableStateOf("") }
    var results: List<SearchMatch> by remember { mutableStateOf(emptyList()) }
    var isSearching: Boolean by remember { mutableStateOf(false) }
    var hasSearched: Boolean by remember { mutableStateOf(false) }
    var errorMessage: String? by remember { mutableStateOf(null) }

    val coroutineScope = rememberCoroutineScope()

    // ── Perform Search ────────────────────────
    fun performSearch() {
        if (query.isBlank()) {
            return
        }

        isSearching = true
        errorMessage = null

        coroutineScope.launch {
            val result: Result<List<SearchMatch>> = ItemRepository.searchByText(query = query)

            if (result.isSuccess) {
                results = result.getOrDefault(emptyList())
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Search failed"
            }

            hasSearched = true
            isSearching = false
        }
    }

    // ── UI ────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Search Items", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding)
        ) {
            // ── Search Bar ────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { newValue: String -> query = newValue },
                    placeholder = { Text(text = "Describe what you're looking for...") },
                    modifier = Modifier.weight(weight = 1f),
                    singleLine = true,
                    shape = RoundedCornerShape(size = 12.dp)
                )

                Spacer(modifier = Modifier.width(width = 8.dp))

                Button(
                    onClick = { performSearch() },
                    shape = RoundedCornerShape(size = 12.dp),
                    enabled = !isSearching && query.isNotBlank()
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(size = 20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "🔍")
                    }
                }
            }

            // ── Results Area ──────────────────
            when {
                isSearching -> {
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
                                text = "Searching with AI...",
                                color = TextSecondary
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Something went wrong",
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                hasSearched && results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🔍",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(height = 16.dp))
                            Text(
                                text = "No matching items found",
                                fontSize = 16.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                results.isNotEmpty() -> {
                    // Results count
                    Text(
                        text = "${results.size} matches found",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        items(items = results) { match: SearchMatch ->
                            SearchResultCard(
                                match = match,
                                onClick = {
                                    onNavigateToDetail(match.itemId)
                                }
                            )
                        }
                    }
                }

                else -> {
                    // Initial state — no search yet
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🤖",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(height = 16.dp))
                            Text(
                                text = "AI-Powered Search",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(height = 8.dp))
                            Text(
                                text = "Describe what you lost or found\nand we'll find similar items",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single search result card showing the item title,
 * status, and similarity score as a percentage.
 */
@Composable
fun SearchResultCard(
    match: SearchMatch,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(size = 12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item title and status
            Column(
                modifier = Modifier.weight(weight = 1f)
            ) {
                Text(
                    text = match.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(height = 4.dp))
                StatusChip(status = match.status)
            }

            Spacer(modifier = Modifier.width(width = 12.dp))

            // Similarity score as percentage
            val similarityPercent: Int = (match.similarity * 100).toInt()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${similarityPercent}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "match",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
