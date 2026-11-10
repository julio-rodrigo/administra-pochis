package com.administrapochi.mappers;

import com.administrapochi.models.dtos.users.UserUpdateRequest;
import com.administrapochi.models.dtos.users.UserCreateRequest;
import com.administrapochi.models.dtos.users.UserResponse;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.enums.UserRole;
import com.administrapochi.models.mappers.UserMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void updateEntityChangesEditableFields() {
        User user = existingUser();
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Updated name");
        request.setUsername("updated");
        request.setPhone("5551234567");
        request.setRole(UserRole.ADMIN);

        userMapper.updateEntity(request, user);

        assertAll(
                () -> assertEquals("Updated name", user.getName()),
                () -> assertEquals("updated", user.getUsername()),
                () -> assertEquals("5551234567", user.getPhone()),
                () -> assertEquals(UserRole.ADMIN, user.getRole()),
                () -> assertEquals(1, user.getId()),
                () -> assertFalse(user.getActive()),
                () -> assertEquals("existing-hash", user.getPasswordHash()),
                () -> assertEquals(Instant.parse("2026-01-01T00:00:00Z"), user.getCreatedAt()));
    }

    @Test
    void updateEntityPreservesExistingValuesWhenFieldsAreNull() {
        User user = existingUser();
        UserUpdateRequest request = new UserUpdateRequest();

        userMapper.updateEntity(request, user);

        assertAll(
                () -> assertEquals("Original name", user.getName()),
                () -> assertEquals("original", user.getUsername()),
                () -> assertEquals("5550000000", user.getPhone()),
                () -> assertEquals(UserRole.OWNER, user.getRole()),
                () -> assertEquals(1, user.getId()),
                () -> assertFalse(user.getActive()),
                () -> assertEquals("existing-hash", user.getPasswordHash()),
                () -> assertEquals(Instant.parse("2026-01-01T00:00:00Z"), user.getCreatedAt()));
    }

    @Test
    void createMapsPhoneAndRoleWithoutPasswordOrManagedFields() {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("New user");
        request.setUsername("new");
        request.setPassword("plain-password");
        request.setPhone("12345678901234567890");
        request.setRole(UserRole.ADMIN);

        User user = userMapper.toEntity(request);

        assertAll(
                () -> assertEquals("New user", user.getName()),
                () -> assertEquals("new", user.getUsername()),
                () -> assertEquals("12345678901234567890", user.getPhone()),
                () -> assertEquals(UserRole.ADMIN, user.getRole()),
                () -> assertNull(user.getPasswordHash()),
                () -> assertNull(user.getId()),
                () -> assertNull(user.getCreatedAt()));
    }

    @Test
    void responseAndListReturnPhoneAndRole() {
        User user = existingUser();

        UserResponse response = userMapper.toResponse(user);

        assertAll(
                () -> assertEquals("5550000000", response.getPhone()),
                () -> assertEquals(UserRole.OWNER, response.getRole()),
                () -> assertEquals(UserRole.OWNER,
                        userMapper.toResponseList(java.util.List.of(user)).getFirst().getRole()));
    }

    private User existingUser() {
        return User.builder()
                .id(1)
                .name("Original name")
                .username("original")
                .phone("5550000000")
                .role(UserRole.OWNER)
                .passwordHash("existing-hash")
                .active(false)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
