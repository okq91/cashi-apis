package com.qadomi.cashi.dto

import java.math.BigDecimal
import java.time.Instant

data class PaymentResponseDTO(
    val id: String,
    val amount: BigDecimal,
    val currency: String,
    val recipientEmail: String,
    val description: String?,
    val createdAt: Instant
)
