package com.github.solisa14.fourbagger.api.tournament;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for setting the doubles pairing mode on an organizer-managed doubles tournament.
 *
 * @param doublesPairingMode {@link DoublesPairingMode#RANDOM} or {@link DoublesPairingMode#MANUAL}
 */
public record UpdateDoublesPairingModeRequest(@NotNull DoublesPairingMode doublesPairingMode) {}
