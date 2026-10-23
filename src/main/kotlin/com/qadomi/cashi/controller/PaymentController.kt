package com.qadomi.cashi.controller

import com.qadomi.cashi.dto.PaymentPageDTO
import com.qadomi.cashi.dto.PaymentRequestDTO
import com.qadomi.cashi.dto.PaymentResponseDTO
import com.qadomi.cashi.services.PaymentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments")
class PaymentController(private val paymentService: PaymentService) {

    @PostMapping
    fun addPayment(
        @AuthenticationPrincipal userId: String,
        @Valid @RequestBody request: PaymentRequestDTO
    ): ResponseEntity<PaymentResponseDTO> {
        val payment = paymentService.addPayment(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(payment)
    }

    @GetMapping
    fun getPaymentHistory(
        @AuthenticationPrincipal userId: String,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<PaymentPageDTO> {
        return ResponseEntity.ok(paymentService.getPaymentHistory(userId, cursor))
    }
}
