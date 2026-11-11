package com.administrapochi.models.dtos.pochiassignments;

import com.administrapochi.models.enums.AssignmentShiftOption;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PochiAssignmentCreateRequest {

    @NotNull
    private LocalDate assignmentDate;

    @NotNull
    private Integer pochiId;

    @NotNull
    private Integer driverId;

    @NotNull
    private AssignmentShiftOption shift;

    @Size(max = 500)
    private String notes;
}
