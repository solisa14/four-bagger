package com.github.solisa14.fourbagger.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.auth.LoginRequest;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestCookieHelper;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

class UserFlowIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void updatePassword_whenPasswordIsChanged_invalidatesOldCredentialsAndAllowsNewLogin()
      throws Exception {
    String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
    String usernamePrefix = "userflow" + suffix;
    String username = usernamePrefix + "user";
    List<String> registerCookies = registerUserAndGetTokens(usernamePrefix);
    String accessToken =
        TestCookieHelper.extractCookieValue(registerCookies, "accessToken");
    String refreshToken =
        TestCookieHelper.extractCookieValue(registerCookies, "refreshToken");
    assertThat(accessToken).isNotBlank();
    assertThat(refreshToken).isNotBlank();

    // 3. Update password (requires accessToken, not refreshToken)
    String newPassword = "NewPass123!";
    UpdatePasswordRequest updatePasswordRequest =
        new UpdatePasswordRequest(TestDataFactory.DEFAULT_PASSWORD, newPassword);
    mockMvc
        .perform(
            put("/api/v1/user/me/password")
                .cookie(TestCookieHelper.cookie("accessToken", accessToken))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(updatePasswordRequest)))
        .andExpect(status().isOk());

    // 4. Login with old password — should fail with 401
    LoginRequest oldPassLogin =
        TestDataFactory.loginRequest(username, TestDataFactory.DEFAULT_PASSWORD);
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(oldPassLogin)))
        .andExpect(status().isUnauthorized());

    // 5. The previous refresh token should no longer work.
    mockMvc
        .perform(
            post("/api/v1/auth/refresh-token")
                .cookie(TestCookieHelper.cookie("refreshToken", refreshToken)))
        .andExpect(status().isUnauthorized());

    // 6. Login with new password — should succeed with cookies
    LoginRequest newPassLogin =
        TestDataFactory.loginRequest(username, newPassword);
    MvcResult newLoginResult =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(newPassLogin)))
            .andExpect(status().isOk())
            .andReturn();

    List<String> newLoginCookies =
        newLoginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    assertThat(TestCookieHelper.extractCookieValue(newLoginCookies, "accessToken"))
        .isNotBlank();
    assertThat(TestCookieHelper.extractCookieValue(newLoginCookies, "refreshToken"))
        .isNotBlank();
  }
}
