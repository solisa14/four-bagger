package com.github.solisa14.fourbagger.api.auth;

import com.github.solisa14.fourbagger.api.testsupport.AbstractIntegrationTest;
import com.github.solisa14.fourbagger.api.testsupport.TestDataFactory;
import com.github.solisa14.fourbagger.api.user.CreateUserCommand;
import com.github.solisa14.fourbagger.api.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.auth.registration.enabled=false")
class RegistrationKillSwitchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void register_whenRegistrationDisabled_returnsForbidden() throws Exception {
        RegisterUserRequest request = TestDataFactory.registerUserRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Registration is disabled"));
    }

    @Test
    void login_whenRegistrationDisabled_stillAuthenticatesExistingUser() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "exist" + suffix;
        userService.createUser(new CreateUserCommand(
                username,
                TestDataFactory.DEFAULT_PASSWORD,
                TestDataFactory.DEFAULT_FIRST_NAME,
                TestDataFactory.DEFAULT_LAST_NAME));

        LoginRequest request = new LoginRequest(username, TestDataFactory.DEFAULT_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
