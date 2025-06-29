package com.arranquesuave.motorcontrolapp.data

import com.arranquesuave.motorcontrolapp.network.AuthApi
import com.arranquesuave.motorcontrolapp.network.RetrofitClient
import com.arranquesuave.motorcontrolapp.network.model.AuthRequest
import com.arranquesuave.motorcontrolapp.network.model.VerifyRequest
import com.arranquesuave.motorcontrolapp.network.model.AuthResponse
import retrofit2.Response

class AuthRepository(private val api: AuthApi = RetrofitClient.authApi) {
    suspend fun signup(email: String, password: String, confirm: String): Response<Void> =
        api.signup(AuthRequest(email, password, confirm))

    suspend fun verify(email: String, code: String): AuthResponse =
        api.verify(VerifyRequest(email, code))

    suspend fun login(email: String, password: String): AuthResponse =
        api.login(AuthRequest(email, password))
    suspend fun logout(): Response<Void> =
        api.logout()
}
