package com.arranquesuave.motorcontrolapp.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arranquesuave.motorcontrolapp.auth.data.AuthRepository
import com.arranquesuave.motorcontrolapp.auth.model.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class AuthViewModel(private val repo: AuthRepository = AuthRepository()) : ViewModel() {
    val signupState = MutableStateFlow<Result<AuthResponse>?>(null)
    val loginState  = MutableStateFlow<Result<AuthResponse>?>(null)
    val logoutState = MutableStateFlow<Result<Response<Void>>?>(null)

    fun signup(email: String, password: String, confirm: String) = viewModelScope.launch {
        signupState.value = runCatching { repo.signup(email, password, confirm) }
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        loginState.value = runCatching { repo.login(email, password) }
    }

    fun logout() = viewModelScope.launch {
        logoutState.value = runCatching { repo.logout() }
    }
}
