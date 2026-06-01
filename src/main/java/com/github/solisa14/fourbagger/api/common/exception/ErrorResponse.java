package com.github.solisa14.fourbagger.api.common.exception;

import java.time.Instant;

/**
 * Immutable error response structure returned to clients when requests fail.
 *
 * <p>Provides consistent error formatting across all API endpoints with timestamp, status code, and
 * human-readable message.
 *
 * @param timestamp exact time when the error was generated
 * @param status HTTP status code representing the error type
 * @param message descriptive error message explaining what went wrong
 */
public record ErrorResponse(Instant timestamp, int status, String message) {}
