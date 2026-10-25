package com.qadomi.cashi.repositories

import com.google.cloud.firestore.Firestore
import com.qadomi.cashi.entity.RefreshToken
import org.springframework.stereotype.Repository

@Repository
class FirestoreRefreshTokenRepository(private val firestore: Firestore) : RefreshTokenRepository {

    private val tokens get() = firestore.collection(Collections.REFRESH_TOKENS)

    override fun save(refreshToken: RefreshToken) {
        tokens.document(refreshToken.tokenHash).set(
            mapOf(
                "userId" to refreshToken.userId,
                "expiryDate" to refreshToken.expiryDate.toTimestamp()
            )
        ).get()
    }

    override fun findByTokenHash(tokenHash: String): RefreshToken? {
        val snapshot = tokens.document(tokenHash).get().get()
        if (!snapshot.exists()) return null
        return RefreshToken(
            tokenHash = snapshot.id,
            userId = snapshot.getString("userId")!!,
            expiryDate = snapshot.getTimestamp("expiryDate")!!.toInstant()
        )
    }

    override fun deleteByTokenHash(tokenHash: String) {
        tokens.document(tokenHash).delete().get()
    }
}
