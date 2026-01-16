package com.github.solisa14.fourbagger.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that validates JWT tokens on incoming HTTP requests.
 *
 * <p>Intercepts requests, extracts the JWT from the "accessToken" cookie, validates it, and sets
 * the user authentication in the Spring Security context if the token is valid.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    final String jwt;
    final String username;

    Optional<Cookie> jwtCookie = Optional.empty();
    if (request.getCookies() != null) {
      jwtCookie =
          Arrays.stream(request.getCookies())
              .filter(cookie -> "accessToken".equals(cookie.getName()))
              .findFirst();
    }

    if (jwtCookie.isEmpty()) {
      filterChain.doFilter(request, response);
      return;
    }

    jwt = jwtCookie.get().getValue();
    try {
      username = jwtService.extractUsername(jwt);

      if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        if (jwtService.isTokenValid(jwt)) {
          var authorities = jwtService.extractAuthorities(jwt);

          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(username, null, authorities);

          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (Exception e) {
      log.debug("JWT Validation failed: {}", e.getMessage());
      SecurityContextHolder.clearContext();
    }
    filterChain.doFilter(request, response);
  }
}
