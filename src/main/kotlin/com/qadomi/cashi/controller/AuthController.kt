package com.qadomi.cashi.controller

import com.qadomi.cashi.dto.AuthResponseDTO
import com.qadomi.cashi.dto.LoginRequestDTO
import com.qadomi.cashi.dto.RefreshTokenRequest
import com.qadomi.cashi.dto.RegisterRequestDTO
import com.qadomi.cashi.services.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    //call this API to register a new user,
    //it will be called from postman as we don't have registration flow in the demo app
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequestDTO): ResponseEntity<AuthResponseDTO> {
        val authResponse = authService.registerUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse)
    }

    //call this API to get a new access token,
    //it will be called in the splash screen with static user credentials as we don't have login screen in the demo app
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequestDTO): ResponseEntity<AuthResponseDTO> {
        val authResponse = authService.authenticateLocal(request)
        return ResponseEntity.ok(authResponse)
    }

    //to refresh the JWT token after expire
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponseDTO> {
        val response = authService.refreshToken(request.refreshToken)
        return ResponseEntity.ok(response)
    }
}
