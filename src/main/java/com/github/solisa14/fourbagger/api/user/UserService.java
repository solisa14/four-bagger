package com.github.solisa14.fourbagger.api.user;

import com.github.solisa14.fourbagger.api.auth.RegisterUserRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Manages user account creation and persistence operations.
 *
 * <p>Handles user validation, password encryption, and database persistence. Ensures username
 * uniqueness and assigns default role to new accounts.
 */
@Service
public class UserService {

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
  }

  /**
   * Creates and persists a new user account with encrypted password.
   *
   * <p>Validates username uniqueness, encrypts the password using BCrypt, assigns the USER role,
   * and saves to the database with auto-generated timestamps.
   *
   * @param request registration data including username, plaintext password, and optional names
   * @return the persisted user entity with generated ID and timestamps
   * @throws UserAlreadyExistsException if a user with the given username already exists
   */
  public User createUser(RegisterUserRequest request) {
    if (userRepository.findUserByUsername(request.username()).isPresent()) {
      throw new UserAlreadyExistsException(request.username());
    }
    if (userRepository.findUserByEmail(request.email()).isPresent()) {
      throw new EmailAlreadyExistsException(request.email());
    }
    User createdUser =
        User.builder()
            .username(request.username())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .firstName(request.firstName())
            .lastName(request.lastName())
            .role(Role.USER)
            .build();

    return userRepository.save(createdUser);
  }
}
