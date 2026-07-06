package com.anshul.campuscare.data.model

import com.google.gson.annotations.SerializedName

data class DiscussionAuthor(
    val id: Int,
    val anonymousUsername: String?,
    val avatarColor: String?
)

data class Discussion(
    val id: Int,
    val title: String,
    val description: String,
    val images: List<String>,
    val latitude: Double?,
    val longitude: Double?,
    val buildingName: String?,
    val upvoteCount: Int,
    val createdAt: String,
    val createdBy: DiscussionAuthor?,
    @SerializedName("_count") val count: DiscussionCount?,
    val hasUpvoted: Boolean = false,
    val comments: List<Comment>? = null
)

data class DiscussionCount(
    val comments: Int
)

data class Comment(
    val id: Int,
    val text: String,
    val image: String?,
    val createdAt: String,
    val author: DiscussionAuthor?,
    val replies: List<Reply>? = null
)

data class Reply(
    val id: Int,
    val text: String,
    val createdAt: String,
    val author: DiscussionAuthor?
)

data class Notification(
    val id: Int,
    val type: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String,
    val discussionId: Int?,
    val commentId: Int?
)

// API Responses
data class DiscussionsResponse(
    val discussions: List<Discussion>
)

data class SingleDiscussionResponse(
    val discussion: Discussion
)

data class CommentResponse(
    val comment: Comment
)

data class ReplyResponse(
    val reply: Reply
)

data class NotificationsResponse(
    val notifications: List<Notification>
)

data class DuplicateResponse(
    val duplicate: Boolean,
    val matchedDiscussion: Discussion,
    val similarity: Double,
    val message: String
)

data class SearchDiscussionsResponse(
    val matches: List<DiscussionMatch>
)

data class DiscussionMatch(
    val discussionId: Int,
    val title: String,
    val description: String,
    val similarity: Double
)
