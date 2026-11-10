package com.administrapochi.repositories;

import com.administrapochi.models.DriverAbsence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DriverAbsenceRepository extends JpaRepository<DriverAbsence, Integer> {

    List<DriverAbsence> findAllByDriverIdOrderByStartDateDescIdDesc(Integer driverId);

    Optional<DriverAbsence> findByIdAndDriverId(Integer id, Integer driverId);

    List<DriverAbsence> findAllByDriverIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Integer driverId, LocalDate date, LocalDate sameDate);
}
