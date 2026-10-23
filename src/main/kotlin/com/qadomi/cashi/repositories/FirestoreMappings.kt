package com.qadomi.cashi.repositories

import com.google.cloud.Timestamp
import java.time.Instant

internal fun Instant.toTimestamp(): Timestamp = Timestamp.ofTimeSecondsAndNanos(epochSecond, nano)

internal fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanos.toLong())

internal object Collections {
    const val USERS = "users"
    const val USER_EMAILS = "user_emails" // email -> userId, guarantees unique emails
    const val REFRESH_TOKENS = "refresh_tokens"
    const val PAYMENTS = "payments" // subcollection of users/{userId}
}
