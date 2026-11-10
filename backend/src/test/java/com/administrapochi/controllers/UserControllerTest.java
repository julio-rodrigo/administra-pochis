package com.administrapochi.controllers;

import com.administrapochi.models.dtos.users.UserCreateRequest;
import com.administrapochi.models.dtos.users.UserResponse;
import com.administrapochi.exceptions.ApiExceptionHandler;
import com.administrapochi.models.dtos.users.UserUpdateRequest;
import com.administrapochi.models.enums.UserRole;
import com.administrapochi.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createAcceptsPhoneAndRoleAndReturnsSafeResponse() throws Exception {
        when(userService.create(any())).thenAnswer(invocation -> {
            UserCreateRequest request = invocation.getArgument(0);
            return UserResponse.builder().id(1).phone(request.getPhone()).role(request.getRole()).build();
        });

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "User",
                                  "username": "user",
                                  "password": "plain-password",
                                  "phone": "12345678901234567890",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").value("12345678901234567890"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void createAllowsMissingPhoneAndRole() throws Exception {
        when(userService.create(any())).thenReturn(UserResponse.builder().role(UserRole.OWNER).build());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"User\",\"username\":\"user\",\"password\":\"plain-password\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void updateAcceptsPhoneAndRole() throws Exception {
        when(userService.update(eq(1), any())).thenAnswer(invocation -> {
            UserUpdateRequest request = invocation.getArgument(1);
            return UserResponse.builder().id(1).phone(request.getPhone()).role(request.getRole()).build();
        });

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"5551234567\",\"role\":\"OWNER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("5551234567"))
                .andExpect(jsonPath("$.role").value("OWNER"));

        verify(userService).update(eq(1), any());
    }

    @Test
    void createAndUpdateRejectPhoneLongerThanTwentyCharacters() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "User",
                                  "username": "user",
                                  "password": "plain-password",
                                  "phone": "123456789012345678901"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").exists());
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"123456789012345678901\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void createAndUpdateRejectUnknownRoles() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"User\",\"username\":\"user\",\"password\":\"password\",\"role\":\"DRIVER\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"DRIVER\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }
}
