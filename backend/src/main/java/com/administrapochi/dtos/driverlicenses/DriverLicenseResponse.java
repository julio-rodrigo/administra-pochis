package com.administrapochi.dtos.driverlicenses;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class DriverLicenseResponse {

    private Integer id;
    private Integer driverId;
    private String licenseType;
    private String licenseNumber;
    private LocalDate issuedAt;
    private LocalDate expiresAt;
    private String issuingMunicipality;
    private Boolean currentLicense;
    private boolean valid;
    private Instant createdAt;
    private Instant updatedAt;
}
