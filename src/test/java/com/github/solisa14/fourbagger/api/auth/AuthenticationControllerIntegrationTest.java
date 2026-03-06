package com.github.solisa14.fourbagger.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class AuthenticationControllerIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void login_setsAuthCookies() throws Exception {
    String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
    String username = "loginuser" + suffix;
    String email = "loginuser" + suffix + "@example.com";
    User user =
        User.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode("Password1!"))
            .firstName("Test")
            .lastName("User")
            .role(Role.USER)
            .build();
    userRepository.saveAndFlush(user);

    LoginRequest request = new LoginRequest(username, "Password1!");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

    List<String> cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(cookies).anyMatch(header -> header.startsWith("accessToken="));
    assertThat(cookies).anyMatch(header -> header.startsWith("refreshToken="));
  }

  @Test
  void userMe_requiresAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/v1/user/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.message").value("Authentication is required to access this resource"));
  }

  @Test
  void refreshToken_requiresRefreshTokenCookie() throws Exception {
    mockMvc
        .perform(post("/api/v1/auth/refresh-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Refresh token is required"));
  }

  @Test
  void protectedEndpoint_withInvalidAccessTokenReturnsUnauthorizedJson() throws Exception {
    mockMvc
        .perform(get("/api/v1/user/me").cookie(new jakarta.servlet.http.Cookie("accessToken", "bad-token")))
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.message").value("Authentication is required to access this resource"));
  }
}
