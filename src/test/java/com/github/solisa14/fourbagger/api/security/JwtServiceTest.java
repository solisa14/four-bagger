package com.github.solisa14.fourbagger.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

  private JwtService jwtService;
  private String secretKey = "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=";
  private long expiration = 3600000; // 1 hour

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(secretKey, expiration);
  }

  @Test
  void generateToken_shouldGenerateValidToken() {
    UserDetails userDetails = mock(UserDetails.class);
    when(userDetails.getUsername()).thenReturn("testuser");

    String token = jwtService.generateToken(userDetails);

    assertThat(token).isNotNull();
    assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
    assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
  }

  @Test
  void isTokenValid_shouldReturnFalseForInvalidUser() {
    UserDetails userDetails = mock(UserDetails.class);
    when(userDetails.getUsername()).thenReturn("testuser");

    UserDetails otherUser = mock(UserDetails.class);
    when(otherUser.getUsername()).thenReturn("otheruser");

    String token = jwtService.generateToken(userDetails);

    assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
  }

  @Test
  void getExpirationTime_shouldReturnConfiguredExpiration() {
    assertThat(jwtService.getExpirationTime()).isEqualTo(expiration);
  }
}
