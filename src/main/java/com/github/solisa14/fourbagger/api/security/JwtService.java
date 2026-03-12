package com.github.solisa14.fourbagger.api.security;

import com.github.solisa14.fourbagger.api.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.*;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service for generating, validating, and extracting information from JSON Web Tokens (JWT).
 *
 * <p>Uses HMAC-SHA256 signing to issue tokens. The secret key and expiration time are managed via
 * application configuration.
 */
@Service
public class JwtService {

  private final String secretKey;
  private final long jwtExpiration;

  /**
   * Constructs a JwtService.
   *
   * @param secretKey the secret key used for signing JWTs
   * @param jwtExpiration the expiration time for JWTs in milliseconds
   */
  public JwtService(
      @Value("${app.security.jwt.secret-key}") String secretKey,
      @Value("${app.security.jwt.expiration-ms}") long jwtExpiration) {
    this.secretKey = secretKey;
    this.jwtExpiration = jwtExpiration;
  }

  /**
   * Generates a new JWT for the provided user.
   *
   * @param userDetails the user to generate the token for
   * @return signed JWT string
   */
  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();

    if (userDetails instanceof User) {
      claims.put("role", ((User) userDetails).getRole().name());
    } else {
      var authority = userDetails.getAuthorities().stream().findFirst();
      authority.ifPresent(
          grantedAuthority ->
              claims.put(
                  "role",
                  Objects.requireNonNull(grantedAuthority.getAuthority()).replace("ROLE_", "")));
    }
    return buildToken(claims, userDetails, jwtExpiration);
  }

  /**
   * Extracts the username (subject) from the JWT.
   *
   * @param token the JWT string
   * @return the username stored in the token
   */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * Validates whether the token is well-formed and not expired.
   *
   * @param token the JWT string
   * @return true if valid, false otherwise
   */
  public boolean isTokenValid(String token) {
    try {
      return !isTokenExpired(token);
    } catch (io.jsonwebtoken.JwtException e) {
      return false;
    }
  }

  /**
   * Extracts the user's authorities (roles) from the JWT.
   *
   * @param token the JWT string
   * @return a list of {@link SimpleGrantedAuthority} representing the user's roles
   */
  public List<SimpleGrantedAuthority> extractAuthorities(String token) {
    String role = extractClaim(token, claims -> claims.get("role", String.class));
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private String buildToken(
      Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
    return Jwts.builder()
        .claims(extraClaims)
        .subject(userDetails.getUsername())
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSignInKey(), Jwts.SIG.HS256)
        .compact();
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * Returns the configured JWT expiration time in milliseconds.
   *
   * @return expiration time in milliseconds
   */
  public long getExpirationTime() {
    return jwtExpiration;
  }
}
