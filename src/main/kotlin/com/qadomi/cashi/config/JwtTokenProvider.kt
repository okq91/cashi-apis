package com.qadomi.cashi.config

import com.qadomi.cashi.entity.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Date

@Component
class JwtTokenProvider(
    @Value("\${security.jwt.secret}") secretKey: String,
    @Value("\${security.jwt.expiration}") private val expirationMs: Long
) {

    private val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

    val accessTokenExpirationMs: Long get() = expirationMs

    fun generateToken(user: User): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(user.id)
            .claim("email", user.email)
            .claim("type", ACCESS_TOKEN_TYPE)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            getClaims(token)["type"] == ACCESS_TOKEN_TYPE
        } catch (e: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): String {
        return getClaims(token).subject
    }

    fun getEmailFromToken(token: String): String {
        return getClaims(token)["email"] as String
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    companion object {
        private const val ACCESS_TOKEN_TYPE = "access"
    }
}

// Refresh tokens are opaque random strings; only their SHA-256 hash is stored.
@Component
class RefreshTokenGenerator {

    private val random = SecureRandom()

    fun generate(): String {
        val bytes = ByteArray(48)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
