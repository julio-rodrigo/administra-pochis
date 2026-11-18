package com.administrapochi.repositories;

import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.models.enums.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PochiAssignmentRepository extends
        JpaRepository<PochiAssignment, Integer>,
        JpaSpecificationExecutor<PochiAssignment> {

    boolean existsByPochiIdAndAssignmentDate(Integer pochiId, LocalDate assignmentDate);

    boolean existsByAssignmentDateAndPochiIdAndShift(
            LocalDate assignmentDate, Integer pochiId, Shift shift);

    boolean existsByAssignmentDateAndDriverIdAndShift(
            LocalDate assignmentDate, Integer driverId, Shift shift);

    boolean existsByAssignmentDateAndPochiIdAndShiftAndIdNot(
            LocalDate assignmentDate, Integer pochiId, Shift shift, Integer id);

    boolean existsByAssignmentDateAndDriverIdAndShiftAndIdNot(
            LocalDate assignmentDate, Integer driverId, Shift shift, Integer id);

    @Query("""
            SELECT assignment FROM PochiAssignment assignment
            JOIN FETCH assignment.pochi
            WHERE assignment.pochi.id = :pochiId
              AND assignment.assignmentDate = :assignmentDate
            ORDER BY assignment.shift
            """)
    List<PochiAssignment> findForAdministrationCharge(
            @Param("pochiId") Integer pochiId, @Param("assignmentDate") LocalDate assignmentDate);

    @Query("""
            SELECT DISTINCT assignment.pochi.id AS pochiId,
                   assignment.assignmentDate AS resolvedDate
            FROM PochiAssignment assignment
            WHERE assignment.pochi.id IN :pochiIds
              AND assignment.assignmentDate BETWEEN :startDate AND :endDate
            """)
    List<PochiDayResolution> findResolvedPochiDays(
            @Param("pochiIds") List<Integer> pochiIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
