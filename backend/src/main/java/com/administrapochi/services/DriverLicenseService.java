package com.administrapochi.services;

import com.administrapochi.dtos.driverlicenses.DriverLicenseCreateRequest;
import com.administrapochi.dtos.driverlicenses.DriverLicenseResponse;

import java.util.List;

public interface DriverLicenseService {

    DriverLicenseResponse register(Integer driverId, DriverLicenseCreateRequest request);

    DriverLicenseResponse findCurrent(Integer driverId);

    List<DriverLicenseResponse> findHistory(Integer driverId);

    DriverLicenseResponse markNonCurrent(Integer driverId, Integer licenseId);
}
