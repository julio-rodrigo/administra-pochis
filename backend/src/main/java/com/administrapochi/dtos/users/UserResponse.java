package com.administrapochi.dtos.users;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UserResponse {

    private Integer id;
    private String name;
    private String username;
    private Boolean active;
    private Instant createdAt;
}
