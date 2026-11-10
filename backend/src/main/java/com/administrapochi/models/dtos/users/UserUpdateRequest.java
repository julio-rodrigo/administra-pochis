package com.administrapochi.models.dtos.users;

import com.administrapochi.models.enums.UserRole;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    @Size(max = 100)
    private String name;

    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    @Size(max = 50)
    private String username;

    @Size(max = 20)
    private String phone;

    private UserRole role;
}
