package com.arranquesuave.motorcontrolapp.auth.model

data class AuthResponse(
    val token: String,
    val user: User,
    val mustChangePassword: Boolean? = false
)
