package com.administrapochi.repositories;

import com.administrapochi.models.entities.PochiDailyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

public interface PochiDailyStatusRepository extends JpaRepository<PochiDailyStatus, Integer>,
        JpaSpecificationExecutor<PochiDailyStatus> {

    boolean existsByPochiIdAndStatusDate(Integer pochiId, LocalDate statusDate);

    boolean existsByPochiIdAndStatusDateAndIdNot(Integer pochiId, LocalDate statusDate, Integer id);

    Optional<PochiDailyStatus> findByPochiIdAndStatusDate(Integer pochiId, LocalDate statusDate);
}
