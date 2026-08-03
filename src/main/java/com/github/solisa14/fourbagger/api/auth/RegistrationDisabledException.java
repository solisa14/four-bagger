package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Indicates that new user registration is currently disabled by configuration. */
public class RegistrationDisabledException extends BusinessException {

    /** Constructs a new exception with a default message and FORBIDDEN status. */
    public RegistrationDisabledException() {
        super("Registration is disabled", HttpStatus.FORBIDDEN);
    }
}
