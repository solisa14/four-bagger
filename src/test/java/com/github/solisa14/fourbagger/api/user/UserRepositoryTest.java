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
  void flywayInfo_whenChecked_returnsCurrentMigration() {
    assertThat(flyway.info().current()).isNotNull();
  }

  @Test
  void findUserByUsername_whenUserExists_returnsUser() {
    User user = createUser("user1");
    userRepository.saveAndFlush(user);

    assertThat(userRepository.findUserByUsername("user1")).contains(user);
  }

  @Test
  void save_whenUsernameAlreadyExists_throwsDataIntegrityViolationException() {
    User user1 = createUser("duplicate");
    User user2 = createUser("duplicate");
    userRepository.saveAndFlush(user1);

    assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private User createUser(String username) {
    return User.builder()
        .username(username)
        .password("encoded")
        .firstName("Test")
        .lastName("User")
        .role(Role.USER)
        .build();
  }
}
