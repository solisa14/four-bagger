package com.github.solisa14.fourbagger.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.solisa14.fourbagger.api.auth.RegisterUserRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private BCryptPasswordEncoder passwordEncoder;

  @InjectMocks private UserService userService;

  @Test
  void createUser_shouldCreateUser_whenValidRequest() {
    RegisterUserRequest request =
        new RegisterUserRequest("testuser", "test@example.com", "password", "Test", "User");
    
    when(userRepository.findUserByUsername(request.username())).thenReturn(Optional.empty());
    when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    User createdUser = userService.createUser(request);

    assertThat(createdUser).isNotNull();
    assertThat(createdUser.getUsername()).isEqualTo(request.username());
    assertThat(createdUser.getEmail()).isEqualTo(request.email());
    assertThat(createdUser.getPassword()).isEqualTo("encodedPassword");
    assertThat(createdUser.getRole()).isEqualTo(Role.USER);

    verify(userRepository).save(any(User.class));
  }

  @Test
  void createUser_shouldThrowException_whenUsernameExists() {
    RegisterUserRequest request =
        new RegisterUserRequest("existinguser", "test@example.com", "password", "Test", "User");
    
    when(userRepository.findUserByUsername(request.username())).thenReturn(Optional.of(new User()));

    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessageContaining(request.username());

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createUser_shouldThrowException_whenEmailExists() {
    RegisterUserRequest request =
        new RegisterUserRequest("newuser", "existing@example.com", "password", "Test", "User");
    
    when(userRepository.findUserByUsername(request.username())).thenReturn(Optional.empty());
    when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.of(new User()));

    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessageContaining(request.email());

    verify(userRepository, never()).save(any(User.class));
  }
}
