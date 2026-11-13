package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.driverdebts.DriverDebtResponse;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.DriverDebt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface DriverDebtMapper {

    @Mapping(target = "driverId", source = "debt.driver.id")
    @Mapping(target = "driverName", expression = "java(formatDriverName(debt.getDriver()))")
    @Mapping(target = "settlementId", source = "debt.settlement.id")
    @Mapping(target = "paidAmount", source = "paidAmount")
    @Mapping(target = "remainingAmount", expression = "java(debt.getAmount().subtract(paidAmount))")
    @Mapping(target = "createdById", source = "debt.createdBy.id")
    @Mapping(target = "createdByName", source = "debt.createdBy.name")
    @Mapping(target = "cancelledById", source = "debt.cancelledBy.id")
    @Mapping(target = "cancelledByName", source = "debt.cancelledBy.name")
    DriverDebtResponse toResponse(DriverDebt debt, BigDecimal paidAmount);

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
