package com.qadomi.cashi.services

import com.qadomi.cashi.config.JwtTokenProvider
import com.qadomi.cashi.config.RefreshTokenGenerator
import com.qadomi.cashi.dto.LoginRequestDTO
import com.qadomi.cashi.dto.RegisterRequestDTO
import com.qadomi.cashi.entity.RefreshToken
import com.qadomi.cashi.entity.User
import com.qadomi.cashi.repositories.RefreshTokenRepository
import com.qadomi.cashi.repositories.UserRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val refreshTokenRepository = mockk<RefreshTokenRepository>()
    private val passwordEncoder = BCryptPasswordEncoder(4) // low cost keeps tests fast
    private val jwtTokenProvider = mockk<JwtTokenProvider>()
    private val refreshTokenGenerator = RefreshTokenGenerator()

    private val refreshExpirationMs = 604_800_000L

    private val service = AuthService(
        userRepository,
        refreshTokenRepository,
        passwordEncoder,
        jwtTokenProvider,
        refreshTokenGenerator,
        refreshExpirationMs
    )

    private val user = User(
        id = "user-1",
        email = "test@example.com",
        fullName = "Test User",
        passwordHash = passwordEncoder.encode("password123")
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { jwtTokenProvider.generateToken(any()) } returns "access-token"
        every { jwtTokenProvider.accessTokenExpirationMs } returns 900_000L
        every { refreshTokenRepository.save(any()) } just Runs
        every { refreshTokenRepository.deleteByTokenHash(any()) } just Runs
    }

    // registerUser

    @Test
    fun `registerUser normalizes input, hashes the password and issues tokens`() {
        val saved = slot<User>()
        every { userRepository.create(capture(saved)) } returns true

        val response = service.registerUser(
            RegisterRequestDTO(email = "  Test@Example.COM ", password = "password123", fullName = "  Test User  ")
        )

        assertEquals("test@example.com", saved.captured.email)
        assertEquals("Test User", saved.captured.fullName)
        assertNotEquals("password123", saved.captured.passwordHash)
        assertTrue(passwordEncoder.matches("password123", saved.captured.passwordHash))
        assertEquals("access-token", response.accessToken)
        assertEquals(900L, response.expiresIn)
        assertEquals("Bearer", response.tokenType)
        assertEquals(saved.captured.id, response.user.id)
        assertTrue(response.refreshToken.isNotBlank())
    }

    @Test
    fun `registerUser throws conflict when email already exists and issues no tokens`() {
        every { userRepository.create(any()) } returns false

        val ex = assertFailsWith<ResponseStatusException> {
            service.registerUser(RegisterRequestDTO("test@example.com", "password123", "Test User"))
        }

        assertEquals(HttpStatus.CONFLICT, ex.statusCode)
        verify(exactly = 0) { refreshTokenRepository.save(any()) }
        verify(exactly = 0) { jwtTokenProvider.generateToken(any()) }
    }

    // authenticateLocal

    @Test
    fun `authenticateLocal returns tokens for valid credentials`() {
        every { userRepository.findByEmail("test@example.com") } returns user

        val response = service.authenticateLocal(LoginRequestDTO("  TEST@example.com ", "password123"))

        assertEquals("access-token", response.accessToken)
        assertEquals(user.id, response.user.id)
        verify { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `authenticateLocal rejects a wrong password`() {
        every { userRepository.findByEmail("test@example.com") } returns user

        val ex = assertFailsWith<ResponseStatusException> {
            service.authenticateLocal(LoginRequestDTO("test@example.com", "wrong-password"))
        }

        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
        assertEquals("Invalid email or password", ex.reason)
        verify(exactly = 0) { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `authenticateLocal uses the same error for an unknown email`() {
        every { userRepository.findByEmail(any()) } returns null

        val ex = assertFailsWith<ResponseStatusException> {
            service.authenticateLocal(LoginRequestDTO("nobody@example.com", "password123"))
        }

        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
        assertEquals("Invalid email or password", ex.reason)
    }

    // refreshToken

    @Test
    fun `refreshToken rotates the token and issues a new pair`() {
        val tokenHash = refreshTokenGenerator.hash("old-token")
        every { refreshTokenRepository.findByTokenHash(tokenHash) } returns
            RefreshToken(tokenHash, user.id, Instant.now().plusSeconds(60))
        every { userRepository.findById(user.id) } returns user

        val response = service.refreshToken("old-token")

        verify { refreshTokenRepository.deleteByTokenHash(tokenHash) }
        assertEquals("access-token", response.accessToken)
        assertNotEquals("old-token", response.refreshToken)
        verify { refreshTokenRepository.save(match { it.tokenHash == refreshTokenGenerator.hash(response.refreshToken) }) }
    }

    @Test
    fun `refreshToken rejects an unknown token`() {
        every { refreshTokenRepository.findByTokenHash(any()) } returns null

        val ex = assertFailsWith<ResponseStatusException> { service.refreshToken("unknown") }

        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
        assertEquals("Invalid refresh token", ex.reason)
        verify(exactly = 0) { refreshTokenRepository.deleteByTokenHash(any()) }
    }

    @Test
    fun `refreshToken rejects and deletes an expired token`() {
        val tokenHash = refreshTokenGenerator.hash("expired-token")
        every { refreshTokenRepository.findByTokenHash(tokenHash) } returns
            RefreshToken(tokenHash, user.id, Instant.now().minusSeconds(1))

        val ex = assertFailsWith<ResponseStatusException> { service.refreshToken("expired-token") }

        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
        verify { refreshTokenRepository.deleteByTokenHash(tokenHash) }
        verify(exactly = 0) { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `refreshToken rejects a token whose user no longer exists`() {
        val tokenHash = refreshTokenGenerator.hash("orphan-token")
        every { refreshTokenRepository.findByTokenHash(tokenHash) } returns
            RefreshToken(tokenHash, "deleted-user", Instant.now().plusSeconds(60))
        every { userRepository.findById("deleted-user") } returns null

        val ex = assertFailsWith<ResponseStatusException> { service.refreshToken("orphan-token") }

        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
        verify(exactly = 0) { refreshTokenRepository.save(any()) }
    }
}
