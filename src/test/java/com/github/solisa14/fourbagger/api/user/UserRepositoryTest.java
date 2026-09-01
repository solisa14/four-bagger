package com.github.solisa14.fourbagger.api.user;

import com.github.solisa14.fourbagger.api.testsupport.AbstractDataJpaTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRepositoryTest extends AbstractDataJpaTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Flyway flyway;

    @Test
    void flywayInfo_whenChecked_returnsCurrentMigration() {
        assertThat(flyway.info().current()).isNotNull();
    }

    @Test
    void findUserByUsername_whenUserExists_returnsUser() {
        User user = TestDataFactory.user(null, "user1", "encoded", Role.USER);
        userRepository.saveAndFlush(user);

        assertThat(userRepository.findUserByUsername("user1")).contains(user);
    }

    @Test
    void save_whenUsernameAlreadyExists_throwsDataIntegrityViolationException() {
        User user1 = TestDataFactory.user(null, "duplicate", "encoded", Role.USER);
        User user2 = TestDataFactory.user(null, "duplicate", "encoded", Role.USER);
        userRepository.saveAndFlush(user1);

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
