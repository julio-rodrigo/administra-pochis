package com.administrapochi.services;

import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.repositories.AdministrationChargeSettlementRepository;
import com.administrapochi.repositories.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SettlementPochiAssignmentReferencePolicy implements PochiAssignmentReferencePolicy {

    private final SettlementRepository settlementRepository;
    private final AdministrationChargeSettlementRepository chargeSettlementRepository;

    @Override
    public boolean isReferenced(PochiAssignment assignment) {
        return chargeSettlementRepository.existsBySettlementPochiAssignmentId(assignment.getId())
                || settlementRepository.findSettledAssignmentIds(List.of(assignment.getId()))
                        .contains(assignment.getId());
    }
}
