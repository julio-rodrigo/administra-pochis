package com.administrapochi.services;

import com.administrapochi.dtos.driverabsences.DriverAbsenceCreateRequest;
import com.administrapochi.dtos.driverabsences.DriverAbsenceResponse;
import com.administrapochi.dtos.driverabsences.DriverAbsenceUpdateRequest;
import com.administrapochi.exceptions.DriverAbsenceNotFoundException;
import com.administrapochi.exceptions.DriverMustBeActiveException;
import com.administrapochi.exceptions.InvalidDriverAbsenceDateRangeException;
import com.administrapochi.mappers.DriverAbsenceMapper;
import com.administrapochi.models.BloodType;
import com.administrapochi.models.Driver;
import com.administrapochi.models.DriverAbsence;
import com.administrapochi.models.DriverStatus;
import com.administrapochi.repositories.DriverAbsenceRepository;
import com.administrapochi.repositories.DriverRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverAbsenceServiceImplTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DriverAbsenceRepository driverAbsenceRepository;

    @Mock
    private DriverAbsenceMapper driverAbsenceMapper;

    @InjectMocks
    private DriverAbsenceServiceImpl absenceService;

    @Test
    void registerSupportsOneDayAndMultiDayAbsences() {
        Driver driver = driver();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        registerRequest(driver, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10));
        registerRequest(driver, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 14));

        verify(driverAbsenceRepository, org.mockito.Mockito.times(2)).save(any(DriverAbsence.class));
    }

    @Test
    void registerRejectsInvalidRangeAndInactiveDriver() {
        Driver driver = driver();
        DriverAbsenceCreateRequest invalid = request(LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 10));
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        assertThrows(InvalidDriverAbsenceDateRangeException.class, () -> absenceService.register(1, invalid));
        verify(driverAbsenceRepository, never()).save(any(DriverAbsence.class));

        driver.setEmploymentStatus(DriverStatus.INACTIVE);
        assertThrows(DriverMustBeActiveException.class,
                () -> absenceService.register(1, request(LocalDate.now(), LocalDate.now())));
    }

    @Test
    void listsDriverAbsencesAndSupportsDateCoverageLookup() {
        Driver driver = driver();
        List<DriverAbsence> absences = List.of(new DriverAbsence());
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverAbsenceRepository.findAllByDriverIdOrderByStartDateDescIdDesc(1)).thenReturn(absences);
        when(driverAbsenceRepository.findAllByDriverIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                1, LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 12))).thenReturn(absences);
        when(driverAbsenceMapper.toResponseList(absences)).thenReturn(List.of(DriverAbsenceResponse.builder().build()));

        assertEquals(1, absenceService.findByDriver(1).size());
        assertEquals(1, absenceService.findCoveringDate(1, LocalDate.of(2026, 11, 12)).size());
        assertTrue(absenceService.findCoveringDate(1, LocalDate.of(2026, 11, 12)).get(0) != null);
    }

    @Test
    void updateChangesEditableFieldsAndKeepsDriverRelationship() {
        Driver driver = driver();
        DriverAbsence absence = DriverAbsence.builder()
                .id(5)
                .driver(driver)
                .startDate(LocalDate.of(2026, 11, 10))
                .endDate(LocalDate.of(2026, 11, 10))
                .reason("Old reason")
                .notes("Old notes")
                .build();
        DriverAbsenceUpdateRequest request = updateRequest(
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 14), "Updated reason", "Updated notes");
        DriverAbsenceResponse response = DriverAbsenceResponse.builder().id(5).reason("Updated reason").build();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        when(driverAbsenceRepository.findByIdAndDriverId(5, 1)).thenReturn(Optional.of(absence));
        when(driverAbsenceRepository.save(absence)).thenReturn(absence);
        when(driverAbsenceMapper.toResponse(absence)).thenReturn(response);

        assertEquals(response, absenceService.update(1, 5, request));

        assertEquals(driver, absence.getDriver());
        verify(driverAbsenceMapper).updateEntity(request, absence);
        verify(driverAbsenceRepository).save(absence);
    }

    @Test
    void updateRejectsInvalidDateRangeAndUnknownAbsence() {
        Driver driver = driver();
        when(driverRepository.findById(1)).thenReturn(Optional.of(driver));
        DriverAbsenceUpdateRequest invalid = updateRequest(
                LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 10), null, null);
        assertThrows(InvalidDriverAbsenceDateRangeException.class,
                () -> absenceService.update(1, 5, invalid));
        verify(driverAbsenceRepository, never()).findByIdAndDriverId(5, 1);

        when(driverAbsenceRepository.findByIdAndDriverId(99, 1)).thenReturn(Optional.empty());
        assertThrows(DriverAbsenceNotFoundException.class,
                () -> absenceService.update(1, 99, updateRequest(
                        LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10), null, null)));
    }

    private void registerRequest(Driver driver, LocalDate startDate, LocalDate endDate) {
        DriverAbsenceCreateRequest request = request(startDate, endDate);
        DriverAbsence absence = new DriverAbsence();
        when(driverAbsenceMapper.toEntity(request, driver)).thenReturn(absence);
        when(driverAbsenceRepository.save(absence)).thenReturn(absence);
        when(driverAbsenceMapper.toResponse(absence)).thenReturn(DriverAbsenceResponse.builder().build());

        absenceService.register(1, request);
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

    private DriverAbsenceCreateRequest request(LocalDate startDate, LocalDate endDate) {
        DriverAbsenceCreateRequest request = new DriverAbsenceCreateRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        return request;
    }

    private DriverAbsenceUpdateRequest updateRequest(
            LocalDate startDate, LocalDate endDate, String reason, String notes) {
        DriverAbsenceUpdateRequest request = new DriverAbsenceUpdateRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setReason(reason);
        request.setNotes(notes);
        return request;
    }
}
