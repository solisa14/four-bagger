package com.github.solisa14.fourbagger.api.auth;

/**
 * Record representing a pair of access and refresh tokens.
 *
 * @param accessToken the JWT access token
 * @param refreshToken the JWT refresh token
 */
public record TokenPair(String accessToken, String refreshToken) {}
