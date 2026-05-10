package com.forzaball.data.auth

import com.forzaball.domain.repository.AuthRepository
import com.forzaball.domain.repository.AuthResult
import com.forzaball.domain.repository.AuthState
import com.forzaball.domain.repository.SignInResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override fun authState(): Flow<AuthState> = callbackFlow {
        trySend(
            when (val user = firebaseAuth.currentUser) {
                null -> AuthState.SignedOut
                else -> AuthState.SignedIn(uid = user.uid, email = user.email)
            }
        )
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(
                when (val u = auth.currentUser) {
                    null -> AuthState.SignedOut
                    else -> AuthState.SignedIn(uid = u.uid, email = u.email)
                }
            )
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String?,
    ): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null && !displayName.isNullOrBlank()) {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
            }
            AuthResult.Success
        } catch (e: Exception) {
            Timber.w(e, "signUpWithEmail failed")
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): SignInResult {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            SignInResult.Success
        } catch (e: Exception) {
            Timber.w(e, "signInWithEmail failed")
            SignInResult.Error(e.message ?: "Sign in failed")
        }
    }

    override suspend fun signInWithGoogle(idToken: String): SignInResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            SignInResult.Success
        } catch (e: Exception) {
            Timber.w(e, "signInWithGoogle failed")
            SignInResult.Error(e.message ?: "Google sign in failed")
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid
}
