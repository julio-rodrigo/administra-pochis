package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusCreateRequest;
import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusResponse;
import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusUpdateRequest;
import com.administrapochi.models.entities.PochiDailyStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PochiDailyStatusMapper {

    @Mapping(target = "pochiId", source = "pochi.id")
    @Mapping(target = "pochiIdentifier", source = "pochi.economicNumber")
    @Mapping(target = "createdBy", source = "createdBy.id")
    @Mapping(target = "updatedBy", source = "updatedBy.id")
    PochiDailyStatusResponse toResponse(PochiDailyStatus dailyStatus);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pochi", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PochiDailyStatus toEntity(PochiDailyStatusCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pochi", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(PochiDailyStatusUpdateRequest request, @MappingTarget PochiDailyStatus dailyStatus);
}
