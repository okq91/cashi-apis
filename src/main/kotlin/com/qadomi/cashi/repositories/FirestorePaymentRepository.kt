package com.qadomi.cashi.repositories

import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.qadomi.cashi.entity.Payment
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class FirestorePaymentRepository(private val firestore: Firestore) : PaymentRepository {

    // Payments live under users/{userId}/payments, so history queries need no composite index
    private fun payments(userId: String) =
        firestore.collection(Collections.USERS).document(userId).collection(Collections.PAYMENTS)

    override fun save(payment: Payment): Payment {
        payments(payment.userId).document(payment.id).set(
            mapOf(
                "userId" to payment.userId,
                // Stored as a string to keep exact decimal precision
                "amount" to payment.amount.toPlainString(),
                "currency" to payment.currency,
                "recipientEmail" to payment.recipientEmail,
                "description" to payment.description,
                "createdAt" to payment.createdAt.toTimestamp()
            )
        ).get()
        return payment
    }

    override fun findByUserId(userId: String, limit: Int, afterId: String?): List<Payment> {
        var query = payments(userId).orderBy("createdAt", Query.Direction.DESCENDING)
        if (afterId != null) {
            // Cursor pagination: continue after the last document of the previous page (no skipped reads)
            val cursor = payments(userId).document(afterId).get().get()
            require(cursor.exists()) { "Unknown cursor" }
            query = query.startAfter(cursor)
        }
        return query
            .limit(limit)
            .get()
            .get()
            .documents
            .map { it.toPayment() }
    }

    private fun DocumentSnapshot.toPayment() = Payment(
        id = id,
        userId = getString("userId")!!,
        amount = BigDecimal(getString("amount")!!),
        currency = getString("currency")!!,
        recipientEmail = getString("recipientEmail")!!,
        description = getString("description"),
        createdAt = getTimestamp("createdAt")!!.toInstant()
    )
}
