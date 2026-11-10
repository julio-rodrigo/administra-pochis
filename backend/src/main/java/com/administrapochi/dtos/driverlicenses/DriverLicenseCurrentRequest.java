package com.administrapochi.dtos.driverlicenses;

import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverLicenseCurrentRequest {

    @NotNull
    @AssertFalse
    private Boolean currentLicense;
}
