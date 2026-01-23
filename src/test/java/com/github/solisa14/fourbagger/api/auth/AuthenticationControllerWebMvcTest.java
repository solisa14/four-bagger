package com.github.solisa14.fourbagger.api.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(
    properties = {
      "app.security.jwt.refresh-token.expiration-ms=604800000",
      "app.security.cookie.secure=false"
    })
class AuthenticationControllerWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private MockMvc mockMvc;
  @MockitoBean private AuthenticationService authenticationService;

  @MockitoBean private com.github.solisa14.fourbagger.api.security.JwtService jwtService;

  @Test
  void register_returnsValidationError() throws Exception {
    RegisterUserRequest request =
        new RegisterUserRequest("abc", "user@example.com", "Password1!", "Test", "User");

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("username: Username must be between 5 and 30 characters"));
  }

  @Test
  void login_returnsValidationError() throws Exception {
    LoginRequest request = new LoginRequest("user1", "");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("password: Password is required"));
  }
}
