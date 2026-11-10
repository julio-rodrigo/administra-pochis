package com.administrapochi.dtos.driverlicenses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DriverLicenseCreateRequest {

    @Size(max = 20)
    private String licenseType;

    @NotBlank
    @Size(max = 50)
    private String licenseNumber;

    @NotNull
    private LocalDate issuedAt;

    @NotNull
    private LocalDate expiresAt;

    @NotBlank
    @Size(max = 40)
    private String issuingMunicipality;
}
