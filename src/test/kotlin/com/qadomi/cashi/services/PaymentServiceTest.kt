package com.qadomi.cashi.services

import com.qadomi.cashi.dto.PaymentRequestDTO
import com.qadomi.cashi.entity.Payment
import com.qadomi.cashi.repositories.PaymentRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaymentServiceTest {

    private val paymentRepository = mockk<PaymentRepository>()
    private val service = PaymentService(paymentRepository)

    private fun stubSave(): io.mockk.CapturingSlot<Payment> {
        val saved = slot<Payment>()
        every { paymentRepository.save(capture(saved)) } answers { saved.captured }
        return saved
    }

    @Test
    fun `addPayment saves a payment owned by the caller and returns it`() {
        val saved = stubSave()
        val before = Instant.now()

        val response = service.addPayment(
            "user-1",
            PaymentRequestDTO(BigDecimal("25.50"), "USD", "john@example.com", "Lunch")
        )

        assertEquals("user-1", saved.captured.userId)
        assertEquals(BigDecimal("25.50"), saved.captured.amount)
        assertEquals("USD", saved.captured.currency)
        assertEquals("john@example.com", saved.captured.recipientEmail)
        assertEquals("Lunch", saved.captured.description)
        assertTrue(saved.captured.id.isNotBlank())
        assertTrue(!saved.captured.createdAt.isBefore(before))

        assertEquals(saved.captured.id, response.id)
        assertEquals(BigDecimal("25.50"), response.amount)
        assertEquals("USD", response.currency)
        assertEquals("john@example.com", response.recipientEmail)
        assertEquals("Lunch", response.description)
        assertEquals(saved.captured.createdAt, response.createdAt)
    }

    @Test
    fun `addPayment normalizes the recipient email`() {
        val saved = stubSave()

        val response = service.addPayment(
            "user-1",
            PaymentRequestDTO(BigDecimal("1.00"), "USD", "  John@Example.COM ")
        )

        assertEquals("john@example.com", saved.captured.recipientEmail)
        assertEquals("john@example.com", response.recipientEmail)
    }

    @Test
    fun `addPayment trims the description`() {
        val saved = stubSave()

        service.addPayment("user-1", PaymentRequestDTO(BigDecimal("1.00"), "USD", "john@example.com", "  Lunch  "))

        assertEquals("Lunch", saved.captured.description)
    }

    @Test
    fun `addPayment stores a blank description as null`() {
        val saved = stubSave()

        val response = service.addPayment("user-1", PaymentRequestDTO(BigDecimal("1.00"), "USD", "john@example.com", "   "))

        assertNull(saved.captured.description)
        assertNull(response.description)
    }

    @Test
    fun `addPayment keeps a missing description as null`() {
        val saved = stubSave()

        service.addPayment("user-1", PaymentRequestDTO(BigDecimal("1.00"), "USD", "john@example.com"))

        assertNull(saved.captured.description)
    }

    @Test
    fun `addPayment generates a distinct id for each payment`() {
        val ids = mutableListOf<String>()
        every { paymentRepository.save(any()) } answers { firstArg<Payment>().also { ids += it.id } }

        repeat(2) { service.addPayment("user-1", PaymentRequestDTO(BigDecimal("1.00"), "USD", "john@example.com")) }

        assertNotEquals(ids[0], ids[1])
    }

    private fun payment(n: Int, description: String? = null) = Payment(
        id = "p$n",
        userId = "user-1",
        amount = BigDecimal("$n.00"),
        currency = "USD",
        recipientEmail = "r$n@example.com",
        description = description,
        createdAt = Instant.parse("2026-01-01T00:00:00Z").plusSeconds(n.toLong())
    )

    @Test
    fun `getPaymentHistory maps a short page and has no next cursor`() {
        val payments = listOf(payment(2), payment(1, "Rent"))
        every { paymentRepository.findByUserId("user-1", 11, null) } returns payments

        val page = service.getPaymentHistory("user-1", null)

        assertEquals(listOf("p2", "p1"), page.items.map { it.id })
        assertEquals(BigDecimal("1.00"), page.items[1].amount)
        assertEquals("USD", page.items[1].currency)
        assertEquals("r1@example.com", page.items[1].recipientEmail)
        assertEquals("Rent", page.items[1].description)
        assertEquals(payments[1].createdAt, page.items[1].createdAt)
        assertNull(page.nextCursor)
    }

    @Test
    fun `getPaymentHistory returns an empty page when there are no payments`() {
        every { paymentRepository.findByUserId("user-1", 11, null) } returns emptyList()

        val page = service.getPaymentHistory("user-1", null)

        assertTrue(page.items.isEmpty())
        assertNull(page.nextCursor)
    }

    @Test
    fun `getPaymentHistory returns exactly one full page without a next cursor`() {
        every { paymentRepository.findByUserId("user-1", 11, null) } returns (10 downTo 1).map { payment(it) }

        val page = service.getPaymentHistory("user-1", null)

        assertEquals(10, page.items.size)
        assertNull(page.nextCursor)
    }

    @Test
    fun `getPaymentHistory trims the extra row and points the cursor at the last item`() {
        every { paymentRepository.findByUserId("user-1", 11, null) } returns (11 downTo 1).map { payment(it) }

        val page = service.getPaymentHistory("user-1", null)

        assertEquals(10, page.items.size)
        assertEquals("p11", page.items.first().id)
        assertEquals("p2", page.items.last().id)
        assertEquals("p2", page.nextCursor)
    }

    @Test
    fun `getPaymentHistory passes the cursor to the repository`() {
        every { paymentRepository.findByUserId("user-1", 11, "p2") } returns listOf(payment(1))

        val page = service.getPaymentHistory("user-1", "p2")

        assertEquals(listOf("p1"), page.items.map { it.id })
        verify { paymentRepository.findByUserId("user-1", 11, "p2") }
    }

    @Test
    fun `getPaymentHistory treats a blank cursor as the first page`() {
        every { paymentRepository.findByUserId("user-1", 11, null) } returns emptyList()

        service.getPaymentHistory("user-1", "  ")

        verify { paymentRepository.findByUserId("user-1", 11, null) }
    }

    @Test
    fun `getPaymentHistory rejects an unknown cursor with 400`() {
        every { paymentRepository.findByUserId("user-1", 11, "nope") } throws IllegalArgumentException("Unknown cursor")

        val ex = assertFailsWith<ResponseStatusException> { service.getPaymentHistory("user-1", "nope") }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        assertEquals("Invalid cursor", ex.reason)
    }
}
