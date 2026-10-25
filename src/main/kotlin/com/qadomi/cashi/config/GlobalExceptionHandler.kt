package com.qadomi.cashi.config

import com.qadomi.cashi.data.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // 1. Validation Errors (DTO level)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors)
    }

    // 2. Validation Errors (request params)
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleMethodValidation(ex: HandlerMethodValidationException): ResponseEntity<ErrorResponse> {
        val errors = ex.parameterValidationResults.associate { result ->
            (result.methodParameter.parameterName ?: "param") to
                (result.resolvableErrors.firstOrNull()?.defaultMessage ?: "Invalid value")
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors)
    }

    // 3. Data Type Mismatch / Malformed JSON
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request or invalid data types")
    }

    // 4. Manual ResponseStatusExceptions
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        return buildResponse(HttpStatus.valueOf(ex.statusCode.value()), ex.reason ?: "Error occurred")
    }

    // 5. Catch-All (The Safety Net)
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", ex)
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred on our server")
    }

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        errors: Map<String, String>? = null
    ): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = status.value(),
            message = message,
            errors = errors,
        )
        return ResponseEntity.status(status).body(response)
    }
}
