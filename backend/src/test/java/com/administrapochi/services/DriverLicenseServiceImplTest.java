package com.administrapochi.services;

import com.administrapochi.models.dtos.driverlicenses.DriverLicenseCreateRequest;
import com.administrapochi.models.dtos.driverlicenses.DriverLicenseResponse;
import com.administrapochi.exceptions.DriverDeletedException;
import com.administrapochi.exceptions.DriverLicenseNotFoundException;
import com.administrapochi.exceptions.DuplicateDriverLicenseNumberException;
import com.administrapochi.exceptions.InvalidDriverLicenseDateRangeException;
import com.administrapochi.models.mappers.DriverLicenseMapper;
import com.administrapochi.models.enums.BloodType;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.DriverLicense;
import com.administrapochi.models.enums.DriverStatus;
import com.administrapochi.repositories.DriverLicenseRepository;
import com.administrapochi.repositories.DriverRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLicenseServiceImplTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverLicenseRepository driverLicenseRepository;

    @Mock
    private DriverLicenseMapper driverLicenseMapper;

    @InjectMocks
    private DriverLicenseServiceImpl licenseService;

    @Test
    void registerCreatesCurrentLicenseWithDefaultType() {
        Driver driver = driver();
        DriverLicenseCreateRequest request = request("LIC-1", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));
        DriverLicense license = new DriverLicense();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverLicenseRepository.existsByLicenseNumber("LIC-1")).thenReturn(false);
        when(driverLicenseMapper.toEntity(request, driver)).thenReturn(license);
        when(driverLicenseRepository.save(license)).thenReturn(license);
        when(driverLicenseMapper.toResponse(license))
                .thenReturn(DriverLicenseResponse.builder().licenseType("C4").currentLicense(true).build());

        DriverLicenseResponse response = licenseService.register(1, request);

        assertAll(
                () -> assertEquals("C4", response.getLicenseType()),
                () -> assertTrue(response.getCurrentLicense()));
    }

    @Test
    void registeringRenewalMarksPreviousLicenseNonCurrentBeforeSavingNewOne() {
        Driver driver = driver();
        DriverLicense previous = DriverLicense.builder().id(10).driver(driver).currentLicense(true).build();
        DriverLicense replacement = new DriverLicense();
        DriverLicenseCreateRequest request = request("LIC-2", LocalDate.of(2026, 1, 1), LocalDate.of(2028, 1, 1));
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverLicenseRepository.existsByLicenseNumber("LIC-2")).thenReturn(false);
        when(driverLicenseRepository.findByDriverIdAndCurrentLicenseTrue(1)).thenReturn(Optional.of(previous));
        when(driverLicenseRepository.saveAndFlush(previous)).thenReturn(previous);
        when(driverLicenseMapper.toEntity(request, driver)).thenReturn(replacement);
        when(driverLicenseRepository.save(replacement)).thenReturn(replacement);
        when(driverLicenseMapper.toResponse(replacement))
                .thenReturn(DriverLicenseResponse.builder().currentLicense(true).build());

        licenseService.register(1, request);

        assertFalse(previous.getCurrentLicense());
        verify(driverLicenseRepository).saveAndFlush(previous);
        verify(driverLicenseRepository).save(replacement);
    }

    @Test
    void registerRejectsDuplicateNumberAndInvalidDateRangeWithoutChangingCurrentLicense() {
        Driver driver = driver();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverLicenseRepository.existsByLicenseNumber("LIC-1")).thenReturn(true);
        assertThrows(DuplicateDriverLicenseNumberException.class,
                () -> licenseService.register(1, request("LIC-1", LocalDate.now(), LocalDate.now().plusDays(1))));

        assertThrows(InvalidDriverLicenseDateRangeException.class,
                () -> licenseService.register(1, request("LIC-2", LocalDate.now(), LocalDate.now())));

        verify(driverLicenseRepository, never()).findByDriverIdAndCurrentLicenseTrue(1);
        verify(driverLicenseRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void findCurrentAndHistoryRequireVisibleDriverAndReturnMappedResults() {
        Driver driver = driver();
        DriverLicense current = DriverLicense.builder().driver(driver).build();
        List<DriverLicense> history = List.of(current);
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverLicenseRepository.findByDriverIdAndCurrentLicenseTrue(1)).thenReturn(Optional.of(current));
        when(driverLicenseRepository.findAllByDriverIdOrderByIssuedAtDescIdDesc(1)).thenReturn(history);
        when(driverLicenseMapper.toResponse(current))
                .thenReturn(DriverLicenseResponse.builder().build());
        when(driverLicenseMapper.toResponseList(history))
                .thenReturn(List.of());

        licenseService.findCurrent(1);
        assertTrue(licenseService.findHistory(1).isEmpty());

        when(driverLicenseRepository.findByDriverIdAndCurrentLicenseTrue(1)).thenReturn(Optional.empty());
        assertThrows(DriverLicenseNotFoundException.class, () -> licenseService.findCurrent(1));

        driver.setDeleted(true);
        assertThrows(DriverDeletedException.class, () -> licenseService.findHistory(1));
    }

    @Test
    void markNonCurrentSetsLicenseFalseByIdAndDriver() {
        Driver driver = driver();
        DriverLicense license = DriverLicense.builder()
                .id(10)
                .driver(driver)
                .currentLicense(true)
                .build();
        DriverLicenseResponse response = DriverLicenseResponse.builder().currentLicense(false).build();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverLicenseRepository.findByIdAndDriverId(10, 1)).thenReturn(Optional.of(license));
        when(driverLicenseRepository.save(license)).thenReturn(license);
        when(driverLicenseMapper.toResponse(license)).thenReturn(response);

        assertEquals(response, licenseService.markNonCurrent(1, 10));
        assertFalse(license.getCurrentLicense());
        verify(driverLicenseRepository).save(license);
    }

    @Test
    void markNonCurrentIsIdempotentAndRejectsMissingLicense() {
        Driver driver = driver();
        DriverLicense license = DriverLicense.builder()
                .id(10)
                .driver(driver)
                .currentLicense(false)
                .build();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverLicenseRepository.findByIdAndDriverId(10, 1)).thenReturn(Optional.of(license));
        when(driverLicenseMapper.toResponse(license)).thenReturn(DriverLicenseResponse.builder().build());

        licenseService.markNonCurrent(1, 10);
        verify(driverLicenseRepository, never()).save(license);

        when(driverLicenseRepository.findByIdAndDriverId(99, 1)).thenReturn(Optional.empty());
        assertThrows(DriverLicenseNotFoundException.class, () -> licenseService.markNonCurrent(1, 99));
    }

    private Driver driver() {
        return Driver.builder()
                .id(1)
                .firstName("Test")
                .firstLastName("Driver")
                .curp("ABCD123456EFGHIJKL")
                .bloodType(BloodType.O_POSITIVE)
                .employmentStatus(DriverStatus.ACTIVE)
                .deleted(false)
                .build();
    }

    private DriverLicenseCreateRequest request(String number, LocalDate issuedAt, LocalDate expiresAt) {
        DriverLicenseCreateRequest request = new DriverLicenseCreateRequest();
        request.setLicenseNumber(number);
        request.setIssuedAt(issuedAt);
        request.setExpiresAt(expiresAt);
        request.setIssuingMunicipality("Municipality");
        return request;
    }
}
