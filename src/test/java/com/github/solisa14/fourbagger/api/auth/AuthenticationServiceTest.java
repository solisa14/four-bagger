package com.github.solisa14.fourbagger.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.solisa14.fourbagger.api.security.JwtService;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import com.github.solisa14.fourbagger.api.user.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  @Mock private UserService userService;
  @Mock private UserRepository userRepository;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;

  @InjectMocks private AuthenticationService authenticationService;

  @Test
  void registerUser_shouldReturnResponse_whenUserCreated() {
    RegisterUserRequest request =
        new RegisterUserRequest("testuser", "test@example.com", "password", "Test", "User");
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .role(Role.USER)
            .build();

    when(userService.createUser(request)).thenReturn(user);

    RegisterUserResponse response = authenticationService.registerUser(request);

    assertThat(response.username()).isEqualTo("testuser");
    assertThat(response.email()).isEqualTo("test@example.com");
    verify(userService).createUser(request);
  }

  @Test
  void authenticate_shouldReturnTokens_whenCredentialsValid() {
    LoginRequest request = new LoginRequest("testuser", "password");
    User user = User.builder().id(UUID.randomUUID()).username("testuser").build();
    RefreshToken refreshToken = RefreshToken.builder().token("refresh-token").build();

    when(userRepository.findUserByUsername("testuser")).thenReturn(Optional.of(user));
    when(jwtService.generateToken(user)).thenReturn("jwt-token");
    when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(refreshToken);

    AuthenticationResponse response = authenticationService.authenticate(request);

    assertThat(response.accessToken()).isEqualTo("jwt-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
    verify(authenticationManager)
        .authenticate(new UsernamePasswordAuthenticationToken("testuser", "password"));
  }
}
