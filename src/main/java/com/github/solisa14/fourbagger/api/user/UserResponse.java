package com.github.solisa14.fourbagger.api.user;

import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    String firstName,
    String lastName,
    Role role
) {}
