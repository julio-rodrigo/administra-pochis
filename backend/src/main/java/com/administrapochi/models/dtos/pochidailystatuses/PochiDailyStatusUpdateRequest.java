package com.administrapochi.models.dtos.pochidailystatuses;

import com.administrapochi.models.enums.PochiDailyStatusType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PochiDailyStatusUpdateRequest {

    @NotNull
    private LocalDate statusDate;

    @NotNull
    private PochiDailyStatusType status;

    @Size(max = 500)
    private String comment;
}
