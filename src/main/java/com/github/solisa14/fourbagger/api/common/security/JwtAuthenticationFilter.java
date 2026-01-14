package com.github.solisa14.fourbagger.api.common.security;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT authentication filter that validates tokens from HTTP cookies.
 *
 * <p>This filter intercepts incoming requests, extracts JWT tokens from the "accessToken" cookie,
 * validates them, and sets the Spring Security authentication context if the token is valid. The
 * filter runs once per request as part of the security filter chain.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
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
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

        if (jwtService.isTokenValid(jwt, userDetails)) {
          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }
    } catch (Exception e) {
      log.debug("JWT Validation failed: {}", e.getMessage());
    }
    filterChain.doFilter(request, response);
  }
}
