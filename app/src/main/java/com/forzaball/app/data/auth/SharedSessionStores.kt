package com.forzaball.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.forzaball.shared.auth.AuthApi
import com.forzaball.shared.auth.SessionState
import com.forzaball.shared.auth.SessionTokenStore
import com.forzaball.shared.auth.SessionTokens
import com.forzaball.shared.auth.SessionUserStore

private const val PREFS_NAME = "kmp_session_store"
private const val KEY_ACCESS_TOKEN = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER_ID = "user_id"
private const val KEY_EMAIL = "email"

private fun securePrefs(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    return EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}

class AndroidSessionTokenStore(
    context: Context,
) : SessionTokenStore {
    private val prefs = securePrefs(context)

    override suspend fun readTokens(): SessionTokens? {
        val access = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null)
        return SessionTokens(accessToken = access, refreshToken = refresh)
    }

    override suspend fun writeTokens(tokens: SessionTokens?) {
        prefs.edit().apply {
            if (tokens == null) {
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_REFRESH_TOKEN)
            } else {
                putString(KEY_ACCESS_TOKEN, tokens.accessToken)
                putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
            }
        }.apply()
    }
}

class AndroidSessionUserStore(
    context: Context,
) : SessionUserStore {
    private val prefs = securePrefs(context)

    override suspend fun readUser(): SessionState.SignedIn? {
        val uid = prefs.getString(KEY_USER_ID, null) ?: return null
        return SessionState.SignedIn(
            userId = uid,
            email = prefs.getString(KEY_EMAIL, null),
        )
    }

    override suspend fun writeUser(user: SessionState.SignedIn?) {
        prefs.edit().apply {
            if (user == null) {
                remove(KEY_USER_ID)
                remove(KEY_EMAIL)
            } else {
                putString(KEY_USER_ID, user.userId)
                putString(KEY_EMAIL, user.email)
            }
        }.apply()
    }
}

class NoOpAuthRefreshApi : AuthApi {
    override suspend fun refresh(tokens: SessionTokens): SessionTokens? = tokens
}
