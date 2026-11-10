package com.administrapochi.models.dtos.users;

import com.administrapochi.models.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UserResponse {

    private Integer id;
    private String name;
    private String username;
    private String phone;
    private UserRole role;
    private Boolean active;
    private Instant createdAt;
}
