package com.administrapochi.services;

import com.administrapochi.models.dtos.drivers.DriverCreateRequest;
import com.administrapochi.models.dtos.drivers.DriverResponse;
import com.administrapochi.models.dtos.drivers.DriverUpdateRequest;
import com.administrapochi.exceptions.DriverDeletedException;
import com.administrapochi.exceptions.DriverNotFoundException;
import com.administrapochi.exceptions.DuplicateCurpException;
import com.administrapochi.models.mappers.DriverMapper;
import com.administrapochi.models.enums.BloodType;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.enums.DriverStatus;
import com.administrapochi.repositories.DriverRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverMapper driverMapper;

    @InjectMocks
    private DriverServiceImpl driverService;

    @Test
    void findAllReturnsOnlyVisibleDriversFromRepository() {
        List<Driver> drivers = List.of(existingDriver());
        List<DriverResponse> responses = List.of(response());
        when(driverRepository.findAllByDeletedFalseOrderByIdAsc()).thenReturn(drivers);
        when(driverMapper.toResponseList(drivers)).thenReturn(responses);

        assertSame(responses, driverService.findAll());
        verify(driverMapper).toResponseList(drivers);
    }

    @Test
    void findDeletedReturnsMappedDeletedDrivers() {
        List<Driver> drivers = List.of(deletedDriver());
        List<DriverResponse> responses = List.of(response());
        when(driverRepository.findAllByDeletedTrueOrderByIdAsc()).thenReturn(drivers);
        when(driverMapper.toResponseList(drivers)).thenReturn(responses);

        assertSame(responses, driverService.findDeleted());
        verify(driverMapper).toResponseList(drivers);
    }

    @Test
    void findByIdReturnsMappedDriver() {
        Driver driver = existingDriver();
        DriverResponse response = response();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverMapper.toResponse(driver)).thenReturn(response);

        assertSame(response, driverService.findById(1));
    }

    @Test
    void findByIdRejectsMissingOrDeletedDriver() {
        when(driverRepository.findById(2)).thenReturn(Optional.empty());
        when(driverRepository.findById(1)).thenReturn(Optional.of(deletedDriver()));

        assertAll(
                () -> assertThrows(DriverNotFoundException.class, () -> driverService.findById(2)),
                () -> assertThrows(DriverDeletedException.class, () -> driverService.findById(1)));
        verifyNoMoreInteractions(driverMapper);
    }

    @Test
    void createNormalizesCurpAndSetsInitialLifecycleValues() {
        DriverCreateRequest request = createRequest("  abcdefghijklmnopqr ");
        Driver driver = new Driver();
        when(driverRepository.existsByCurp("ABCDEFGHIJKLMNOPQR")).thenReturn(false);
        when(driverMapper.toEntity(request)).thenReturn(driver);
        when(driverRepository.save(driver)).thenReturn(driver);
        when(driverMapper.toResponse(driver)).thenReturn(response());

        driverService.create(request);

        assertAll(
                () -> assertEquals("ABCDEFGHIJKLMNOPQR", driver.getCurp()),
                () -> assertEquals(DriverStatus.ACTIVE, driver.getEmploymentStatus()),
                () -> assertFalse(driver.getDeleted()));
    }

    @Test
    void createRejectsDuplicateCurpIncludingHistoricalRecords() {
        DriverCreateRequest request = createRequest("abcdefghijklmnopqr");
        when(driverRepository.existsByCurp("ABCDEFGHIJKLMNOPQR")).thenReturn(true);

        assertThrows(DuplicateCurpException.class, () -> driverService.create(request));

        verify(driverRepository, never()).save(any(Driver.class));
        verifyNoMoreInteractions(driverMapper);
    }

    @Test
    void updateMapsEditableFieldsButPreservesIdentityAndNormalizesNames() {
        Driver driver = existingDriver();
        DriverUpdateRequest request = updateRequest();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverRepository.save(driver)).thenReturn(driver);
        when(driverMapper.toResponse(driver)).thenReturn(response());

        driverService.update(1, request);

        assertAll(
                () -> assertEquals("New", driver.getFirstName()),
                () -> assertEquals("Driver", driver.getFirstLastName()),
                () -> assertNull(driver.getSecondLastName()),
                () -> assertEquals("ABCD123456EFGHIJKL", driver.getCurp()),
                () -> assertEquals(DriverStatus.ACTIVE, driver.getEmploymentStatus()));
        verify(driverMapper).updateEntity(request, driver);
    }

    @Test
    void activateAndDeactivateChangeEmploymentStatusInBothDirections() {
        Driver driver = existingDriver();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverRepository.save(driver)).thenReturn(driver);
        when(driverMapper.toResponse(driver)).thenReturn(response());

        driverService.deactivate(1);
        assertEquals(DriverStatus.INACTIVE, driver.getEmploymentStatus());

        driverService.activate(1);
        assertEquals(DriverStatus.ACTIVE, driver.getEmploymentStatus());
        verify(driverRepository, org.mockito.Mockito.times(2)).save(driver);
    }

    @Test
    void activateAndDeactivateAreIdempotentWhenAlreadyInRequestedState() {
        Driver driver = existingDriver();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverMapper.toResponse(driver)).thenReturn(response());

        driverService.activate(1);
        verify(driverRepository, never()).save(any(Driver.class));

        driver.setEmploymentStatus(DriverStatus.INACTIVE);
        driverService.deactivate(1);
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void deleteLogicallyDeletesAndDeactivatesDriver() {
        Driver driver = existingDriver();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverRepository.save(driver)).thenReturn(driver);

        driverService.delete(1);

        assertAll(
                () -> assertTrue(driver.getDeleted()),
                () -> assertEquals(DriverStatus.INACTIVE, driver.getEmploymentStatus()),
                () -> assertTrue(driver.getDeletedAt().isAfter(Instant.EPOCH)));
    }

    @Test
    void restoreClearsLogicalDeletionButDoesNotReactivateEmployment() {
        Driver driver = deletedDriver();
        driver.setDeletedAt(Instant.parse("2026-01-02T00:00:00Z"));
        DriverResponse response = response();
        when(driverRepository.findByIdAndDeletedTrue(1)).thenReturn(Optional.of(driver));
        when(driverRepository.save(driver)).thenReturn(driver);
        when(driverMapper.toResponse(driver)).thenReturn(response);

        assertSame(response, driverService.restore(1));

        assertAll(
                () -> assertFalse(driver.getDeleted()),
                () -> assertNull(driver.getDeletedAt()),
                () -> assertEquals(DriverStatus.INACTIVE, driver.getEmploymentStatus()));
    }

    @Test
    void restoreRejectsIdThatDoesNotIdentifyDeletedDriver() {
        when(driverRepository.findByIdAndDeletedTrue(2)).thenReturn(Optional.empty());

        assertThrows(DriverNotFoundException.class, () -> driverService.restore(2));
        verify(driverRepository, never()).save(any(Driver.class));
        verifyNoMoreInteractions(driverMapper);
    }

    private Driver existingDriver() {
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

    private Driver deletedDriver() {
        Driver driver = existingDriver();
        driver.setDeleted(true);
        driver.setEmploymentStatus(DriverStatus.INACTIVE);
        return driver;
    }

    private DriverResponse response() {
        return DriverResponse.builder().id(1).firstName("Test").build();
    }

    private DriverCreateRequest createRequest(String curp) {
        DriverCreateRequest request = new DriverCreateRequest();
        request.setFirstName("Test");
        request.setFirstLastName("Driver");
        request.setCurp(curp);
        request.setBloodType(BloodType.O_POSITIVE);
        return request;
    }

    private DriverUpdateRequest updateRequest() {
        DriverUpdateRequest request = new DriverUpdateRequest();
        request.setFirstName(" New ");
        request.setFirstLastName(" Driver ");
        request.setSecondLastName(" ");
        request.setBloodType(BloodType.A_POSITIVE);
        request.setEmploymentStatus(DriverStatus.ACTIVE);
        return request;
    }
}
