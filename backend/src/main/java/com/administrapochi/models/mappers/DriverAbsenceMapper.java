package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.driverabsences.DriverAbsenceCreateRequest;
import com.administrapochi.models.dtos.driverabsences.DriverAbsenceResponse;
import com.administrapochi.models.dtos.driverabsences.DriverAbsenceUpdateRequest;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.DriverAbsence;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DriverAbsenceMapper {

    @Mapping(target = "driverId", source = "driver.id")
    DriverAbsenceResponse toResponse(DriverAbsence absence);

    List<DriverAbsenceResponse> toResponseList(List<DriverAbsence> absences);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", source = "driver")
    @Mapping(target = "reason", expression = "java(request.getReason() == null || request.getReason().isBlank() ? null : request.getReason().trim())")
    @Mapping(target = "notes", expression = "java(request.getNotes() == null || request.getNotes().isBlank() ? null : request.getNotes().trim())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DriverAbsence toEntity(DriverAbsenceCreateRequest request, Driver driver);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "reason", expression = "java(request.getReason() == null || request.getReason().isBlank() ? null : request.getReason().trim())")
    @Mapping(target = "notes", expression = "java(request.getNotes() == null || request.getNotes().isBlank() ? null : request.getNotes().trim())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(DriverAbsenceUpdateRequest request, @MappingTarget DriverAbsence absence);
}
