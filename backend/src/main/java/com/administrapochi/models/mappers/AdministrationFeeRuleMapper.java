package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleCreateRequest;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleResponse;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleUpdateRequest;
import com.administrapochi.models.entities.AdministrationFeeRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdministrationFeeRuleMapper {

    @Mapping(target = "pochiId", source = "pochi.id")
    @Mapping(target = "pochiIdentifier", source = "pochi.economicNumber")
    @Mapping(target = "global", expression = "java(rule.getPochi() == null)")
    AdministrationFeeRuleResponse toResponse(AdministrationFeeRule rule);

    List<AdministrationFeeRuleResponse> toResponseList(List<AdministrationFeeRule> rules);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pochi", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    AdministrationFeeRule toEntity(AdministrationFeeRuleCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pochi", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            AdministrationFeeRuleUpdateRequest request, @MappingTarget AdministrationFeeRule rule);
}
