package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.expenses.ExpenseResponse;
import com.administrapochi.models.dtos.expenses.CreateExpenseRequest;
import com.administrapochi.models.dtos.expenses.UpdateExpenseRequest;
import com.administrapochi.models.entities.Expense;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "expenseType", ignore = true)
    @Mapping(target = "pochi", ignore = true)
    @Mapping(target = "paidByUser", ignore = true)
    @Mapping(target = "registeredByUser", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancelledByUser", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Expense toEntity(CreateExpenseRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "expenseType", ignore = true)
    @Mapping(target = "pochi", ignore = true)
    @Mapping(target = "paidByUser", ignore = true)
    @Mapping(target = "registeredByUser", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancelledByUser", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateExpenseRequest request, @MappingTarget Expense expense);

    @Mapping(target = "expenseTypeId", source = "expenseType.id")
    @Mapping(target = "expenseTypeCode", source = "expenseType.code")
    @Mapping(target = "expenseTypeName", source = "expenseType.name")
    @Mapping(target = "pochiId", source = "pochi.id")
    @Mapping(target = "pochiIdentifier", source = "pochi.economicNumber")
    @Mapping(target = "paidByUserId", source = "paidByUser.id")
    @Mapping(target = "paidByUserName", source = "paidByUser.name")
    @Mapping(target = "registeredByUserId", source = "registeredByUser.id")
    @Mapping(target = "registeredByUserName", source = "registeredByUser.name")
    @Mapping(target = "cancelledByUserId", source = "cancelledByUser.id")
    @Mapping(target = "cancelledByUserName", source = "cancelledByUser.name")
    ExpenseResponse toResponse(Expense expense);
}
