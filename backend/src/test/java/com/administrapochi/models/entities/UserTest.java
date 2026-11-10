package com.administrapochi.models.entities;

import com.administrapochi.models.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void creationDefaultsRoleToOwnerAndLeavesPhoneOptional() {
        User user = new User();

        user.applyDatabaseDefaults();

        assertAll(
                () -> assertEquals(UserRole.OWNER, user.getRole()),
                () -> assertNull(user.getPhone()),
                () -> assertTrue(user.getActive()),
                () -> assertNotNull(user.getCreatedAt()));
    }

    @Test
    void creationPreservesExplicitAdminRoleAndPhone() {
        User user = User.builder().role(UserRole.ADMIN).phone("5551234567").build();

        user.applyDatabaseDefaults();

        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals("5551234567", user.getPhone());
    }
}
