package com.qadomi.cashi.dto

data class AuthResponseDTO(
    val user: UserProfileDTO,
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val refreshToken: String
)
