package com.github.solisa14.fourbagger.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestCookieHelper;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private RefreshTokenService refreshTokenService;

  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void happyPath_registerLoginRefreshLogout() throws Exception {
    RegisterUserRequest registerRequest = TestDataFactory.registerUserRequest();

    MvcResult registerResult =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value(registerRequest.username()))
            .andExpect(jsonPath("$.email").value(registerRequest.email()))
            .andReturn();

    List<String> registerCookies = registerResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    String accessToken = TestCookieHelper.extractCookieValue(registerCookies, "accessToken");
    String refreshToken = TestCookieHelper.extractCookieValue(registerCookies, "refreshToken");
    assertThat(accessToken).isNotBlank();
    assertThat(refreshToken).isNotBlank();

    mockMvc
        .perform(get("/api/v1/user/me").cookie(TestCookieHelper.cookie("accessToken", accessToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(registerRequest.username()))
        .andExpect(jsonPath("$.email").value(registerRequest.email()));

    MvcResult refreshResult =
        mockMvc
            .perform(
                post("/api/v1/auth/refresh-token")
                    .cookie(TestCookieHelper.cookie("refreshToken", refreshToken)))
            .andExpect(status().isOk())
            .andReturn();

    List<String> refreshCookies = refreshResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    String newAccessToken = TestCookieHelper.extractCookieValue(refreshCookies, "accessToken");
    String newRefreshToken = TestCookieHelper.extractCookieValue(refreshCookies, "refreshToken");
    assertThat(newAccessToken).isNotBlank();
    assertThat(newRefreshToken).isNotBlank();
    assertThat(newRefreshToken).isNotEqualTo(refreshToken);

    MvcResult logoutResult =
        mockMvc
            .perform(
                post("/api/v1/auth/logout")
                    .cookie(TestCookieHelper.cookie("refreshToken", newRefreshToken)))
            .andExpect(status().isOk())
            .andReturn();

    List<String> logoutCookies = logoutResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(TestCookieHelper.hasClearedCookie(logoutCookies, "accessToken")).isTrue();
    assertThat(TestCookieHelper.hasClearedCookie(logoutCookies, "refreshToken")).isTrue();

    mockMvc
        .perform(
            post("/api/v1/auth/refresh-token")
                .cookie(TestCookieHelper.cookie("refreshToken", newRefreshToken)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_failsWithInvalidPassword() throws Exception {
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

    LoginRequest request = new LoginRequest(username, "WrongPassword1!");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid username or password"));
  }

  @Test
  void refreshToken_failsWhenExpired() throws Exception {
    String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
    String username = "expireduser" + suffix;
    String email = "expireduser" + suffix + "@example.com";
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

    RefreshToken token =
        RefreshToken.builder()
            .user(user)
            .tokenHash(refreshTokenService.hashToken("expired-token"))
            .expiryDate(Instant.now().minusSeconds(30))
            .build();
    refreshTokenRepository.saveAndFlush(token);

    mockMvc
        .perform(
            post("/api/v1/auth/refresh-token")
                .cookie(TestCookieHelper.cookie("refreshToken", "expired-token")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message", containsString("Refresh token was expired")));
  }

  @Test
  void login_replacesPreviousRefreshSession() throws Exception {
    String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
    String username = "sessionuser" + suffix;
    String email = "sessionuser" + suffix + "@example.com";
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

    MvcResult firstLogin =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

    String firstRefreshToken =
        TestCookieHelper.extractCookieValue(
            firstLogin.getResponse().getHeaders(HttpHeaders.SET_COOKIE), "refreshToken");

    MvcResult secondLogin =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

    String secondRefreshToken =
        TestCookieHelper.extractCookieValue(
            secondLogin.getResponse().getHeaders(HttpHeaders.SET_COOKIE), "refreshToken");

    assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

    mockMvc
        .perform(
            post("/api/v1/auth/refresh-token")
                .cookie(TestCookieHelper.cookie("refreshToken", firstRefreshToken)))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/api/v1/auth/refresh-token")
                .cookie(TestCookieHelper.cookie("refreshToken", secondRefreshToken)))
        .andExpect(status().isOk());
  }
}
