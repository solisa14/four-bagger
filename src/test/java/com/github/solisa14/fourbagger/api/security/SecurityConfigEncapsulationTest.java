package com.github.solisa14.fourbagger.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SecurityConfigEncapsulationTest {

  @Test
  void constructorCopiesAllowedOrigins() {
    List<String> origins = new ArrayList<>(List.of("https://example.com"));
    SecurityConfig config =
        new SecurityConfig(
            mock(UserRepository.class),
            origins,
            mock(ApiAuthenticationEntryPoint.class),
            mock(ApiAccessDeniedHandler.class));

    origins.add("https://attacker.example");

    Object retainedOrigins = ReflectionTestUtils.getField(config, "allowedOrigins");
    assertThat(retainedOrigins).isEqualTo(List.of("https://example.com"));
  }
}
