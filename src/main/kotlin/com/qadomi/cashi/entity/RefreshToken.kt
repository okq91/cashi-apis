package com.qadomi.cashi.entity

import java.time.Instant

data class RefreshToken(
    val tokenHash: String, // SHA-256 of the token; the raw token is never stored
    val userId: String,
    val expiryDate: Instant
)
