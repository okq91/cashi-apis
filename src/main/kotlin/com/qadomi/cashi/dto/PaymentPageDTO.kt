package com.qadomi.cashi.dto

data class PaymentPageDTO(
    val items: List<PaymentResponseDTO>,
    /** Pass as `cursor` to fetch the next page; null on the last page. */
    val nextCursor: String?
)
