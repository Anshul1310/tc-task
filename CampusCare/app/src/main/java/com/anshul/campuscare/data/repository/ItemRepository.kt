package com.anshul.campuscare.data.repository

// ──────────────────────────────────────────────
// Item Repository
//
// Single place for all data operations. Wraps the
// API service calls and returns Result<T> so the
// UI can handle success and failure easily.
//
// This is an object (singleton) — no dependency
// injection needed. Just call ItemRepository.getAllItems().
// ──────────────────────────────────────────────

import android.content.Context
import android.net.Uri
import com.anshul.campuscare.data.model.CreateItemResponse
import com.anshul.campuscare.data.model.Item
import com.anshul.campuscare.data.model.SearchMatch
import com.anshul.campuscare.data.model.TextSearchRequest
import com.anshul.campuscare.data.model.User
import com.anshul.campuscare.data.network.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

object ItemRepository {

    private val apiService = ApiClient.apiService

    // ── Auth ──────────────────────────────────

    /**
     * Check if the user is currently logged in by
     * calling GET /auth/me. Returns the user if the
     * session cookie is valid, null otherwise.
     */
    suspend fun getCurrentUser(): Result<User?> {
        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful) {
                val userResponse = response.body()
                Result.success(userResponse?.user)
            } else {
                // 401 means not logged in — that's not an error
                Result.success(null)
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Log out the current user.
     */
    suspend fun logout(): Result<String> {
        return try {
            val response = apiService.logout()
            if (response.isSuccessful) {
                ApiClient.cookieJar.clearAllCookies()
                Result.success("Logged out successfully")
            } else {
                Result.failure(Exception("Logout failed"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    // ── Items ─────────────────────────────────

    /**
     * Get all items, optionally filtered by status.
     * @param status "LOST", "FOUND", "CLAIMED", or null for all
     */
    suspend fun getAllItems(status: String? = null): Result<List<Item>> {
        return try {
            val response = apiService.getAllItems(status = status)
            if (response.isSuccessful) {
                val itemList = response.body()?.items ?: emptyList()
                Result.success(itemList)
            } else {
                Result.failure(Exception("Failed to fetch items: ${response.code()}"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Get a single item by its ID.
     */
    suspend fun getItemById(itemId: Int): Result<Item> {
        return try {
            val response = apiService.getItemById(itemId = itemId)
            if (response.isSuccessful) {
                val item = response.body()?.item
                if (item != null) {
                    Result.success(item)
                } else {
                    Result.failure(Exception("Item not found"))
                }
            } else {
                Result.failure(Exception("Failed to fetch item: ${response.code()}"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Create a new lost or found item with optional images.
     *
     * @param title        Item title
     * @param description  Item description
     * @param category     Category (e.g., "Wallet", "Phone")
     * @param location     Where the item was lost/found
     * @param date         Date in format "YYYY-MM-DD"
     * @param status       "LOST" or "FOUND"
     * @param imageUris    List of image URIs selected from the gallery
     * @param context      Android context needed to read image files
     */
    suspend fun createItem(
        title: String,
        description: String,
        category: String,
        location: String,
        date: String,
        status: String,
        imageUris: List<Uri>,
        context: Context
    ): Result<CreateItemResponse> {
        return try {
            // Convert strings to RequestBody for multipart
            val titleBody: RequestBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody: RequestBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody: RequestBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val locationBody: RequestBody = location.toRequestBody("text/plain".toMediaTypeOrNull())
            val dateBody: RequestBody = date.toRequestBody("text/plain".toMediaTypeOrNull())
            val statusBody: RequestBody = status.toRequestBody("text/plain".toMediaTypeOrNull())

            // Convert image URIs to multipart parts
            val imageParts: List<MultipartBody.Part> = buildImageParts(
                imageUris = imageUris,
                context = context,
                partName = "images"
            )

            val response = apiService.createItem(
                title = titleBody,
                description = descriptionBody,
                category = categoryBody,
                location = locationBody,
                dateLostOrFound = dateBody,
                status = statusBody,
                images = imageParts
            )

            if (response.isSuccessful) {
                val createResponse = response.body()
                if (createResponse != null) {
                    Result.success(createResponse)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                Result.failure(Exception("Failed to create item: ${response.code()}"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Update an existing item.
     */
    suspend fun updateItem(
        itemId: Int,
        title: String,
        description: String,
        category: String,
        location: String,
        date: String,
        imageUris: List<Uri>,
        context: Context
    ): Result<Item> {
        return try {
            val titleBody: RequestBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody: RequestBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody: RequestBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val locationBody: RequestBody = location.toRequestBody("text/plain".toMediaTypeOrNull())
            val dateBody: RequestBody = date.toRequestBody("text/plain".toMediaTypeOrNull())

            val imageParts: List<MultipartBody.Part> = buildImageParts(
                imageUris = imageUris,
                context = context,
                partName = "images"
            )

            val response = apiService.updateItem(
                itemId = itemId,
                title = titleBody,
                description = descriptionBody,
                category = categoryBody,
                location = locationBody,
                dateLostOrFound = dateBody,
                images = imageParts
            )

            if (response.isSuccessful) {
                val item = response.body()?.item
                if (item != null) {
                    Result.success(item)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                Result.failure(Exception("Failed to update item: ${response.code()}"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Delete an item by ID.
     */
    suspend fun deleteItem(itemId: Int): Result<String> {
        return try {
            val response = apiService.deleteItem(itemId = itemId)
            if (response.isSuccessful) {
                Result.success("Item deleted")
            } else if (response.code() == 403) {
                Result.failure(Exception("You can only delete your own items"))
            } else {
                Result.failure(Exception("Failed to delete item: ${response.code()}"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Mark an item as claimed.
     */
    suspend fun markAsClaimed(itemId: Int): Result<Item> {
        return try {
            val response = apiService.markAsClaimed(itemId = itemId)
            if (response.isSuccessful) {
                val item = response.body()?.item
                if (item != null) {
                    Result.success(item)
                } else {
                    Result.failure(Exception("Empty response from server"))
                }
            } else if (response.code() == 403) {
                Result.failure(Exception("You can only claim your own items"))
            } else {
                Result.failure(Exception("Failed to claim item: ${response.code()}"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    // ── Search ────────────────────────────────

    /**
     * Search items by text description using AI similarity.
     */
    suspend fun searchByText(query: String): Result<List<SearchMatch>> {
        return try {
            val request = TextSearchRequest(query = query)
            val response = apiService.searchByText(request = request)
            if (response.isSuccessful) {
                val matches = response.body()?.matches ?: emptyList()
                Result.success(matches)
            } else {
                Result.failure(Exception("Search failed: ${response.code()}"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    // ── Helper: Build Multipart Image Parts ───

    /**
     * Converts a list of image URIs into multipart body parts
     * that Retrofit can send to the backend.
     *
     * @param imageUris  URIs from the image picker
     * @param context    Android context to open input streams
     * @param partName   The form field name (e.g., "images")
     */
    private fun buildImageParts(
        imageUris: List<Uri>,
        context: Context,
        partName: String
    ): List<MultipartBody.Part> {
        val parts: MutableList<MultipartBody.Part> = mutableListOf()

        for (uri: Uri in imageUris) {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val bytes: ByteArray = inputStream.readBytes()
                inputStream.close()

                val requestBody: RequestBody = bytes.toRequestBody(
                    "image/*".toMediaTypeOrNull()
                )

                val fileName: String = "image_${System.currentTimeMillis()}.jpg"

                val part: MultipartBody.Part = MultipartBody.Part.createFormData(
                    name = partName,
                    filename = fileName,
                    body = requestBody
                )

                parts.add(part)
            }
        }

        return parts
    }
}
