package com.winsongr.saga.orchestrator.api

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ExceptionAdvice {

    private val log = LoggerFactory.getLogger(ExceptionAdvice::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<Map<String, String>> {
        log.warn("bad request: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to (e.message ?: "Bad request")))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(e: IllegalStateException): ResponseEntity<Map<String, String>> {
        log.warn("conflict: {}", e.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(mapOf("error" to (e.message ?: "Conflict")))
    }

    @ExceptionHandler(Exception::class)
    fun handleInternal(e: Exception): ResponseEntity<Map<String, String>> {
        log.error("internal error", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to "Internal server error"))
    }
}
