package com.github.solisa14.fourbagger.api.auth;

/**
 * Response payload containing authentication tokens.
 *
 * @param accessToken the short-lived JWT access token for API requests
 * @param refreshToken the long-lived refresh token for obtaining new access tokens
 */
public record AuthenticationResponse(String accessToken, String refreshToken) {}
