package com.forzaball.data.profile

import android.content.Context
import android.net.Uri
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
    companion object {
        private const val AVATAR_FULL = "avatar_full.jpg"
        private const val AVATAR_THUMB = "avatar_thumb.jpg"
        /** Legacy paths that may exist from earlier builds. */
        private val LEGACY_AVATAR_NAMES = listOf("profile.jpg", "avatar.jpg", "avatar.png")
    }

    suspend fun uploadProfilePhoto(croppedUri: Uri): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = auth.currentUser?.uid ?: error("Not signed in")
            val processed = ProfileImageProcessor.processCroppedImage(context, croppedUri)
            deleteExistingAvatars(uid)
            val fullRef = storage.reference.child("users/$uid/$AVATAR_FULL")
            val thumbRef = storage.reference.child("users/$uid/$AVATAR_THUMB")
            fullRef.putBytes(processed.fullJpeg).await()
            thumbRef.putBytes(processed.thumbJpeg).await()
            val fullUrl = fullRef.downloadUrl.await().toString()
            val thumbUrl = thumbRef.downloadUrl.await().toString()
            val existing = preferencesRepository.observeUserPreferences().first()
            val cacheVersion = System.currentTimeMillis()
            val updated = existing.copy(
                profilePhotoUrl = fullUrl,
                profilePhotoThumbUrl = thumbUrl,
                profilePhotoCacheVersion = cacheVersion,
            )
            preferencesRepository.updateUserPreferences(updated)
            feedRepository.syncUserProfilePreferences(updated)
            fullUrl to thumbUrl
        }.onFailure { Timber.w(it, "uploadProfilePhoto") }
    }

    private suspend fun deleteExistingAvatars(uid: String) {
        val paths = buildList {
            add("users/$uid/$AVATAR_FULL")
            add("users/$uid/$AVATAR_THUMB")
            LEGACY_AVATAR_NAMES.forEach { name -> add("users/$uid/$name") }
        }
        paths.forEach { path ->
            runCatching {
                storage.reference.child(path).delete().await()
            }.onFailure { e ->
                Timber.tag("ProfileImage").d(e, "delete skipped or missing: %s", path)
            }
        }
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
