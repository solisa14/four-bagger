package com.github.solisa14.fourbagger.api.security.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration for in-process rate limits on auth and join-code endpoints.
 *
 * @param enabled whether rate limiting is active
 * @param auth limit applied to register/login/refresh-token
 * @param joinCode limit applied to tournament join-code lookup and join
 * @param trustedProxies peer addresses whose forwarded client IP header is trusted
 */
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        @Valid @NotNull Budget auth,
        @Valid @NotNull Budget joinCode,
        List<String> trustedProxies) {

    public RateLimitProperties {
        trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
    }

    /**
     * A request budget for a fixed time window.
     *
     * @param requests maximum requests allowed in the window
     * @param windowSeconds length of the window in seconds
     */
    public record Budget(@Positive int requests, @Positive int windowSeconds) {}
}
