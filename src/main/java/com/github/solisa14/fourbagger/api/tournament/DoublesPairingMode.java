package com.github.solisa14.fourbagger.api.tournament;

/**
 * How doubles partners are assigned in an organizer-managed doubles tournament.
 *
 * <ul>
 *   <li>{@link #RANDOM} — bracket generation and reshuffle randomize both pairings and team seeds
 *   <li>{@link #MANUAL} — organizer-defined pairs are preserved; only team seeds are randomized
 * </ul>
 *
 * <p>Only meaningful for organizer-managed doubles. Self-join and singles leave this unset.
 */
public enum DoublesPairingMode {
    RANDOM,
    MANUAL
}
