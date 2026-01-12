package com.github.solisa14.fourbagger.api.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.application.security.jwt.secret-key=YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE="
    })
@Transactional
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Test
  void findUserByUsername_shouldReturnUser_whenUserExists() {
    User user =
        User.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .role(Role.USER)
            .build();
    userRepository.save(user);

    Optional<User> found = userRepository.findUserByUsername("testuser");

    assertThat(found).isPresent();
    assertThat(found.get().getUsername()).isEqualTo(user.getUsername());
  }

  @Test
  void findUserByUsername_shouldReturnEmpty_whenUserDoesNotExist() {
    Optional<User> found = userRepository.findUserByUsername("nonexistent");

    assertThat(found).isEmpty();
  }

  @Test
  void findUserByEmail_shouldReturnUser_whenUserExists() {
    User user =
        User.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .role(Role.USER)
            .build();
    userRepository.save(user);

    Optional<User> found = userRepository.findUserByEmail("test@example.com");

    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo(user.getEmail());
  }
}
