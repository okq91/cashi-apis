package com.qadomi.cashi.services

import com.qadomi.cashi.config.JwtTokenProvider
import com.qadomi.cashi.config.RefreshTokenGenerator
import com.qadomi.cashi.dto.AuthResponseDTO
import com.qadomi.cashi.dto.LoginRequestDTO
import com.qadomi.cashi.dto.RegisterRequestDTO
import com.qadomi.cashi.entity.RefreshToken
import com.qadomi.cashi.entity.User
import com.qadomi.cashi.mappers.toResponse
import com.qadomi.cashi.repositories.RefreshTokenRepository
import com.qadomi.cashi.repositories.UserRepository
import com.qadomi.cashi.utils.ShortIdGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenGenerator: RefreshTokenGenerator,
    @Value("\${security.jwt.refresh-expiration}") private val refreshExpirationMs: Long
) {

    fun registerUser(request: RegisterRequestDTO): AuthResponseDTO {
        val user = User(
            id = ShortIdGenerator.generate(),
            email = request.email.trim().lowercase(),
            fullName = request.fullName.trim(),
            passwordHash = passwordEncoder.encode(request.password)
        )

        if (!userRepository.create(user)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered")
        }

        return issueTokens(user)
    }

    fun authenticateLocal(request: LoginRequestDTO): AuthResponseDTO {
        val user = userRepository.findByEmail(request.email.trim().lowercase())
        // Same message for unknown email and wrong password, so accounts can't be enumerated
        if (user == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
        }

        return issueTokens(user)
    }

    fun refreshToken(requestToken: String): AuthResponseDTO {
        // 1. Find token
        val tokenHash = refreshTokenGenerator.hash(requestToken)
        val token = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")

        // 2. Rotate: a refresh token can be used only once
        refreshTokenRepository.deleteByTokenHash(tokenHash)

        // 3. Check if expired
        if (token.expiryDate < Instant.now()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired. Please login again")
        }

        val user = userRepository.findById(token.userId)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")

        return issueTokens(user)
    }

    private fun issueTokens(user: User): AuthResponseDTO {
        val accessToken = jwtTokenProvider.generateToken(user)
        val refreshToken = createAndSaveRefreshToken(user)

        return AuthResponseDTO(
            user = user.toResponse(),
            accessToken = accessToken,
            expiresIn = jwtTokenProvider.accessTokenExpirationMs / 1000,
            refreshToken = refreshToken
        )
    }

    private fun createAndSaveRefreshToken(user: User): String {
        val tokenValue = refreshTokenGenerator.generate()
        refreshTokenRepository.save(
            RefreshToken(
                tokenHash = refreshTokenGenerator.hash(tokenValue),
                userId = user.id,
                expiryDate = Instant.now().plusMillis(refreshExpirationMs)
            )
        )
        return tokenValue
    }
}
