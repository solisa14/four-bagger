package com.github.solisa14.fourbagger.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.CreateUserCommand;
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

  @Mock private com.github.solisa14.fourbagger.api.security.JwtService jwtService;

  @Mock private RefreshTokenService refreshTokenService;

  @InjectMocks private AuthenticationService authenticationService;

  @Test
  void registerUser_whenRequestIsValid_returnsUserDetails() {
    UUID id = UUID.randomUUID();
    User user = TestDataFactory.user(id, "user1", "user1@example.com", "encoded", Role.USER);
    CreateUserCommand command =
        new CreateUserCommand("user1", "user1@example.com", "Password1!", "Test", "User");

    when(userService.createUser(command)).thenReturn(user);

    User response = authenticationService.registerUser(command);

    assertThat(response.getId()).isEqualTo(id);
    assertThat(response.getUsername()).isEqualTo(user.getUsername());
    assertThat(response.getEmail()).isEqualTo(user.getEmail());
    assertThat(response.getRole()).isEqualTo(user.getRole());
  }

  @Test
  void authenticate_whenCredentialsAreValid_returnsTokens() {
    User user =
        TestDataFactory.user(UUID.randomUUID(), "user1", "user1@example.com", "encoded", Role.USER);
    LoginCommand command = new LoginCommand("user1", "Password1!");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(command.username(), command.password()));
    when(userRepository.findUserByUsername(command.username())).thenReturn(Optional.of(user));
    when(jwtService.generateToken(user)).thenReturn("jwt-token");
    when(refreshTokenService.issueRefreshToken(user.getId()))
        .thenReturn(new RefreshTokenSession(user, "refresh-token"));

    TokenPair response = authenticationService.authenticate(command);

    assertThat(response.accessToken()).isEqualTo("jwt-token");
    assertThat(response.refreshToken()).isEqualTo("refresh-token");
  }

  @Test
  void authenticate_whenUserMissingAfterAuthentication_throwsAuthenticationFailedException() {
    LoginCommand command = new LoginCommand("user1", "Password1!");
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(
            new UsernamePasswordAuthenticationToken(command.username(), command.password()));
    when(userRepository.findUserByUsername(command.username())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authenticationService.authenticate(command))
        .isInstanceOf(AuthenticationFailedException.class);
  }

  @Test
  void refreshToken_whenTokenIsValid_rotatesTokens() {
    User user =
        TestDataFactory.user(UUID.randomUUID(), "user1", "user1@example.com", "encoded", Role.USER);
    when(refreshTokenService.rotateRefreshToken("old-refresh-token"))
        .thenReturn(new RefreshTokenSession(user, "new-refresh-token"));
    when(jwtService.generateToken(user)).thenReturn("jwt-token");

    TokenPair response = authenticationService.refreshToken("old-refresh-token");

    assertThat(response.accessToken()).isEqualTo("jwt-token");
    assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
  }

  @Test
  void logout_whenRefreshTokenProvided_deletesRefreshToken() {
    authenticationService.logout("refresh-token");

    verify(refreshTokenService).deleteByToken("refresh-token");
  }
}
