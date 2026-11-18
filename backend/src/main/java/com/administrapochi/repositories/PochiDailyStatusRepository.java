package com.administrapochi.repositories;

import com.administrapochi.models.entities.PochiDailyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PochiDailyStatusRepository extends JpaRepository<PochiDailyStatus, Integer>,
        JpaSpecificationExecutor<PochiDailyStatus> {

    boolean existsByPochiIdAndStatusDate(Integer pochiId, LocalDate statusDate);

    boolean existsByPochiIdAndStatusDateAndIdNot(Integer pochiId, LocalDate statusDate, Integer id);

    Optional<PochiDailyStatus> findByPochiIdAndStatusDate(Integer pochiId, LocalDate statusDate);

    @Query("""
            SELECT dailyStatus.pochi.id AS pochiId,
                   dailyStatus.statusDate AS resolvedDate
            FROM PochiDailyStatus dailyStatus
            WHERE dailyStatus.pochi.id IN :pochiIds
              AND dailyStatus.statusDate BETWEEN :startDate AND :endDate
            """)
    List<PochiDayResolution> findResolvedPochiDays(
            @Param("pochiIds") List<Integer> pochiIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
