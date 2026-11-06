package com.administrapochi.mappers;

import com.administrapochi.dtos.users.UserUpdateRequest;
import com.administrapochi.models.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void updateEntityChangesOnlyNameAndUsername() {
        User user = existingUser();
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Updated name");
        request.setUsername("updated");

        userMapper.updateEntity(request, user);

        assertAll(
                () -> assertEquals("Updated name", user.getName()),
                () -> assertEquals("updated", user.getUsername()),
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
                () -> assertEquals(1, user.getId()),
                () -> assertFalse(user.getActive()),
                () -> assertEquals("existing-hash", user.getPasswordHash()),
                () -> assertEquals(Instant.parse("2026-01-01T00:00:00Z"), user.getCreatedAt()));
    }

    private User existingUser() {
        return User.builder()
                .id(1)
                .name("Original name")
                .username("original")
                .passwordHash("existing-hash")
                .active(false)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
