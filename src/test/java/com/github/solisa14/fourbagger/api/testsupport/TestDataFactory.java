package com.github.solisa14.fourbagger.api.testsupport;

import com.github.solisa14.fourbagger.api.auth.LoginRequest;
import com.github.solisa14.fourbagger.api.auth.RefreshToken;
import com.github.solisa14.fourbagger.api.auth.RegisterUserRequest;
import com.github.solisa14.fourbagger.api.game.Game;
import com.github.solisa14.fourbagger.api.game.GameStatus;
import com.github.solisa14.fourbagger.api.game.GameType;
import com.github.solisa14.fourbagger.api.tournament.Tournament;
import com.github.solisa14.fourbagger.api.tournament.TournamentStatus;
import com.github.solisa14.fourbagger.api.user.Role;
import com.github.solisa14.fourbagger.api.user.UpdatePasswordRequest;
import com.github.solisa14.fourbagger.api.user.UpdateProfileRequest;
import com.github.solisa14.fourbagger.api.user.User;
import java.time.Instant;
import java.util.UUID;

public final class TestDataFactory {

  public static final String DEFAULT_PASSWORD = "Password1!";
  public static final String DEFAULT_FIRST_NAME = "Test";
  public static final String DEFAULT_LAST_NAME = "User";

  private TestDataFactory() {}

  public static RegisterUserRequest registerUserRequest() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    return registerUserRequest("user" + suffix, "user" + suffix + "@example.com", DEFAULT_PASSWORD);
  }

  public static RegisterUserRequest registerUserRequest(
      String username, String email, String password) {
    return new RegisterUserRequest(
        username, email, password, DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME);
  }

  public static LoginRequest loginRequest(String username, String password) {
    return new LoginRequest(username, password);
  }

  public static UpdateProfileRequest updateProfileRequest() {
    return new UpdateProfileRequest(DEFAULT_FIRST_NAME, DEFAULT_LAST_NAME);
  }

  public static UpdatePasswordRequest updatePasswordRequest(String currentPassword) {
    return new UpdatePasswordRequest(currentPassword, DEFAULT_PASSWORD);
  }

  public static User user(UUID id, String username, String email, String password, Role role) {
    return User.builder()
        .id(id)
        .username(username)
        .email(email)
        .password(password)
        .firstName(DEFAULT_FIRST_NAME)
        .lastName(DEFAULT_LAST_NAME)
        .role(role)
        .build();
  }

  public static RefreshToken refreshToken(User user, Instant expiryDate, String token) {
    return RefreshToken.builder().user(user).expiryDate(expiryDate).tokenHash(token).build();
  }

  public static Game game(User playerOne, User playerTwo, GameStatus status) {
    return Game.builder()
        .playerOne(playerOne)
        .playerTwo(playerTwo)
        .targetScore(21)
        .status(status)
        .createdBy(playerOne)
        .build();
  }

  public static Tournament tournament(User organizer, String title, String joinCode) {
    return tournament(null, organizer, title, joinCode, GameType.SINGLES);
  }

  public static Tournament tournament(UUID id, User organizer, String title, String joinCode) {
    return tournament(id, organizer, title, joinCode, GameType.SINGLES);
  }

  public static Tournament tournament(
      UUID id, User organizer, String title, String joinCode, GameType gameType) {
    return Tournament.builder()
        .id(id)
        .organizer(organizer)
        .title(title)
        .joinCode(joinCode)
        .status(TournamentStatus.REGISTRATION)
        .gameType(gameType)
        .build();
  }
}
