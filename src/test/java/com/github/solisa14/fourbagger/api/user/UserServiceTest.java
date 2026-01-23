package com.github.solisa14.fourbagger.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.solisa14.fourbagger.api.auth.RegisterUserRequest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
  void createUser_throwsWhenUsernameExists() {
    RegisterUserRequest request = TestDataFactory.registerUserRequest();
    when(userRepository.findUserByUsername(request.username())).thenReturn(Optional.of(new User()));

    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOf(UserAlreadyExistsException.class);
  }

  @Test
  void createUser_throwsWhenEmailExists() {
    RegisterUserRequest request = TestDataFactory.registerUserRequest();
    when(userRepository.findUserByUsername(request.username())).thenReturn(Optional.empty());
    when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.of(new User()));

    assertThatThrownBy(() -> userService.createUser(request))
        .isInstanceOf(EmailAlreadyExistsException.class);
  }

  @Test
  void createUser_savesUserWithEncodedPasswordAndRole() {
    RegisterUserRequest request = TestDataFactory.registerUserRequest();
    when(userRepository.findUserByUsername(request.username())).thenReturn(Optional.empty());
    when(userRepository.findUserByEmail(request.email())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(request.password())).thenReturn("encoded");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              user.setId(UUID.randomUUID());
              return user;
            });

    User created = userService.createUser(request);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getUsername()).isEqualTo(request.username());
    assertThat(saved.getEmail()).isEqualTo(request.email());
    assertThat(saved.getPassword()).isEqualTo("encoded");
    assertThat(saved.getRole()).isEqualTo(Role.USER);
    assertThat(created.getId()).isNotNull();
  }

  @Test
  void getUser_throwsWhenNotFound() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUser(id)).isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void updateProfile_updatesNames() {
    UUID id = UUID.randomUUID();
    User user = TestDataFactory.user(id, "user1", "user1@example.com", "encoded", Role.USER);
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    UpdateProfileRequest request = new UpdateProfileRequest("New", "Name");

    User updated = userService.updateProfile(id, request);

    assertThat(updated.getFirstName()).isEqualTo("New");
    assertThat(updated.getLastName()).isEqualTo("Name");
  }

  @Test
  void updatePassword_throwsWhenCurrentPasswordInvalid() {
    UUID id = UUID.randomUUID();
    User user = TestDataFactory.user(id, "user1", "user1@example.com", "encoded", Role.USER);
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

    assertThatThrownBy(
            () -> userService.updatePassword(id, new UpdatePasswordRequest("bad", "NewPassword1!")))
        .isInstanceOf(InvalidPasswordException.class);
  }

  @Test
  void updatePassword_updatesWhenCurrentPasswordValid() {
    UUID id = UUID.randomUUID();
    User user = TestDataFactory.user(id, "user1", "user1@example.com", "encoded", Role.USER);
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("current", "encoded")).thenReturn(true);
    when(passwordEncoder.encode("NewPassword1!")).thenReturn("new-encoded");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    userService.updatePassword(id, new UpdatePasswordRequest("current", "NewPassword1!"));

    assertThat(user.getPassword()).isEqualTo("new-encoded");
    verify(userRepository).save(user);
  }
}
