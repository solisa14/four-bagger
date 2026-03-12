package com.github.solisa14.fourbagger.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

/**
 * Validator implementation for {@link AtLeastOneFieldRequired}.
 *
 * <p>Ensures that partial update requests contain at least one non-blank field. This validator uses
 * reflection to check all String fields in the target object.
 */
public class AtLeastOneFieldRequiredValidator
    implements ConstraintValidator<AtLeastOneFieldRequired, Object> {

  /**
   * Implements the validation logic.
   *
   * <p>Validates that the object contains at least one non-blank string field.
   *
   * @param obj the object to validate
   * @param context context in which the constraint is evaluated
   * @return true if the object is valid, false otherwise
   */
  @Override
  public boolean isValid(Object obj, ConstraintValidatorContext context) {
    if (obj == null) {
      return true; // Let @NotNull handle null validation
    }

    Field[] fields = obj.getClass().getDeclaredFields();
    for (Field field : fields) {
      field.setAccessible(true);
      Object fieldValue;
      try {
        fieldValue = field.get(obj);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        continue; // skip this field, check the others
      }
      if (fieldValue instanceof String) {
        if (!((String) fieldValue).isBlank()) {
          return true;
        }
      }
    }
    return false;
  }
}
