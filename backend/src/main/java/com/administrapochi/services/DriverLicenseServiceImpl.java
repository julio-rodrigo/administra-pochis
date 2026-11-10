package com.administrapochi.services;

import com.administrapochi.models.dtos.driverlicenses.DriverLicenseCreateRequest;
import com.administrapochi.models.dtos.driverlicenses.DriverLicenseResponse;
import com.administrapochi.exceptions.DriverDeletedException;
import com.administrapochi.exceptions.DriverLicenseNotFoundException;
import com.administrapochi.exceptions.DriverNotFoundException;
import com.administrapochi.exceptions.DuplicateDriverLicenseNumberException;
import com.administrapochi.exceptions.InvalidDriverLicenseDateRangeException;
import com.administrapochi.models.mappers.DriverLicenseMapper;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.DriverLicense;
import com.administrapochi.repositories.DriverLicenseRepository;
import com.administrapochi.repositories.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverLicenseServiceImpl implements DriverLicenseService {

    private final DriverRepository driverRepository;
    private final DriverLicenseRepository driverLicenseRepository;
    private final DriverLicenseMapper driverLicenseMapper;

    @Override
    @Transactional
    public DriverLicenseResponse register(Integer driverId, DriverLicenseCreateRequest request) {
        Driver driver = findDriver(driverId);
        validateDateRange(request);

        String licenseNumber = request.getLicenseNumber().trim();
        if (driverLicenseRepository.existsByLicenseNumber(licenseNumber)) {
            throw new DuplicateDriverLicenseNumberException(licenseNumber);
        }

        driverLicenseRepository.findByDriverIdAndCurrentLicenseTrue(driverId).ifPresent(current -> {
            current.setCurrentLicense(false);
            driverLicenseRepository.saveAndFlush(current);
        });

        DriverLicense license = driverLicenseMapper.toEntity(request, driver);
        return driverLicenseMapper.toResponse(driverLicenseRepository.save(license));
    }

    @Override
    @Transactional(readOnly = true)
    public DriverLicenseResponse findCurrent(Integer driverId) {
        findDriver(driverId);
        DriverLicense license = driverLicenseRepository.findByDriverIdAndCurrentLicenseTrue(driverId)
                .orElseThrow(() -> new DriverLicenseNotFoundException(driverId));
        return driverLicenseMapper.toResponse(license);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverLicenseResponse> findHistory(Integer driverId) {
        findDriver(driverId);
        return driverLicenseMapper.toResponseList(
                driverLicenseRepository.findAllByDriverIdOrderByIssuedAtDescIdDesc(driverId));
    }

    @Override
    @Transactional
    public DriverLicenseResponse markNonCurrent(Integer driverId, Integer licenseId) {
        findDriver(driverId);
        DriverLicense license = driverLicenseRepository.findByIdAndDriverId(licenseId, driverId)
                .orElseThrow(() -> new DriverLicenseNotFoundException(driverId, licenseId));
        if (Boolean.TRUE.equals(license.getCurrentLicense())) {
            license.setCurrentLicense(false);
            license = driverLicenseRepository.save(license);
        }
        return driverLicenseMapper.toResponse(license);
    }

    private Driver findDriver(Integer driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new DriverNotFoundException(driverId));
        if (Boolean.TRUE.equals(driver.getDeleted())) {
            throw new DriverDeletedException(driverId);
        }
        return driver;
    }

    private void validateDateRange(DriverLicenseCreateRequest request) {
        if (request.getExpiresAt() == null || request.getIssuedAt() == null
                || !request.getExpiresAt().isAfter(request.getIssuedAt())) {
            throw new InvalidDriverLicenseDateRangeException();
        }
    }
}
