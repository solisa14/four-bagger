package com.github.solisa14.fourbagger.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

/**
 * Validator implementation for {@link AtLeastOneFieldRequired}.
 *
 * <p>Ensures that partial update requests contain at least one meaningful field. String fields must
 * be non-blank, while non-string fields only need to be non-null.
 */
public class AtLeastOneFieldRequiredValidator
    implements ConstraintValidator<AtLeastOneFieldRequired, Object> {

  /**
   * Implements the validation logic.
   *
   * <p>Validates that the object contains at least one populated field.
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
      if (fieldValue == null) {
        continue;
      }
      if (fieldValue instanceof String stringValue) {
        if (!stringValue.isBlank()) {
          return true;
        }
        continue;
      }
      return true;
    }
    return false;
  }
}
