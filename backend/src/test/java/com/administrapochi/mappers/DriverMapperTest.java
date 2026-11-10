package com.administrapochi.mappers;

import com.administrapochi.dtos.drivers.DriverCreateRequest;
import com.administrapochi.dtos.drivers.DriverResponse;
import com.administrapochi.dtos.drivers.DriverUpdateRequest;
import com.administrapochi.models.BloodType;
import com.administrapochi.models.Driver;
import com.administrapochi.models.DriverStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverMapperTest {

    private final DriverMapper driverMapper = Mappers.getMapper(DriverMapper.class);

    @Test
    void toResponseBuildsFullNameWithoutNullOrExtraSpaces() {
        Driver driver = existingDriver();
        driver.setSecondLastName(" Lopez ");

        DriverResponse response = driverMapper.toResponse(driver);

        assertAll(
                () -> assertEquals("Alex Driver Lopez", response.getFullName()),
                () -> assertEquals(BloodType.O_POSITIVE, response.getBloodType()),
                () -> assertEquals(DriverStatus.ACTIVE, response.getEmploymentStatus()));

        driver.setSecondLastName(null);
        assertEquals("Alex Driver", driverMapper.toResponse(driver).getFullName());
    }

    @Test
    void toEntityMapsRequestAndIgnoresManagedFields() {
        DriverCreateRequest request = new DriverCreateRequest();
        request.setFirstName("Alex");
        request.setFirstLastName("Driver");
        request.setCurp("ABCD123456EFGHIJKL");
        request.setBloodType(BloodType.O_POSITIVE);

        Driver driver = driverMapper.toEntity(request);

        assertAll(
                () -> assertEquals("ABCD123456EFGHIJKL", driver.getCurp()),
                () -> assertEquals(BloodType.O_POSITIVE, driver.getBloodType()),
                () -> assertNull(driver.getId()),
                () -> assertNull(driver.getEmploymentStatus()),
                () -> assertNull(driver.getDeleted()),
                () -> assertNull(driver.getCreatedAt()));
    }

    @Test
    void updatePreservesIdentityAndSoftDeleteFields() {
        Driver driver = existingDriver();
        driver.setDeleted(true);
        driver.setDeletedAt(Instant.parse("2026-01-02T00:00:00Z"));
        DriverUpdateRequest request = new DriverUpdateRequest();
        request.setFirstName("Updated");
        request.setFirstLastName("Name");
        request.setBloodType(BloodType.A_NEGATIVE);
        request.setEmploymentStatus(DriverStatus.INACTIVE);

        driverMapper.updateEntity(request, driver);

        assertAll(
                () -> assertEquals(1, driver.getId()),
                () -> assertEquals("ABCD123456EFGHIJKL", driver.getCurp()),
                () -> assertEquals("Updated", driver.getFirstName()),
                () -> assertEquals(BloodType.A_NEGATIVE, driver.getBloodType()),
                () -> assertTrue(driver.getDeleted()),
                () -> assertEquals(Instant.parse("2026-01-02T00:00:00Z"), driver.getDeletedAt()));
    }

    private Driver existingDriver() {
        return Driver.builder()
                .id(1)
                .firstName("Alex")
                .firstLastName("Driver")
                .curp("ABCD123456EFGHIJKL")
                .bloodType(BloodType.O_POSITIVE)
                .employmentStatus(DriverStatus.ACTIVE)
                .deleted(false)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }
}
