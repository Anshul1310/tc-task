package com.anshul.campuscare.data.repository

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
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

    suspend fun getDiscussionById(id: Int): Result<DiscussionDetails> = try {
        val response = apiService.getDiscussionById(id)
        if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            Result.success(
                DiscussionDetails(
                    discussion = body.discussion,
                    relatedDiscussions = body.relatedDiscussions ?: emptyList()
                )
            )
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
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
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
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
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

    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = try {
        val response = apiService.reverseGeocode(latitude = latitude, longitude = longitude)
        if (response.isSuccessful && response.body() != null) {
            response.body()!!.address
        } else null
    } catch (e: Exception) {
        null
    }


    suspend fun ragSearch(query: String): Result<RagSearchResponse> = try {
        val response = apiService.ragSearch(query)
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("RAG search failed: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return getFileFromUriFallback(uri)
            }

            val maxDimension = 1600
            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val scaleOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, scaleOptions)
            } ?: return getFileFromUriFallback(uri)

            val fileName = "compressed_${System.currentTimeMillis()}_${(100..999).random()}.jpg"
            val compressedFile = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(compressedFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
            bitmap.recycle()

            compressedFile
        } catch (e: Exception) {
            e.printStackTrace()
            getFileFromUriFallback(uri)
        }
    }

    private fun getFileFromUriFallback(uri: Uri): File? {
        val contentResolver = context.contentResolver
        val tempFile = File(context.cacheDir, "fallback_${System.currentTimeMillis()}.jpg")
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
