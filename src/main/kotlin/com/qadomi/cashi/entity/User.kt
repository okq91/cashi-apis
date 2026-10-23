package com.qadomi.cashi.entity

import java.time.Instant

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val passwordHash: String,
    val createdAt: Instant = Instant.now()
)
