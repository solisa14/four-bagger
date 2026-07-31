package com.github.solisa14.fourbagger.api.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Process-up check for load balancer probes.
 *
 * <p>Returns HTTP 200 when the application process is running. Intentionally does not check
 * database or other dependencies.
 */
@RestController
public class HealthController {

  /**
   * Returns 200 when the process is up.
   *
   * @return empty 200 response
   */
  @GetMapping("/health")
  public ResponseEntity<Void> health() {
    return ResponseEntity.ok().build();
  }
}
