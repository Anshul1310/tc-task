package com.anshul.campuscare.data.network

// ──────────────────────────────────────────────
// API Service Interface
//
// Defines all the HTTP endpoints that match the
// backend routes. Retrofit generates the actual
// implementation at runtime.
//
// Endpoints:
//   Auth:   GET /auth/me, POST /auth/logout
//   Items:  GET /items, GET /items/{id}, POST /items,
//           PUT /items/{id}, DELETE /items/{id},
//           PATCH /items/{id}/claim
//   Search: POST /search/text, POST /search/image
// ──────────────────────────────────────────────

import com.anshul.campuscare.data.model.CreateItemResponse
import com.anshul.campuscare.data.model.DeleteItemResponse
import com.anshul.campuscare.data.model.ItemListResponse
import com.anshul.campuscare.data.model.ItemResponse
import com.anshul.campuscare.data.model.LogoutResponse
import com.anshul.campuscare.data.model.SearchResponse
import com.anshul.campuscare.data.model.TextSearchRequest
import com.anshul.campuscare.data.model.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ── Auth Endpoints ────────────────────────

    /**
     * Get the currently logged-in user.
     * Returns 401 if not authenticated.
     */
    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    /**
     * Log out the current user by destroying the session.
     */
    @POST("auth/logout")
    suspend fun logout(): Response<LogoutResponse>

    // ── Item Endpoints ────────────────────────

    /**
     * Get all items, optionally filtered by status.
     * @param status Optional filter: "LOST", "FOUND", or "CLAIMED"
     */
    @GET("items")
    suspend fun getAllItems(
        @Query("status") status: String? = null
    ): Response<ItemListResponse>

    /**
     * Get a single item by its ID.
     */
    @GET("items/{id}")
    suspend fun getItemById(
        @Path("id") itemId: Int
    ): Response<ItemResponse>

    /**
     * Create a new lost or found item.
     * Uses multipart form data because it can include images.
     */
    @Multipart
    @POST("items")
    suspend fun createItem(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("location") location: RequestBody,
        @Part("dateLostOrFound") dateLostOrFound: RequestBody,
        @Part("status") status: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): Response<CreateItemResponse>

    /**
     * Update an existing item.
     * Uses multipart form data because it can include new images.
     */
    @Multipart
    @PUT("items/{id}")
    suspend fun updateItem(
        @Path("id") itemId: Int,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("category") category: RequestBody,
        @Part("location") location: RequestBody,
        @Part("dateLostOrFound") dateLostOrFound: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): Response<ItemResponse>

    /**
     * Delete an item. Only the owner can delete.
     */
    @DELETE("items/{id}")
    suspend fun deleteItem(
        @Path("id") itemId: Int
    ): Response<DeleteItemResponse>

    /**
     * Mark an item as claimed. Only the owner can claim.
     */
    @PATCH("items/{id}/claim")
    suspend fun markAsClaimed(
        @Path("id") itemId: Int
    ): Response<ItemResponse>

    // ── Search Endpoints ──────────────────────

    /**
     * Search items by text description.
     * The backend generates an embedding and finds similar items.
     */
    @POST("search/text")
    suspend fun searchByText(
        @Body request: TextSearchRequest
    ): Response<SearchResponse>

    /**
     * Search items by uploading an image.
     * The backend generates a CLIP embedding and finds similar items.
     */
    @Multipart
    @POST("search/image")
    suspend fun searchByImage(
        @Part image: MultipartBody.Part
    ): Response<SearchResponse>
}
