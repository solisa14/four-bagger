package com.github.solisa14.fourbagger.api.security.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.auth.LoginRequest;
import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@TestPropertySource(
    properties = {
      "app.rate-limit.auth.requests=2",
      "app.rate-limit.join-code.requests=2",
      "app.rate-limit.trusted-proxies=10.0.0.5"
    })
class RateLimitFilterIntegrationTest extends AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private RateLimitFilter rateLimitFilter;

  @BeforeEach
  void clearRateLimits() {
    rateLimitFilter.clearBuckets();
  }

  @Test
  void login_whenAuthRateLimitExceeded_returnsTooManyRequestsWithCorsBackoff() throws Exception {
    login("ratelimituser", "http://localhost:3000", null, null).andExpect(status().isUnauthorized());
    login("ratelimituser", "http://localhost:3000", null, null).andExpect(status().isUnauthorized());

    login("ratelimituser", "http://localhost:3000", null, null)
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.message").value("Too many requests"))
        .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Retry-After"));
  }

  @Test
  void login_whenForwardedAddressChanges_usesClientAddressFromTrustedProxiesOnly()
      throws Exception {
    login("untrusteduser", null, "10.0.0.6", "198.51.100.1").andExpect(status().isUnauthorized());
    login("untrusteduser", null, "10.0.0.6", "198.51.100.2").andExpect(status().isUnauthorized());
    login("untrusteduser", null, "10.0.0.6", "198.51.100.3")
        .andExpect(status().isTooManyRequests());

    clearRateLimits();

    login("trusteduser", null, "10.0.0.5", "198.51.100.10, 203.0.113.1")
        .andExpect(status().isUnauthorized());
    login("trusteduser", null, "10.0.0.5", "198.51.100.11, 203.0.113.1")
        .andExpect(status().isUnauthorized());
    login("trusteduser", null, "10.0.0.5", "198.51.100.12, 203.0.113.1")
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void register_andJoinCodeLookup_whenRateLimitsExceeded_returnTooManyRequests() throws Exception {
    register("ratelimreg").andExpect(status().isCreated());
    register("ratelimreg2").andExpect(status().isCreated());
    register("ratelimreg3").andExpect(status().isTooManyRequests());

    clearRateLimits();

    mockMvc.perform(get("/api/v1/tournaments/join-code/ABC123")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/tournaments/join-code/ABC123")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/v1/tournaments/join-code/ABC123"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.status").value(429));
  }

  private ResultActions login(String username, String origin, String remote, String forwarded)
      throws Exception {
    MockHttpServletRequestBuilder request =
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest(username, "Password1!")));
    if (origin != null) {
      request.header(HttpHeaders.ORIGIN, origin);
    }
    if (remote != null) {
      request.with(mockRequest -> {
        mockRequest.setRemoteAddr(remote);
        return mockRequest;
      });
    }
    if (forwarded != null) {
      request.header("X-Forwarded-For", forwarded);
    }
    return mockMvc.perform(request);
  }

  private ResultActions register(String username) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                objectMapper.writeValueAsString(
                    TestDataFactory.registerUserRequest(username, TestDataFactory.DEFAULT_PASSWORD))));
  }
}
