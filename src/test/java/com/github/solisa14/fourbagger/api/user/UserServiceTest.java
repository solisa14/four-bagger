package com.github.solisa14.fourbagger.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.solisa14.fourbagger.api.auth.RefreshTokenService;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Mock private RefreshTokenService refreshTokenService;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, passwordEncoder, refreshTokenService);
  }

  @Test
  void createUser_whenUsernameExists_throwsUserAlreadyExistsException() {
    CreateUserCommand command =
        new CreateUserCommand("user1", "user@example.com", "Password1!", "Test", "User");
    when(userRepository.findUserByUsername(command.username())).thenReturn(Optional.of(new User()));

    assertThatThrownBy(() -> userService.createUser(command))
        .isInstanceOf(UserAlreadyExistsException.class);
  }

  @Test
  void createUser_whenEmailExists_throwsEmailAlreadyExistsException() {
    CreateUserCommand command =
        new CreateUserCommand("user1", "user@example.com", "Password1!", "Test", "User");
    when(userRepository.findUserByUsername(command.username())).thenReturn(Optional.<User>empty());
    when(userRepository.findUserByEmail(command.email())).thenReturn(Optional.of(new User()));

    assertThatThrownBy(() -> userService.createUser(command))
        .isInstanceOf(EmailAlreadyExistsException.class);
  }

  @Test
  void createUser_whenRequestIsValid_savesUserWithEncodedPasswordAndRole() {
    CreateUserCommand command =
        new CreateUserCommand("user1", "user@example.com", "Password1!", "Test", "User");
    when(userRepository.findUserByUsername(command.username())).thenReturn(Optional.<User>empty());
    when(userRepository.findUserByEmail(command.email())).thenReturn(Optional.<User>empty());
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0, User.class);
              user.setId(UUID.randomUUID());
              return user;
            });

    User created = userService.createUser(command);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getUsername()).isEqualTo(command.username());
    assertThat(saved.getEmail()).isEqualTo(command.email());
    assertThat(saved.getPassword()).isNotEqualTo(command.password());
    assertThat(passwordEncoder.matches(command.password(), saved.getPassword())).isTrue();
    assertThat(saved.getRole()).isEqualTo(Role.USER);
    assertThat(created.getId()).isNotNull();
  }

  @Test
  void createUser_whenInsertRaceTriggersConstraint_throwsUserAlreadyExistsException() {
    CreateUserCommand command =
        new CreateUserCommand("user1", "user@example.com", "Password1!", "Test", "User");
    when(userRepository.findUserByUsername(command.username()))
        .thenReturn(Optional.<User>empty())
        .thenReturn(Optional.of(new User()));
    when(userRepository.findUserByEmail(command.email()))
        .thenReturn(Optional.<User>empty())
        .thenReturn(Optional.<User>empty());
    when(userRepository.save(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("uk_users_username"));

    assertThatThrownBy(() -> userService.createUser(command))
        .isInstanceOf(UserAlreadyExistsException.class);
  }

  @Test
  void getUser_whenUserNotFound_throwsUserNotFoundException() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUser(id)).isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void updateProfile_whenRequestHasNames_updatesNames() {
    UUID id = UUID.randomUUID();
    User user = TestDataFactory.user(id, "user1", "user1@example.com", "encoded", Role.USER);
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, User.class));
    UpdateProfileCommand command = new UpdateProfileCommand("New", "Name");

    User updated = userService.updateProfile(id, command);

    assertThat(updated.getFirstName()).isEqualTo("New");
    assertThat(updated.getLastName()).isEqualTo("Name");
  }

  @Test
  void updateProfile_whenBothFieldsNull_throwsInvalidProfileUpdateException() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> userService.updateProfile(id, new UpdateProfileCommand(null, null)))
        .isInstanceOf(InvalidProfileUpdateException.class);

    verify(userRepository, never()).findById(any());
  }

  @Test
  void updatePassword_whenCurrentPasswordIsInvalid_throwsInvalidPasswordException() {
    UUID id = UUID.randomUUID();
    User user =
        TestDataFactory.user(
            id,
            "user1",
            "user1@example.com",
            passwordEncoder.encode("current"),
            Role.USER);
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    assertThatThrownBy(
            () -> userService.updatePassword(id, new UpdatePasswordCommand("bad", "NewPassword1!")))
        .isInstanceOf(InvalidPasswordException.class);
  }

  @Test
  void updatePassword_whenCurrentPasswordIsValid_updatesPasswordAndInvalidatesSessions() {
    UUID id = UUID.randomUUID();
    String currentPasswordHash = passwordEncoder.encode("current");
    User user =
        TestDataFactory.user(id, "user1", "user1@example.com", currentPasswordHash, Role.USER);
    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, User.class));

    userService.updatePassword(id, new UpdatePasswordCommand("current", "NewPassword1!"));

    assertThat(user.getPassword()).isNotEqualTo(currentPasswordHash);
    assertThat(passwordEncoder.matches("NewPassword1!", user.getPassword())).isTrue();
    verify(userRepository).save(user);
    verify(refreshTokenService).deleteByUserId(id);
  }
}
