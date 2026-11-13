package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.settlements.SettlementResponse;
import com.administrapochi.models.dtos.settlements.SettlementUpdateRequest;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.Settlement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = PochiAssignmentMapper.class)
public interface SettlementMapper {

    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "driverName", expression = "java(formatDriverName(settlement.getDriver()))")
    @Mapping(target = "pochiId", source = "pochi.id")
    @Mapping(target = "pochiIdentifier", source = "pochi.economicNumber")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.name")
    @Mapping(target = "modifiedById", source = "modifiedBy.id")
    @Mapping(target = "modifiedByName", source = "modifiedBy.name")
    SettlementResponse toResponse(Settlement settlement);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "pochi", ignore = true)
    @Mapping(target = "workDate", ignore = true)
    @Mapping(target = "expectedAmount", ignore = true)
    @Mapping(target = "pochiAssignments", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(SettlementUpdateRequest request, @MappingTarget Settlement settlement);

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
