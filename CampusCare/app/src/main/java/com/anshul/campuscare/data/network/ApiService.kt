package com.anshul.campuscare.data.network

// ──────────────────────────────────────────────
// API Service Interface
//
// Defines all the HTTP endpoints for the backend.
// ──────────────────────────────────────────────

import com.anshul.campuscare.data.model.AddressResponse
import com.anshul.campuscare.data.model.CommentResponse
import com.anshul.campuscare.data.model.DiscussionsResponse
import com.anshul.campuscare.data.model.LogoutResponse
import com.anshul.campuscare.data.model.ReplyResponse
import com.anshul.campuscare.data.model.SearchDiscussionsResponse
import com.anshul.campuscare.data.model.SingleDiscussionResponse
import com.anshul.campuscare.data.model.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {

    // ── Auth Endpoints ────────────────────────

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<LogoutResponse>

    // ── Community Discussions ──────────────────

    @GET("discussions")
    suspend fun getAllDiscussions(): Response<DiscussionsResponse>

    @GET("discussions/trending")
    suspend fun getTrendingDiscussions(): Response<DiscussionsResponse>

    @GET("discussions/{id}")
    suspend fun getDiscussionById(@Path("id") discussionId: Int): Response<SingleDiscussionResponse>

    @Multipart
    @POST("discussions")
    suspend fun createDiscussion(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part("buildingName") buildingName: RequestBody?,
        @Part("createAnyway") createAnyway: RequestBody?,
        @Part images: List<MultipartBody.Part>
    ): Response<SingleDiscussionResponse>

    @POST("discussions/{id}/upvote")
    suspend fun toggleUpvote(@Path("id") discussionId: Int): Response<Any>

    @Multipart
    @POST("discussions/{id}/comments")
    suspend fun addComment(
        @Path("id") discussionId: Int,
        @Part("text") text: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<CommentResponse>

    @FormUrlEncoded
    @POST("comments/{id}/replies")
    suspend fun addReply(
        @Path("id") commentId: Int,
        @Field("text") text: String
    ): Response<ReplyResponse>

    // ── Community Search ────────────────────────

    @FormUrlEncoded
    @POST("community/search/text")
    suspend fun searchDiscussionsByText(
        @Field("query") query: String
    ): Response<SearchDiscussionsResponse>

    @Multipart
    @POST("community/search/image")
    suspend fun searchDiscussionsByImage(
        @Part image: MultipartBody.Part
    ): Response<SearchDiscussionsResponse>

    @FormUrlEncoded
    @POST("community/search/rag")
    suspend fun ragSearch(
        @Field("query") query: String
    ): Response<com.anshul.campuscare.data.model.RagSearchResponse>

    // ── Location / Reverse Geocoding ──────────

    @GET("location/reverse-geocode")
    suspend fun reverseGeocode(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double
    ): Response<AddressResponse>
}
