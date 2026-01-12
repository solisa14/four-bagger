package com.github.solisa14.fourbagger.api.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.security.JwtService;
import com.github.solisa14.fourbagger.api.user.Role;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockMvc mockMvc;
  @Mock private AuthenticationService authenticationService;
  @Mock private JwtService jwtService;

  private AuthenticationController authenticationController;

  @BeforeEach
  void setUp() {
    authenticationController = new AuthenticationController(authenticationService, jwtService, 604800000L);
    mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
  }

  @Test
  void register_shouldReturnCreated_whenRequestValid() throws Exception {
    RegisterUserRequest request =
        new RegisterUserRequest("testuser", "test@example.com", "StrongP@ssw0rd!", "Test", "User");
    RegisterUserResponse response =
        new RegisterUserResponse(UUID.randomUUID(), "testuser", "test@example.com", Role.USER);
    AuthenticationResponse authResponse = new AuthenticationResponse("jwt-token", "refresh-token");

    when(authenticationService.registerUser(any(RegisterUserRequest.class))).thenReturn(response);
    when(authenticationService.authenticate(any(LoginRequest.class))).thenReturn(authResponse);
    when(jwtService.getExpirationTime()).thenReturn(3600000L);

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(cookie().value("accessToken", "jwt-token"))
        .andExpect(cookie().httpOnly("accessToken", true))
        .andExpect(cookie().value("refreshToken", "refresh-token"))
        .andExpect(cookie().httpOnly("refreshToken", true));
  }

  @Test
  void login_shouldReturnOk_whenCredentialsValid() throws Exception {
    LoginRequest request = new LoginRequest("testuser", "StrongP@ssw0rd!");
    AuthenticationResponse authResponse = new AuthenticationResponse("jwt-token", "refresh-token");

    when(authenticationService.authenticate(any(LoginRequest.class))).thenReturn(authResponse);
    when(jwtService.getExpirationTime()).thenReturn(3600000L);

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(cookie().value("accessToken", "jwt-token"))
        .andExpect(cookie().httpOnly("accessToken", true))
        .andExpect(cookie().value("refreshToken", "refresh-token"))
        .andExpect(cookie().httpOnly("refreshToken", true));
  }

  @Test
  void refreshToken_shouldReturnNewAccessToken_whenTokenValid() throws Exception {
    String refreshToken = "valid-refresh-token";
    String newAccessToken = "new-access-token";

    when(authenticationService.refreshToken(refreshToken)).thenReturn(newAccessToken);
    when(jwtService.getExpirationTime()).thenReturn(3600000L);

    mockMvc
        .perform(
            post("/api/v1/auth/refresh-token")
                .cookie(new Cookie("refreshToken", refreshToken)))
        .andExpect(status().isOk())
        .andExpect(cookie().value("accessToken", newAccessToken))
        .andExpect(cookie().httpOnly("accessToken", true));
  }

  @Test
  void logout_shouldClearCookies() throws Exception {
    String refreshToken = "valid-refresh-token";

    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .cookie(new Cookie("refreshToken", refreshToken)))
        .andExpect(status().isOk())
        .andExpect(cookie().value("accessToken", ""))
        .andExpect(cookie().maxAge("accessToken", 0))
        .andExpect(cookie().value("refreshToken", ""))
        .andExpect(cookie().maxAge("refreshToken", 0));
        
    verify(authenticationService).logout(refreshToken);
  }
}
