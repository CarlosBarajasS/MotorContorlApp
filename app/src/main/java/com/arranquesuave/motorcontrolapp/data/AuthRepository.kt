package com.arranquesuave.motorcontrolapp.data

import com.arranquesuave.motorcontrolapp.network.AuthApi
import com.arranquesuave.motorcontrolapp.network.RetrofitClient
import com.arranquesuave.motorcontrolapp.network.model.AuthRequest

class AuthRepository(private val api: AuthApi = RetrofitClient.authApi) {
    suspend fun signup(email: String, password: String, confirm: String) =
        api.signup(AuthRequest(email, password, confirm))

    suspend fun login(email: String, password: String) =
        api.login(AuthRequest(email, password, ""))

    suspend fun logout() = api.logout()
}
