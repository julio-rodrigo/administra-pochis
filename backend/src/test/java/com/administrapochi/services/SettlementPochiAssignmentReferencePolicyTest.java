package com.administrapochi.services;

import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.repositories.AdministrationChargeSettlementRepository;
import com.administrapochi.repositories.SettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementPochiAssignmentReferencePolicyTest {

    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private AdministrationChargeSettlementRepository chargeSettlementRepository;

    @InjectMocks
    private SettlementPochiAssignmentReferencePolicy policy;

    @Test
    void treatsChargeLinkedAssignmentAsReferenced() {
        PochiAssignment assignment = PochiAssignment.builder().id(101).build();
        when(chargeSettlementRepository.existsBySettlementPochiAssignmentId(101)).thenReturn(true);

        assertThat(policy.isReferenced(assignment)).isTrue();

        verify(settlementRepository, org.mockito.Mockito.never()).findSettledAssignmentIds(List.of(101));
    }

    @Test
    void retainsExistingSettlementReferenceValidation() {
        PochiAssignment assignment = PochiAssignment.builder().id(101).build();
        when(chargeSettlementRepository.existsBySettlementPochiAssignmentId(101)).thenReturn(false);
        when(settlementRepository.findSettledAssignmentIds(List.of(101))).thenReturn(Set.of(101));

        assertThat(policy.isReferenced(assignment)).isTrue();
    }
}
