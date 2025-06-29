package com.arranquesuave.motorcontrolapp.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.arranquesuave.motorcontrolapp.data.AuthRepository
import com.arranquesuave.motorcontrolapp.network.model.AuthResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class AuthViewModel(private val repo: AuthRepository = AuthRepository()) : ViewModel() {
    val signupState = MutableStateFlow<Result<Response<Void>>?>(null)
    val verifyState = MutableStateFlow<Result<AuthResponse>?>(null)
    val loginState  = MutableStateFlow<Result<AuthResponse>?>(null)
    val logoutState = MutableStateFlow<Result<Response<Void>>?>(null)

    fun signup(email: String, password: String, confirm: String) = viewModelScope.launch {
        signupState.value = runCatching { repo.signup(email, password, confirm) }
    }

    fun verify(email: String, code: String) = viewModelScope.launch {
        verifyState.value = runCatching { repo.verify(email, code) }
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        loginState.value = runCatching { repo.login(email, password) }
    }

    fun logout() = viewModelScope.launch {
        logoutState.value = runCatching { repo.logout() }
    }

}
