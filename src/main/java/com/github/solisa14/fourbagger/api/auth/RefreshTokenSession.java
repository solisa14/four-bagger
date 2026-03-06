package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.user.User;

/** Carries the authenticated user and newly issued raw refresh token value. */
public record RefreshTokenSession(User user, String rawToken) {}
