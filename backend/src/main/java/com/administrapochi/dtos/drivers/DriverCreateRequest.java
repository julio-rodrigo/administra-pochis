package com.administrapochi.dtos.drivers;

import com.administrapochi.models.BloodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverCreateRequest {

    @NotBlank
    @Size(max = 40)
    private String firstName;

    @NotBlank
    @Size(max = 40)
    private String firstLastName;

    @Size(max = 40)
    private String secondLastName;

    @NotBlank
    @Pattern(regexp = "\\s*[A-Za-z0-9]{18}\\s*", message = "CURP must contain exactly 18 characters")
    private String curp;

    @NotNull
    private BloodType bloodType;
}
