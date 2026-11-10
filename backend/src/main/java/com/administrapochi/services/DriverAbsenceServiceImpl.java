package com.administrapochi.services;

import com.administrapochi.models.dtos.driverabsences.DriverAbsenceCreateRequest;
import com.administrapochi.models.dtos.driverabsences.DriverAbsenceResponse;
import com.administrapochi.models.dtos.driverabsences.DriverAbsenceUpdateRequest;
import com.administrapochi.exceptions.DriverDeletedException;
import com.administrapochi.exceptions.DriverAbsenceNotFoundException;
import com.administrapochi.exceptions.DriverMustBeActiveException;
import com.administrapochi.exceptions.DriverNotFoundException;
import com.administrapochi.exceptions.InvalidDriverAbsenceDateRangeException;
import com.administrapochi.models.mappers.DriverAbsenceMapper;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.DriverAbsence;
import com.administrapochi.models.enums.DriverStatus;
import com.administrapochi.repositories.DriverAbsenceRepository;
import com.administrapochi.repositories.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverAbsenceServiceImpl implements DriverAbsenceService {

    private final DriverRepository driverRepository;
    private final DriverAbsenceRepository driverAbsenceRepository;
    private final DriverAbsenceMapper driverAbsenceMapper;

    @Override
    @Transactional
    public DriverAbsenceResponse register(Integer driverId, DriverAbsenceCreateRequest request) {
        Driver driver = findDriver(driverId);
        if (driver.getEmploymentStatus() != DriverStatus.ACTIVE) {
            throw new DriverMustBeActiveException(driverId);
        }
        if (request.getStartDate() == null || request.getEndDate() == null
                || request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidDriverAbsenceDateRangeException();
        }

        DriverAbsence absence = driverAbsenceMapper.toEntity(request, driver);
        return driverAbsenceMapper.toResponse(driverAbsenceRepository.save(absence));
    }

    @Override
    @Transactional
    public DriverAbsenceResponse update(
            Integer driverId, Integer absenceId, DriverAbsenceUpdateRequest request) {
        findDriver(driverId);
        if (request.getStartDate() == null || request.getEndDate() == null
                || request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidDriverAbsenceDateRangeException();
        }
        DriverAbsence absence = driverAbsenceRepository.findByIdAndDriverId(absenceId, driverId)
                .orElseThrow(() -> new DriverAbsenceNotFoundException(driverId, absenceId));
        driverAbsenceMapper.updateEntity(request, absence);
        return driverAbsenceMapper.toResponse(driverAbsenceRepository.save(absence));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverAbsenceResponse> findByDriver(Integer driverId) {
        findDriver(driverId);
        return driverAbsenceMapper.toResponseList(
                driverAbsenceRepository.findAllByDriverIdOrderByStartDateDescIdDesc(driverId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverAbsenceResponse> findCoveringDate(Integer driverId, LocalDate date) {
        findDriver(driverId);
        return driverAbsenceMapper.toResponseList(
                driverAbsenceRepository.findAllByDriverIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        driverId, date, date));
    }

    private Driver findDriver(Integer driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new DriverNotFoundException(driverId));
        if (Boolean.TRUE.equals(driver.getDeleted())) {
            throw new DriverDeletedException(driverId);
        }
        return driver;
    }
}
