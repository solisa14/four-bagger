package com.github.solisa14.fourbagger.api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation to ensure at least one field in the target class is non-blank.
 *
 * <p>Applied at the class level to validate partial update requests where at least one field must
 * be provided. Reusable across any DTO that supports partial updates.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtLeastOneFieldRequiredValidator.class)
public @interface AtLeastOneFieldRequired {
  /**
   * The default error message to return when validation fails.
   *
   * @return the error message
   */
  String message() default "At least one field must be provided";

  /**
   * The groups the constraint belongs to.
   *
   * @return the groups
   */
  Class<?>[] groups() default {};

  /**
   * The payload associated with the constraint.
   *
   * @return the payload
   */
  Class<? extends Payload>[] payload() default {};
}
