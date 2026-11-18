package com.administrapochi.models.dtos.weeklyclosings;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WeeklyClosingReopenRequest {

    @NotBlank
    private String reason;
}
