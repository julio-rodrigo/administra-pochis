package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.expensetypes.ExpenseTypeCreateRequest;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeResponse;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeUpdateRequest;
import com.administrapochi.models.entities.ExpenseType;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExpenseTypeMapper {

    ExpenseTypeResponse toResponse(ExpenseType expenseType);

    List<ExpenseTypeResponse> toResponseList(List<ExpenseType> expenseTypes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ExpenseType toEntity(ExpenseTypeCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ExpenseTypeUpdateRequest request, @MappingTarget ExpenseType expenseType);
}
