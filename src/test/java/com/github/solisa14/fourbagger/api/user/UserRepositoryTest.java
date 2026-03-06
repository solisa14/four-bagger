package com.github.solisa14.fourbagger.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.solisa14.fourbagger.api.testsupport.AbstractDataJpaTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class UserRepositoryTest extends AbstractDataJpaTest {

  @Autowired private UserRepository userRepository;

  @Autowired private Flyway flyway;

  @Test
  void flywayMigrationsApplied() {
    assertThat(flyway.info().current()).isNotNull();
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("4");
  }

  @Test
  void findUserByUsername_returnsUser() {
    User user = createUser("user1", "user1@example.com");
    userRepository.saveAndFlush(user);

    assertThat(userRepository.findUserByUsername("user1")).contains(user);
  }

  @Test
  void findUserByEmail_returnsUser() {
    User user = createUser("user2", "user2@example.com");
    userRepository.saveAndFlush(user);

    assertThat(userRepository.findUserByEmail("user2@example.com")).contains(user);
  }

  @Test
  void save_enforcesUniqueUsername() {
    User user1 = createUser("duplicate", "one@example.com");
    User user2 = createUser("duplicate", "two@example.com");
    userRepository.saveAndFlush(user1);

    assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void save_enforcesUniqueEmail() {
    User user1 = createUser("user3", "duplicate@example.com");
    User user2 = createUser("user4", "duplicate@example.com");
    userRepository.saveAndFlush(user1);

    assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private User createUser(String username, String email) {
    return User.builder()
        .username(username)
        .email(email)
        .password("encoded")
        .firstName("Test")
        .lastName("User")
        .role(Role.USER)
        .build();
  }
}
