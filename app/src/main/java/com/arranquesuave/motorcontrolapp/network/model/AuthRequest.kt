package com.arranquesuave.motorcontrolapp.network.model

data class AuthRequest(val email: String, val password: String, val confirm: String? = null)

