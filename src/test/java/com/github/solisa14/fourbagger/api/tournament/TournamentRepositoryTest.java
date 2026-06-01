package com.github.solisa14.fourbagger.api.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.solisa14.fourbagger.api.testsupport.AbstractDataJpaTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.User;
import com.github.solisa14.fourbagger.api.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TournamentRepositoryTest extends AbstractDataJpaTest {

  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private UserRepository userRepository;

  private User savedUser(String suffix) {
    return userRepository.saveAndFlush(
        TestDataFactory.user(
            null, "user" + suffix, "user" + suffix + "@example.com", "encoded", Role.USER));
  }

  @Test
  void findByJoinCode_whenCodeExists_returnsTournament() {
    User organizer = savedUser("a");
    tournamentRepository.saveAndFlush(
        TestDataFactory.tournament(organizer, "Summer Cup", "ABC123"));

    Optional<Tournament> result = tournamentRepository.findByJoinCode("ABC123");

    assertThat(result).isPresent();
    assertThat(result.get().getTitle()).isEqualTo("Summer Cup");
  }

  @Test
  void findByJoinCode_whenCodeDoesNotExist_returnsEmpty() {
    Optional<Tournament> result = tournamentRepository.findByJoinCode("NOTEXIST");

    assertThat(result).isEmpty();
  }
}
