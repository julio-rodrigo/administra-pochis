package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentCreateRequest;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentResponse;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentUpdateRequest;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.models.enums.Shift;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PochiAssignmentMapper {

    @Mapping(target = "pochiId", source = "pochi.id")
    @Mapping(target = "pochiIdentifier", source = "pochi.economicNumber")
    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "driverName", expression = "java(formatDriverName(assignment.getDriver()))")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.name")
    @Mapping(target = "updatedById", source = "updatedBy.id")
    @Mapping(target = "updatedByName", source = "updatedBy.name")
    PochiAssignmentResponse toResponse(PochiAssignment assignment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignmentDate", source = "request.assignmentDate")
    @Mapping(target = "pochi", source = "pochi")
    @Mapping(target = "driver", source = "driver")
    @Mapping(target = "shift", source = "shift")
    @Mapping(target = "notes", source = "request.notes")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    PochiAssignment toEntity(
            PochiAssignmentCreateRequest request, Pochi pochi, Driver driver, Shift shift);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pochi", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(PochiAssignmentUpdateRequest request, @MappingTarget PochiAssignment assignment);

    default String formatDriverName(Driver driver) {
        if (driver == null) {
            return null;
        }
        String name = driver.getFirstName() + " " + driver.getFirstLastName();
        if (driver.getSecondLastName() != null && !driver.getSecondLastName().isBlank()) {
            name += " " + driver.getSecondLastName();
        }
        return name;
    }
}
