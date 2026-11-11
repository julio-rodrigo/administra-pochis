package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.settlementrules.SettlementRuleCreateRequest;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleResponse;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleUpdateRequest;
import com.administrapochi.models.entities.SettlementRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SettlementRuleMapper {

    @Mapping(target = "pochiId", source = "pochi.id")
    @Mapping(target = "pochiEconomicNumber", source = "pochi.economicNumber")
    SettlementRuleResponse toResponse(SettlementRule rule);

    List<SettlementRuleResponse> toResponseList(List<SettlementRule> rules);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pochi", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SettlementRule toEntity(SettlementRuleCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pochi", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(SettlementRuleUpdateRequest request, @MappingTarget SettlementRule rule);
}
