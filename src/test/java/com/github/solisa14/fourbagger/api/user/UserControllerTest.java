package com.github.solisa14.fourbagger.api.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  private MockMvc mockMvc;
  @Mock private UserService userService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private User mockUser;

  @BeforeEach
  void setUp() {
    mockUser = User.builder()
        .id(UUID.randomUUID())
        .username("testuser")
        .email("test@example.com")
        .firstName("Test")
        .lastName("User")
        .role(Role.USER)
        .build();

    UserController controller = new UserController(userService);
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterAnnotation(AuthenticationPrincipal.class) != null;
          }

          @Override
          public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return mockUser;
          }
        })
        .build();
  }

  @Test
  void getCurrentUser_shouldReturnUser() throws Exception {
    when(userService.getUser(mockUser.getId())).thenReturn(mockUser);

    mockMvc.perform(get("/api/v1/user/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.firstName").value("Test"));
  }

  @Test
  void updateProfile_shouldReturnUpdatedUser() throws Exception {
    UpdateProfileRequest request = new UpdateProfileRequest("New", "Name");
    User updatedUser = User.builder()
        .id(mockUser.getId())
        .username("testuser")
        .firstName("New")
        .lastName("Name")
        .role(Role.USER)
        .build();

    when(userService.updateProfile(eq(mockUser.getId()), any(UpdateProfileRequest.class)))
        .thenReturn(updatedUser);

    mockMvc.perform(patch("/api/v1/user/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("New"))
        .andExpect(jsonPath("$.lastName").value("Name"));
  }

  @Test
  void updatePassword_shouldReturnOk() throws Exception {
    UpdatePasswordRequest request = new UpdatePasswordRequest("oldPass", "newPass123!");

    mockMvc.perform(put("/api/v1/user/me/password")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(userService).updatePassword(eq(mockUser.getId()), any(UpdatePasswordRequest.class));
  }
}
