package com.administrapochi.models.dtos.drivers;

import com.administrapochi.models.enums.BloodType;
import com.administrapochi.models.enums.DriverStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverUpdateRequest {

    @NotBlank
    @Size(max = 40)
    private String firstName;

    @NotBlank
    @Size(max = 40)
    private String firstLastName;

    @Size(max = 40)
    private String secondLastName;

    @NotNull
    private BloodType bloodType;

    @NotNull
    private DriverStatus employmentStatus;
}
