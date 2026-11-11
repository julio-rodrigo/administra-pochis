package com.administrapochi.models.dtos.pochiassignments;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class PochiAssignmentResponse {

    private Integer id;
    private LocalDate assignmentDate;
    private String shift;
    private Integer pochiId;
    private String pochiIdentifier;
    private Integer driverId;
    private String driverName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer createdById;
    private String createdByName;
    private Integer updatedById;
    private String updatedByName;
}
