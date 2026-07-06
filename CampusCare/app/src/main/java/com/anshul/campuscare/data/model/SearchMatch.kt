package com.anshul.campuscare.data.model

// ──────────────────────────────────────────────
// Search Match Data Classes
//
// Represents a similarity search result from the
// backend. The backend returns matches with a
// similarity score between 0.0 and 1.0.
// ──────────────────────────────────────────────

/**
 * A single search match result.
 * The backend returns these when you search by text or image,
 * and also when creating a new item (auto-matching).
 */
data class SearchMatch(
    val itemId: Int,
    val title: String,
    val status: String,
    val similarity: Double
)

/**
 * Response wrapper for POST /search/text and POST /search/image
 * The backend returns: { "matches": [ ... ] }
 */
data class SearchResponse(
    val matches: List<SearchMatch>
)

/**
 * Request body for POST /search/text
 * The backend expects: { "query": "..." }
 */
data class TextSearchRequest(
    val query: String
)
