package com.qadomi.cashi.repositories

import com.qadomi.cashi.entity.Payment

interface PaymentRepository {
    fun save(payment: Payment): Payment

    /**
     * Newest first. Returns up to [limit] payments that come after the payment with id [afterId],
     * or from the start when [afterId] is null.
     *
     * @throws IllegalArgumentException if [afterId] does not match a payment of this user
     */
    fun findByUserId(userId: String, limit: Int, afterId: String? = null): List<Payment>
}
