package com.qadomi.cashi.mappers

import com.qadomi.cashi.dto.PaymentResponseDTO
import com.qadomi.cashi.entity.Payment

fun Payment.toResponse() = PaymentResponseDTO(
    id = this.id,
    amount = this.amount,
    currency = this.currency,
    recipientEmail = this.recipientEmail,
    description = this.description,
    createdAt = this.createdAt
)
