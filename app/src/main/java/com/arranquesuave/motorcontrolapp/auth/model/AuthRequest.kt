package com.arranquesuave.motorcontrolapp.auth.model

data class AuthRequest(
    val email: String,
    val password: String,
    val confirm: String? = null
)
