package com.administrapochi.mappers;

import com.administrapochi.dtos.driverlicenses.DriverLicenseCreateRequest;
import com.administrapochi.dtos.driverlicenses.DriverLicenseResponse;
import com.administrapochi.models.Driver;
import com.administrapochi.models.DriverLicense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.util.List;

@Mapper(componentModel = "spring")
public interface DriverLicenseMapper {

    @Mapping(target = "driverId", source = "license.driver.id")
    @Mapping(target = "valid", expression = "java(isValid(license))")
    DriverLicenseResponse toResponse(DriverLicense license);

    List<DriverLicenseResponse> toResponseList(List<DriverLicense> licenses);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", source = "driver")
    @Mapping(target = "licenseType", expression = "java(request.getLicenseType() == null || request.getLicenseType().isBlank() ? \"C4\" : request.getLicenseType().trim())")
    @Mapping(target = "licenseNumber", expression = "java(request.getLicenseNumber().trim())")
    @Mapping(target = "issuingMunicipality", expression = "java(request.getIssuingMunicipality().trim())")
    @Mapping(target = "currentLicense", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DriverLicense toEntity(DriverLicenseCreateRequest request, Driver driver);

    default boolean isValid(DriverLicense license) {
        return !license.getExpiresAt().isBefore(LocalDate.now());
    }
}
