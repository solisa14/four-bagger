package com.github.solisa14.fourbagger.api.auth;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.common.exception.GlobalExceptionHandler;
import com.github.solisa14.fourbagger.api.user.EmailAlreadyExistsException;
import com.github.solisa14.fourbagger.api.user.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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
  void register_whenUsernameTooShort_returnsBadRequest() throws Exception {
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
  void login_whenPasswordMissing_returnsBadRequest() throws Exception {
    LoginRequest request = new LoginRequest("user1", "");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("password: Password is required"));
  }

  @Test
  void register_whenDataIntegrityViolationBubblesUp_returnsConflict() throws Exception {
    RegisterUserRequest request =
        new RegisterUserRequest("validuser", "user@example.com", "Password1!", "Test", "User");
    when(authenticationService.registerUser(request))
        .thenThrow(new DataIntegrityViolationException("uk_users_username"));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Request conflicts with existing data"));
  }

  @Test
  void register_whenUsernameAlreadyExists_returnsConflict() throws Exception {
    RegisterUserRequest request =
        new RegisterUserRequest("validuser", "user@example.com", "Password1!", "Test", "User");
    when(authenticationService.registerUser(request))
        .thenThrow(new UserAlreadyExistsException(request.username()));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.message")
                .value("User with username '" + request.username() + "' may already exist."));
  }

  @Test
  void register_whenEmailAlreadyExists_returnsConflict() throws Exception {
    RegisterUserRequest request =
        new RegisterUserRequest("validuser", "user@example.com", "Password1!", "Test", "User");
    when(authenticationService.registerUser(request))
        .thenThrow(new EmailAlreadyExistsException(request.email()));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("An account with this email may already exist."));
  }

  @Test
  void refreshToken_whenCookieIsMissing_returnsUnauthorized() throws Exception {
    mockMvc
        .perform(post("/api/v1/auth/refresh-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Refresh token is required"));
  }
}
