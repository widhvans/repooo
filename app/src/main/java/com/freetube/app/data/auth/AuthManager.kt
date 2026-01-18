package com.freetube.app.data.auth

import android.content.Context
import android.webkit.CookieManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

/**
 * Manages YouTube authentication state
 * Uses cookies from WebView login for API calls
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val KEY_COOKIES = stringPreferencesKey("youtube_cookies")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_IS_SIGNED_IN = stringPreferencesKey("is_signed_in")
    }
    
    val isSignedIn: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        prefs[KEY_IS_SIGNED_IN] == "true"
    }
    
    val userName: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[KEY_USER_NAME]
    }
    
    val userEmail: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[KEY_USER_EMAIL]
    }
    
    val cookies: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[KEY_COOKIES]
    }
    
    /**
     * Save authentication after successful WebView login
     */
    suspend fun saveAuth(cookies: String, userName: String? = null, userEmail: String? = null) {
        context.authDataStore.edit { prefs ->
            prefs[KEY_COOKIES] = cookies
            prefs[KEY_IS_SIGNED_IN] = "true"
            userName?.let { prefs[KEY_USER_NAME] = it }
            userEmail?.let { prefs[KEY_USER_EMAIL] = it }
        }
    }
    
    /**
     * Sign out - clear all auth data
     */
    suspend fun signOut() {
        // Clear DataStore
        context.authDataStore.edit { prefs ->
            prefs.clear()
        }
        
        // Clear WebView cookies
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
    }
    
    /**
     * Get current cookies for API calls
     */
    suspend fun getCookiesSync(): String? {
        var result: String? = null
        context.authDataStore.data.collect { prefs ->
            result = prefs[KEY_COOKIES]
        }
        return result
    }
}
