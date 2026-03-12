package com.github.solisa14.fourbagger.api.user;

import com.github.solisa14.fourbagger.api.auth.RefreshTokenService;
import com.github.solisa14.fourbagger.api.auth.RegisterUserRequest;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final RefreshTokenService refreshTokenService;

  /**
   * Constructs a new UserService with the required dependencies.
   *
   * @param userRepository the repository for user data access
   * @param passwordEncoder the encoder used for hashing passwords
   * @param refreshTokenService the service for managing refresh tokens
   */
  public UserService(
      UserRepository userRepository,
      BCryptPasswordEncoder passwordEncoder,
      RefreshTokenService refreshTokenService) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
    this.refreshTokenService = refreshTokenService;
  }

  /**
   * Creates and persists a new user account with an encrypted password.
   *
   * <p>Validates username and email uniqueness, encrypts the password using BCrypt, assigns the
   * USER role, and saves to the database with auto-generated timestamps.
   *
   * @param request registration data including username, plaintext password, and required names
   * @return the persisted user entity with generated ID and timestamps
   * @throws UserAlreadyExistsException if a user with the given username already exists
   * @throws EmailAlreadyExistsException if a user with the given email already exists
   */
  @Transactional
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

    try {
      return userRepository.save(createdUser);
    } catch (DataIntegrityViolationException ex) {
      if (userRepository.findUserByUsername(request.username()).isPresent()) {
        throw new UserAlreadyExistsException(request.username());
      }
      if (userRepository.findUserByEmail(request.email()).isPresent()) {
        throw new EmailAlreadyExistsException(request.email());
      }
      throw ex;
    }
  }

  /**
   * Retrieves a user by their unique identifier.
   *
   * @param id the UUID of the user to retrieve
   * @return the found User entity
   * @throws UserNotFoundException if no user is found with the provided ID
   */
  public User getUser(UUID id) {
    return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
  }

  /**
   * Updates a user's profile information.
   *
   * @param id the UUID of the user to update
   * @param request the profile update request payload containing optional fields
   * @return the updated User entity
   * @throws UserNotFoundException if no user is found with the provided ID
   */
  @Transactional
  public User updateProfile(UUID id, UpdateProfileRequest request) {
    User user = getUser(id);
    if (request.firstName() != null) {
      user.setFirstName(request.firstName());
    }
    if (request.lastName() != null) {
      user.setLastName(request.lastName());
    }
    return userRepository.save(user);
  }

  /**
   * Updates a user's password and revokes all active refresh tokens for security.
   *
   * @param id the UUID of the user changing their password
   * @param request the password update request payload containing current and new passwords
   * @throws InvalidPasswordException if the provided current password does not match the stored
   *     hash
   * @throws UserNotFoundException if no user is found with the provided ID
   */
  @Transactional
  public void updatePassword(UUID id, UpdatePasswordRequest request) {
    User user = getUser(id);
    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new InvalidPasswordException();
    }
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    refreshTokenService.deleteByUserId(user.getId());
  }
}
