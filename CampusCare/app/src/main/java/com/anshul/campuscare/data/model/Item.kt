package com.anshul.campuscare.data.model

// ──────────────────────────────────────────────
// Item Data Classes
//
// Represents a lost or found item from the backend.
// The backend has three statuses: LOST, FOUND, CLAIMED.
// Images are stored as file paths like "uploads/abc123.jpg".
// ──────────────────────────────────────────────

/**
 * A single lost/found item.
 * Matches the Item model from the backend Prisma schema.
 *
 * The "user" field is included when the backend uses
 * Prisma's "include: { user: ... }" in the query.
 */
data class Item(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val dateLostOrFound: String,
    val status: String,
    val images: List<String>,
    val userId: Int,
    val user: User?,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Response wrapper for GET /items
 * The backend returns: { "items": [ ... ] }
 */
data class ItemListResponse(
    val items: List<Item>
)

/**
 * Response wrapper for GET /items/:id
 * The backend returns: { "item": { ... } }
 */
data class ItemResponse(
    val item: Item
)

/**
 * Response wrapper for POST /items (create)
 * The backend returns: { "item": { ... }, "matches": [ ... ] }
 */
data class CreateItemResponse(
    val item: Item,
    val matches: List<SearchMatch>
)

/**
 * Response wrapper for DELETE /items/:id
 * The backend returns: { "message": "Item deleted" }
 */
data class DeleteItemResponse(
    val message: String
)
