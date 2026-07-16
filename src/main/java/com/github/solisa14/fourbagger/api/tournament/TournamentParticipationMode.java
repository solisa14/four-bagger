package com.github.solisa14.fourbagger.api.tournament;

/**
 * How players enter a tournament. Chosen at creation and immutable thereafter.
 *
 * <ul>
 *   <li>{@link #SELF_JOIN} — account holders join with a join code
 *   <li>{@link #ORGANIZER_MANAGED} — organizer enters guest participants; no join code
 * </ul>
 */
public enum TournamentParticipationMode {
  SELF_JOIN,
  ORGANIZER_MANAGED
}
