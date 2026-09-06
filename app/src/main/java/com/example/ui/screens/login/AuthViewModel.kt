package com.example.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import com.example.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object SignedOut : AuthUiState
    data object SigningIn : AuthUiState
    data class Authenticated(val user: UserProfile) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository = AppModule.provideAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.SignedOut)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkInitialAuthState()
    }

    private fun checkInitialAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null && user.uid.isNotBlank()) {
                    _uiState.value = AuthUiState.Authenticated(user)
                } else {
                    _uiState.value = AuthUiState.SignedOut
                }
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        _uiState.value = AuthUiState.SigningIn
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(context)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Google Sign-In failed")
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState.SignedOut
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.SignedOut
        }
    }
}
