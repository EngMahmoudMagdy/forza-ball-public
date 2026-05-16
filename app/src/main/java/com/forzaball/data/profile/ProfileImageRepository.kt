package com.forzaball.data.profile

import android.content.Context
import android.net.Uri
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.repository.FeedRepository
import com.forzaball.domain.repository.PreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

class ProfileImageRepository(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    private val preferencesRepository: PreferencesRepository,
    private val feedRepository: FeedRepository,
) {
    suspend fun uploadProfilePhoto(uri: Uri): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = auth.currentUser?.uid ?: error("Not signed in")
            val processed = ProfileImageProcessor.process(context, uri)
            val fullRef = storage.reference.child("users/$uid/avatar_full.jpg")
            val thumbRef = storage.reference.child("users/$uid/avatar_thumb.jpg")
            fullRef.putBytes(processed.fullJpeg).await()
            thumbRef.putBytes(processed.thumbJpeg).await()
            val fullUrl = fullRef.downloadUrl.await().toString()
            val thumbUrl = thumbRef.downloadUrl.await().toString()
            val existing = preferencesRepository.observeUserPreferences().first()
            val updated = existing.copy(
                profilePhotoUrl = fullUrl,
                profilePhotoThumbUrl = thumbUrl,
            )
            preferencesRepository.updateUserPreferences(updated)
            feedRepository.syncUserProfilePreferences(updated)
            fullUrl to thumbUrl
        }.onFailure { Timber.w(it, "uploadProfilePhoto") }
    }

    suspend fun updateDisplayName(name: String): Result<Unit> = runCatching {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Name required" }
        require(trimmed.length <= 40) { "Name too long" }
        val existing = preferencesRepository.observeUserPreferences().first()
        val updated = existing.copy(nickname = trimmed)
        preferencesRepository.updateUserPreferences(updated)
        feedRepository.syncUserProfilePreferences(updated)
    }
}
