package com.arranquesuave.motorcontrolapp.auth.model

data class User(
    val id: Int,
    val email: String,
    val name: String? = null,
    val role: String? = null
)
