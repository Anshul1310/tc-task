package com.anshul.campuscare.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.anshul.campuscare.data.model.*
import com.anshul.campuscare.data.network.ApiService
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.webkit.MimeTypeMap
import android.content.ContentResolver

class DiscussionRepository(private val apiService: ApiService, private val context: Context) {

    suspend fun getAllDiscussions(): Result<List<Discussion>> = try {
        val response = apiService.getAllDiscussions()
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.discussions)
        } else {
            Result.failure(Exception("Failed to load discussions: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTrendingDiscussions(): Result<List<Discussion>> = try {
        val response = apiService.getTrendingDiscussions()
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.discussions)
        } else {
            Result.failure(Exception("Failed to load trending discussions: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getDiscussionById(id: Int): Result<Discussion> = try {
        val response = apiService.getDiscussionById(id)
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.discussion)
        } else {
            Result.failure(Exception("Failed to load discussion: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun createDiscussion(
        title: String,
        description: String,
        latitude: Double?,
        longitude: Double?,
        buildingName: String?,
        createAnyway: Boolean,
        imageUris: List<Uri>
    ): Result<Discussion> = try {
        val titlePart = title.toRequestBody("text/plain".toMediaTypeOrNull())
        val descPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
        val latPart = latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val lngPart = longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val buildingPart = buildingName?.toRequestBody("text/plain".toMediaTypeOrNull())
        val createAnywayPart = createAnyway.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val imageParts = imageUris.mapNotNull { uri ->
            getFileFromUri(uri)?.let { file ->
                val mimeType = getMimeType(uri) ?: "image/jpeg"
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("images", file.name, requestFile)
            }
        }

        val response = apiService.createDiscussion(
            title = titlePart,
            description = descPart,
            latitude = latPart,
            longitude = lngPart,
            buildingName = buildingPart,
            createAnyway = createAnywayPart,
            images = imageParts
        )

        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.discussion)
        } else if (response.code() == 409) {
            // Duplicate detected
            val errorBody = response.errorBody()?.string()
            val duplicateResponse = Gson().fromJson(errorBody, DuplicateResponse::class.java)
            Result.failure(DuplicateDiscussionException(duplicateResponse))
        } else {
            Result.failure(Exception("Failed to create discussion: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun toggleUpvote(discussionId: Int): Result<Unit> = try {
        val response = apiService.toggleUpvote(discussionId)
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to toggle upvote: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addComment(discussionId: Int, text: String, imageUri: Uri?): Result<Comment> = try {
        val textPart = text.toRequestBody("text/plain".toMediaTypeOrNull())
        val imagePart = imageUri?.let { uri ->
            getFileFromUri(uri)?.let { file ->
                val mimeType = getMimeType(uri) ?: "image/jpeg"
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, requestFile)
            }
        }

        val response = apiService.addComment(discussionId, textPart, imagePart)
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.comment)
        } else {
            Result.failure(Exception("Failed to add comment: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addReply(commentId: Int, text: String): Result<Reply> = try {
        val response = apiService.addReply(commentId, text)
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.reply)
        } else {
            Result.failure(Exception("Failed to add reply: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getNotifications(): Result<List<Notification>> = try {
        val response = apiService.getNotifications()
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.notifications)
        } else {
            Result.failure(Exception("Failed to load notifications: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun markNotificationAsRead(notificationId: Int): Result<Unit> = try {
        val response = apiService.markNotificationAsRead(notificationId)
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to mark notification as read: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun searchDiscussionsByText(query: String): Result<List<DiscussionMatch>> = try {
        val response = apiService.searchDiscussionsByText(query)
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!.matches)
        } else {
            Result.failure(Exception("Search failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun searchDiscussionsByImage(imageUri: Uri): Result<List<DiscussionMatch>> = try {
        val file = getFileFromUri(imageUri)
        if (file != null) {
            val mimeType = getMimeType(imageUri) ?: "image/jpeg"
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
            
            val response = apiService.searchDiscussionsByImage(imagePart)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.matches)
            } else {
                Result.failure(Exception("Image search failed: ${response.code()}"))
            }
        } else {
            Result.failure(Exception("Could not read image file"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun getMimeType(uri: Uri): String? {
        var mimeType: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            mimeType = context.contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
        return mimeType
    }

    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        val name = cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) it.getString(nameIndex) else "temp_file"
            } else "temp_file"
        } ?: "temp_file"

        val tempFile = File(context.cacheDir, name)
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class DuplicateDiscussionException(val duplicateResponse: DuplicateResponse) : Exception(duplicateResponse.message)
