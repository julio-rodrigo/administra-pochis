package com.administrapochi.models.dtos.drivers;

import com.administrapochi.models.enums.DriverStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverStatusUpdateRequest {

    @NotNull
    private DriverStatus employmentStatus;
}
