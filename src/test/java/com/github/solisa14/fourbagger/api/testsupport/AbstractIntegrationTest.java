package com.github.solisa14.fourbagger.api.testsupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired protected MockMvc mockMvc;

  protected List<String> registerUserAndGetTokens(String usernamePrefix) throws Exception {
    var request =
        TestDataFactory.registerUserRequest(
            usernamePrefix + "user",
            usernamePrefix + "user@example.com",
            TestDataFactory.DEFAULT_PASSWORD);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

    return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
  }

  protected String registerAndGetToken(String usernamePrefix) throws Exception {
    List<String> cookies = registerUserAndGetTokens(usernamePrefix);
    return TestCookieHelper.extractCookieValue(cookies, "accessToken");
  }
}
