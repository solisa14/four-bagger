package com.github.solisa14.fourbagger.api.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service class for JWT (JSON Web Token) operations.
 *
 * <p>This service provides functionality to generate, validate, and extract information from JWT
 * tokens used for authentication in the application. It uses the HS256 algorithm for token signing
 * and verification with a configurable secret key and expiration time.
 */
@Service
public class JwtService {

  private final JwtProperties jwtProperties;

  public JwtService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  /**
   * Generates a new JWT for the provided user.
   *
   * @param userDetails the user to generate the token for
   * @return signed JWT string
   */
  public String generateToken(UserDetails userDetails) {
    return buildToken(new HashMap<>(), userDetails, jwtProperties.getExpirationMs());
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

  /**
   * Extracts a specific claim from the JWT token using a claims resolver function.
   *
   * @param <T> the type of the claim to extract
   * @param token the JWT string
   * @param claimsResolver function to extract the specific claim from Claims object
   * @return the extracted claim value
   */
  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /**
   * Builds a JWT token with the specified claims, user details, and expiration time.
   *
   * @param extraClaims additional claims to include in the token
   * @param userDetails the user for whom the token is being created
   * @param expiration the token expiration time in milliseconds
   * @return the signed JWT token string
   */
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

  /**
   * Checks if the JWT token has expired.
   *
   * @param token the JWT string to check
   * @return true if the token has expired, false otherwise
   */
  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /**
   * Extracts the expiration date from the JWT token.
   *
   * @param token the JWT string
   * @return the token's expiration date
   */
  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * Extracts and verifies all claims from the JWT token.
   *
   * @param token the JWT string to parse
   * @return the verified Claims object containing all token claims
   */
  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
  }

  /**
   * Creates a SecretKey from the configured JWT secret key for token signing and verification.
   *
   * @return the HMAC-SHA key for JWT operations
   */
  private SecretKey getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * Returns the configured JWT expiration time in milliseconds.
   *
   * @return expiration time in milliseconds
   */
  public long getExpirationTime() {
    return jwtProperties.getExpirationMs();
  }
}
