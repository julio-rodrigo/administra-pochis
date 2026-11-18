package com.administrapochi.models.dtos.weeklyclosings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WeeklyClosingCreateRequest {

    @NotNull
    @Positive
    private Integer ownerId;

    @NotNull
    @Positive
    @Max(9999)
    private Integer weekYear;

    @NotNull
    @Positive
    @Max(53)
    private Integer weekNumber;
}
