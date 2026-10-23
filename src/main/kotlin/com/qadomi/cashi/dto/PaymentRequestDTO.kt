package com.qadomi.cashi.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class PaymentRequestDTO(
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @field:Digits(integer = 12, fraction = 2, message = "Amount can have at most 2 decimal places")
    val amount: BigDecimal?,

    @field:NotBlank(message = "Currency is required")
    @field:Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code, e.g. USD")
    val currency: String,

    @field:NotBlank(message = "Recipient email is required")
    @field:Email(message = "Invalid recipient email")
    @field:Size(max = 254, message = "Recipient email must be at most 254 characters")
    val recipientEmail: String,

    @field:Size(max = 255, message = "Description must be at most 255 characters")
    val description: String? = null
)
