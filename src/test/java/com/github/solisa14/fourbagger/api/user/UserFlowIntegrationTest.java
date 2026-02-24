package com.github.solisa14.fourbagger.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.auth.LoginRequest;
import com.github.solisa14.fourbagger.api.auth.RegisterUserRequest;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestCookieHelper;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class UserFlowIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private MockMvc mockMvc;

  @Test
  void passwordUpdate_fullFlow() throws Exception {
    RegisterUserRequest registerRequest = TestDataFactory.registerUserRequest();

    // 1. Register user
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

    // 2. Extract accessToken cookie
    List<String> registerCookies =
        registerResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
    String accessToken =
        TestCookieHelper.extractCookieValue(registerCookies, "accessToken");
    assertThat(accessToken).isNotBlank();

    // 3. Update password (requires accessToken, not refreshToken)
    String newPassword = "NewPass123!";
    UpdatePasswordRequest updatePasswordRequest =
        new UpdatePasswordRequest(registerRequest.password(), newPassword);
    mockMvc
        .perform(
            put("/api/v1/user/me/password")
                .cookie(TestCookieHelper.cookie("accessToken", accessToken))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(updatePasswordRequest)))
        .andExpect(status().isOk());

    // 4. Login with old password — should fail with 401
    LoginRequest oldPassLogin =
        TestDataFactory.loginRequest(registerRequest.username(), registerRequest.password());
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(oldPassLogin)))
        .andExpect(status().isUnauthorized());

    // 5. Login with new password — should succeed with cookies
    LoginRequest newPassLogin =
        TestDataFactory.loginRequest(registerRequest.username(), newPassword);
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
