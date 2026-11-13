package com.administrapochi.models.dtos.driverdebts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverDebtCancelRequest {

    @NotBlank
    @Size(max = 500)
    private String cancellationReason;
}
