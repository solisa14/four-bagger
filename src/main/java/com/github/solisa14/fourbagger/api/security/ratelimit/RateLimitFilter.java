package com.github.solisa14.fourbagger.api.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.solisa14.fourbagger.api.common.exception.ErrorResponse;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Rejects excess requests to authentication and join-code endpoints with HTTP 429.
 *
 * <p>Limits are keyed by client IP. The {@code X-Forwarded-For} header is used only when the
 * direct peer is configured as a trusted proxy.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

  private static final String AUTH_BUCKET_TYPE = "auth";
  private static final String JOIN_CODE_BUCKET_TYPE = "join-code";

  private static final long MAXIMUM_BUCKETS = 10_000;

  private final RateLimitProperties properties;
  private final ObjectMapper objectMapper;
  private final Cache<String, Bucket> buckets =
      Caffeine.newBuilder()
          .maximumSize(MAXIMUM_BUCKETS)
          .expireAfterAccess(Duration.ofMinutes(5))
          .build();

  /**
   * Constructs a RateLimitFilter.
   *
   * @param properties configured budgets
   * @param objectMapper JSON mapper for error responses
   */
  public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    return !properties.enabled() || resolveBucketType(request) == null;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String bucketType = resolveBucketType(request);
    RateLimitProperties.Budget budget =
        AUTH_BUCKET_TYPE.equals(bucketType) ? properties.auth() : properties.joinCode();
    String key = clientIp(request) + ":" + bucketType;

    ConsumptionProbe probe = getBucket(key, budget).tryConsumeAndReturnRemaining(1);
    if (!probe.isConsumed()) {
      writeTooManyRequests(response, probe);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private static String resolveBucketType(HttpServletRequest request) {
    String method = request.getMethod();
    String path = request.getRequestURI();

    if ("POST".equalsIgnoreCase(method)
        && (path.equals("/api/v1/auth/register")
            || path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/refresh-token"))) {
      return AUTH_BUCKET_TYPE;
    }
    if ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/tournaments/join")) {
      return JOIN_CODE_BUCKET_TYPE;
    }
    if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/v1/tournaments/join-code/")) {
      return JOIN_CODE_BUCKET_TYPE;
    }
    return null;
  }

  private Bucket getBucket(String key, RateLimitProperties.Budget budget) {
    return buckets.get(
        key,
        ignored ->
            Bucket.builder()
                .addLimit(
                    limit ->
                        limit
                            .capacity(budget.requests())
                            .refillGreedy(
                                budget.requests(), Duration.ofSeconds(budget.windowSeconds())))
                .build());
  }

  private String clientIp(HttpServletRequest request) {
    String remote = request.getRemoteAddr();
    if (!properties.trustedProxies().contains(remote)) {
      return remote == null || remote.isBlank() ? "unknown" : remote;
    }

    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded == null || forwarded.isBlank()) {
      return remote;
    }

    String[] addresses = forwarded.split(",");
    for (int index = addresses.length - 1; index >= 0; index--) {
      String address = addresses[index].trim();
      if (!address.isBlank() && !properties.trustedProxies().contains(address)) {
        return address;
      }
    }
    return remote;
  }

  private void writeTooManyRequests(HttpServletResponse response, ConsumptionProbe probe)
      throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader("Retry-After", Long.toString(secondsUntilRefill(probe)));
    objectMapper.writeValue(
        response.getOutputStream(),
        new ErrorResponse(
            Instant.now(), HttpStatus.TOO_MANY_REQUESTS.value(), "Too many requests"));
  }

  private static long secondsUntilRefill(ConsumptionProbe probe) {
    return Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
  }

  void clearBuckets() {
    buckets.invalidateAll();
  }
}
