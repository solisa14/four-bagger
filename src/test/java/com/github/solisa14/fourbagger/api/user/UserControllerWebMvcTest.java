package com.github.solisa14.fourbagger.api.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.solisa14.fourbagger.api.common.exception.GlobalExceptionHandler;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private MockMvc mockMvc;
  @MockitoBean private UserService userService;
  @MockitoBean private com.github.solisa14.fourbagger.api.security.JwtService jwtService;

  @Test
  void updateProfile_whenPayloadIsEmpty_returnsBadRequest() throws Exception {
    User principal =
        TestDataFactory.user(UUID.randomUUID(), "user1", "user1@example.com", "encoded", Role.USER);

    mockMvc
        .perform(
            patch("/api/v1/user/me")
                .with(user(principal))
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("At least one field must be provided"));
  }

  @Test
  void updatePassword_whenNewPasswordMissing_returnsBadRequest() throws Exception {
    User principal =
        TestDataFactory.user(UUID.randomUUID(), "user1", "user1@example.com", "encoded", Role.USER);
    Map<String, String> payload = Map.of("currentPassword", "current");

    mockMvc
        .perform(
            put("/api/v1/user/me/password")
                .with(user(principal))
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("newPassword: New password is required"));
  }
}
