package com.github.solisa14.fourbagger.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service for generating, validating, and extracting information from JSON Web Tokens (JWT).
 *
 * <p>Uses HMAC-SHA256 encryption to sign tokens. The secret key and expiration time are managed via
 * application configuration.
 */
@Service
public class JwtService {

  private final String secretKey;
  private final long jwtExpiration;

  public JwtService(
      @Value("${spring.application.security.jwt.secret-key}") String secretKey,
      @Value("${spring.application.security.jwt.expiration-ms}") long jwtExpiration) {
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
    return buildToken(new HashMap<>(), userDetails, jwtExpiration);
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
   * Validates if the token belongs to the user and is not expired.
   *
   * @param token the JWT string
   * @param userDetails the user to validate against
   * @return true if valid, false otherwise
   */
  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
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
