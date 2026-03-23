package com.spencehouse.logue.ui.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spencehouse.logue.service.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    var uiState by mutableStateOf(LoginUiState())
        private set


    fun onUsernameChange(username: String) {
        uiState = uiState.copy(username = username, error = null)
    }

    fun onPasswordChange(password: String) {
        uiState = uiState.copy(password = password, error = null)
    }

    fun login() {
        if (uiState.username.isBlank() || uiState.password.isBlank()) {
            uiState = uiState.copy(error = "Username and password cannot be empty")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val result = authService.login(uiState.username, uiState.password)
            result.onSuccess {
                uiState = uiState.copy(isLoading = false, loginSuccess = true, username = "", password = "")
            }.onFailure {
                uiState = uiState.copy(isLoading = false, error = it.message ?: "An unknown error occurred")
            }
        }
    }

    fun onLoginComplete() {
        uiState = uiState.copy(loginSuccess = false)
    }
}
