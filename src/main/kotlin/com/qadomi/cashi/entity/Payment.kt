package com.qadomi.cashi.entity

import java.math.BigDecimal
import java.time.Instant

data class Payment(
    val id: String,
    val userId: String,
    val amount: BigDecimal,
    val currency: String,
    val recipientEmail: String,
    val description: String?,
    val createdAt: Instant
)
