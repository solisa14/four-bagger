package com.github.solisa14.fourbagger.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

  private static final String SECRET_KEY = "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=";

  @Test
  void generateToken_whenPrincipalIsUser_includesRoleAndValidates() {
    JwtService jwtService = new JwtService(SECRET_KEY, 3600000L);
    User user =
        TestDataFactory.user(
            UUID.randomUUID(), "user1", "user1@example.com", "encoded", Role.ADMIN);

    String token = jwtService.generateToken(user);

    assertThat(jwtService.extractUsername(token)).isEqualTo("user1");
    assertThat(jwtService.extractAuthorities(token))
        .containsExactly(new SimpleGrantedAuthority("ROLE_ADMIN"));
    assertThat(jwtService.isTokenValid(token)).isTrue();
  }

  @Test
  void generateToken_whenPrincipalIsUserDetails_usesGrantedAuthorities() {
    JwtService jwtService = new JwtService(SECRET_KEY, 3600000L);
    UserDetails userDetails =
        new org.springframework.security.core.userdetails.User(
            "user2", "pw", List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));

    String token = jwtService.generateToken(userDetails);

    assertThat(jwtService.extractUsername(token)).isEqualTo("user2");
    assertThat(jwtService.extractAuthorities(token))
        .containsExactly(new SimpleGrantedAuthority("ROLE_MANAGER"));
  }

  @Test
  void isTokenValid_whenTokenIsExpired_returnsFalse() {
    JwtService jwtService = new JwtService(SECRET_KEY, -1000L);
    User user =
        TestDataFactory.user(UUID.randomUUID(), "user1", "user1@example.com", "encoded", Role.USER);

    String token = jwtService.generateToken(user);

    assertThat(jwtService.isTokenValid(token)).isFalse();
  }
}
