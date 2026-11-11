package com.administrapochi.services;

import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentCreateRequest;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentResponse;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentUpdateRequest;
import com.administrapochi.models.enums.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface PochiAssignmentService {

    List<PochiAssignmentResponse> create(PochiAssignmentCreateRequest request);

    PochiAssignmentResponse findById(Integer id);

    Page<PochiAssignmentResponse> findAll(
            LocalDate assignmentDate,
            LocalDate startDate,
            LocalDate endDate,
            Integer driverId,
            Integer pochiId,
            Shift shift,
            Pageable pageable);

    PochiAssignmentResponse update(Integer id, PochiAssignmentUpdateRequest request);

    void delete(Integer id);
}
