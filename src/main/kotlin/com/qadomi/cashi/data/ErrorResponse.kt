package com.qadomi.cashi.data

data class ErrorResponse(
    val status: Int,
    val message: String,
    val errors: Map<String, String>? = null // Only populated for validation errors
)
