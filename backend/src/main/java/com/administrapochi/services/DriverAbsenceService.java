package com.administrapochi.services;

import com.administrapochi.dtos.driverabsences.DriverAbsenceCreateRequest;
import com.administrapochi.dtos.driverabsences.DriverAbsenceResponse;
import com.administrapochi.dtos.driverabsences.DriverAbsenceUpdateRequest;

import java.time.LocalDate;
import java.util.List;

public interface DriverAbsenceService {

    DriverAbsenceResponse register(Integer driverId, DriverAbsenceCreateRequest request);

    DriverAbsenceResponse update(Integer driverId, Integer absenceId, DriverAbsenceUpdateRequest request);

    List<DriverAbsenceResponse> findByDriver(Integer driverId);

    List<DriverAbsenceResponse> findCoveringDate(Integer driverId, LocalDate date);
}
