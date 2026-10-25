package com.qadomi.cashi.services

import com.qadomi.cashi.dto.PaymentPageDTO
import com.qadomi.cashi.dto.PaymentRequestDTO
import com.qadomi.cashi.dto.PaymentResponseDTO
import com.qadomi.cashi.entity.Payment
import com.qadomi.cashi.mappers.toResponse
import com.qadomi.cashi.repositories.PaymentRepository
import com.qadomi.cashi.utils.ShortIdGenerator
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository
) {

    fun addPayment(userId: String, request: PaymentRequestDTO): PaymentResponseDTO {
        val payment = Payment(
            id = ShortIdGenerator.generate(),
            userId = userId,
            amount = request.amount!!,
            currency = request.currency,
            recipientEmail = request.recipientEmail.trim().lowercase(),
            description = request.description?.trim()?.ifBlank { null },
            createdAt = Instant.now()
        )
        return paymentRepository.save(payment).toResponse()
    }

    fun getPaymentHistory(userId: String, cursor: String?): PaymentPageDTO {
        // Fetch one extra row to know whether another page exists
        val payments = try {
            paymentRepository.findByUserId(userId, PAGE_SIZE + 1, cursor?.ifBlank { null })
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor")
        }
        val page = payments.take(PAGE_SIZE)
        return PaymentPageDTO(
            items = page.map { it.toResponse() },
            nextCursor = if (payments.size > PAGE_SIZE) page.last().id else null
        )
    }

    companion object {
        const val PAGE_SIZE = 10
    }
}
