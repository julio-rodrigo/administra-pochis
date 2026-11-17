package com.administrapochi.models.dtos.expensetypes;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseTypeUpdateRequest {

    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    @Size(max = 50)
    private String code;

    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    private Boolean requiresPochi;

    private Boolean active;
}
