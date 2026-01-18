package com.freetube.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetube.app.data.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    
    fun saveAuth(cookies: String) {
        viewModelScope.launch {
            authManager.saveAuth(cookies, "YouTube User")
            android.util.Log.d("SignInViewModel", "Auth saved successfully")
        }
    }
}
