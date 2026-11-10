package com.administrapochi.models.dtos.driverabsences;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DriverAbsenceUpdateRequest {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @Size(max = 100)
    private String reason;

    @Size(max = 500)
    private String notes;
}
