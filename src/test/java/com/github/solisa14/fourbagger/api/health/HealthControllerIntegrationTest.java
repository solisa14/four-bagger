package com.github.solisa14.fourbagger.api.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class HealthControllerIntegrationTest extends AbstractIntegrationTest {

  @Test
  void health_whenUnauthenticated_returnsOk() throws Exception {
    mockMvc.perform(get("/health")).andExpect(status().isOk());
  }

  @Test
  void protectedRoute_whenUnauthenticated_returnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/user/me")).andExpect(status().isUnauthorized());
  }
}
