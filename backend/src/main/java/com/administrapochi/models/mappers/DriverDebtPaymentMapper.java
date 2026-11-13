package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.driverdebts.DriverDebtPaymentResponse;
import com.administrapochi.models.entities.DriverDebtPayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DriverDebtPaymentMapper {

    @Mapping(target = "driverDebtId", source = "driverDebt.id")
    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", source = "createdBy.name")
    DriverDebtPaymentResponse toResponse(DriverDebtPayment payment);
}
