package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeResponse;
import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeSettlementSummary;
import com.administrapochi.models.entities.AdministrationCharge;
import com.administrapochi.models.entities.AdministrationChargeSettlement;
import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.models.entities.Settlement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AdministrationChargeMapper {

    @Mapping(target = "id", source = "charge.id")
    @Mapping(target = "chargeDate", source = "charge.chargeDate")
    @Mapping(target = "shiftCount", source = "charge.shiftCount")
    @Mapping(target = "suggestedAmount", source = "charge.suggestedAmount")
    @Mapping(target = "amount", source = "charge.amount")
    @Mapping(target = "note", source = "charge.note")
    @Mapping(target = "createdAt", source = "charge.createdAt")
    @Mapping(target = "pochiId", source = "charge.pochi.id")
    @Mapping(target = "pochiIdentifier", source = "charge.pochi.economicNumber")
    @Mapping(target = "administrationFeeRuleId", source = "charge.administrationFeeRule.id")
    @Mapping(target = "globalRule", expression = "java(charge.getAdministrationFeeRule().getPochi() == null)")
    @Mapping(target = "adjusted", expression = "java(charge.getAmount().compareTo(charge.getSuggestedAmount()) != 0)")
    @Mapping(target = "createdById", source = "charge.createdBy.id")
    @Mapping(target = "createdByName", source = "charge.createdBy.name")
    @Mapping(target = "settlements", source = "links")
    AdministrationChargeResponse toResponse(
            AdministrationCharge charge, List<AdministrationChargeSettlement> links);

    @Mapping(target = "settlementId", source = "settlement.id")
    @Mapping(target = "pochiAssignmentIds", expression = "java(assignmentIds(link.getSettlement()))")
    @Mapping(target = "shifts", expression = "java(shifts(link.getSettlement()))")
    @Mapping(target = "expectedAmount", source = "settlement.expectedAmount")
    @Mapping(target = "acceptedAmount", source = "settlement.acceptedAmount")
    @Mapping(target = "settledAt", source = "settlement.createdAt")
    AdministrationChargeSettlementSummary toSettlementSummary(AdministrationChargeSettlement link);

    default List<Integer> assignmentIds(Settlement settlement) {
        return settlement.getPochiAssignments().stream()
                .map(PochiAssignment::getId)
                .sorted()
                .toList();
    }

    default String shifts(Settlement settlement) {
        return settlement.getPochiAssignments().stream()
                .map(assignment -> assignment.getShift().name())
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));
    }
}
