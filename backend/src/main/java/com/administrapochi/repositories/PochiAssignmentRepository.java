package com.administrapochi.repositories;

import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.models.enums.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

public interface PochiAssignmentRepository extends
        JpaRepository<PochiAssignment, Integer>,
        JpaSpecificationExecutor<PochiAssignment> {

    boolean existsByAssignmentDateAndPochiIdAndShift(
            LocalDate assignmentDate, Integer pochiId, Shift shift);

    boolean existsByAssignmentDateAndDriverIdAndShift(
            LocalDate assignmentDate, Integer driverId, Shift shift);

    boolean existsByAssignmentDateAndPochiIdAndShiftAndIdNot(
            LocalDate assignmentDate, Integer pochiId, Shift shift, Integer id);

    boolean existsByAssignmentDateAndDriverIdAndShiftAndIdNot(
            LocalDate assignmentDate, Integer driverId, Shift shift, Integer id);
}
