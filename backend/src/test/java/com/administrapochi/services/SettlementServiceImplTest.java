package com.administrapochi.services;

import com.administrapochi.exceptions.InvalidSettlementException;
import com.administrapochi.exceptions.PochiAssignmentAlreadySettledException;
import com.administrapochi.exceptions.PochiAssignmentNotFoundException;
import com.administrapochi.exceptions.AdministrationChargeConflictException;
import com.administrapochi.exceptions.SettlementNotFoundException;
import com.administrapochi.models.dtos.settlements.SettlementCreateRequest;
import com.administrapochi.models.dtos.settlements.SettlementResponse;
import com.administrapochi.models.dtos.settlements.SettlementUpdateRequest;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.models.entities.Settlement;
import com.administrapochi.models.entities.SettlementRule;
import com.administrapochi.models.enums.Shift;
import com.administrapochi.models.mappers.SettlementMapper;
import com.administrapochi.repositories.PochiAssignmentRepository;
import com.administrapochi.repositories.AdministrationChargeSettlementRepository;
import com.administrapochi.repositories.SettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceImplTest {

    private static final Integer DRIVER_ID = 8;
    private static final Integer POCHI_ID = 16;
    private static final Integer ASSIGNMENT_ID = 101;
    private static final LocalDate WORK_DATE = LocalDate.of(2026, 11, 11);

    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private AdministrationChargeSettlementRepository chargeSettlementRepository;
    @Mock
    private PochiAssignmentRepository assignmentRepository;
    @Mock
    private SettlementRuleService settlementRuleService;
    @Mock
    private SettlementMapper settlementMapper;

    @InjectMocks
    private SettlementServiceImpl service;

    @Test
    void createsSettlementForMorningAndAfternoonUsingApplicableRule() {
        PochiAssignment morning = assignment(ASSIGNMENT_ID, DRIVER_ID, POCHI_ID, WORK_DATE, Shift.MORNING);
        PochiAssignment afternoon = assignment(102, DRIVER_ID, POCHI_ID, WORK_DATE, Shift.AFTERNOON);
        SettlementRule rule = rule("400.00");
        SettlementResponse response = SettlementResponse.builder()
                .id(45)
                .expectedAmount(new BigDecimal("400.00"))
                .acceptedAmount(new BigDecimal("400.00"))
                .build();
        SettlementCreateRequest request = request(List.of(ASSIGNMENT_ID, 102), "400.00", null);
        when(assignmentRepository.findAllById(request.getPochiAssignmentIds()))
                .thenReturn(List.of(morning, afternoon));
        when(settlementRepository.findSettledAssignmentIds(request.getPochiAssignmentIds())).thenReturn(Set.of());
        when(settlementRuleService.resolveApplicableRule(POCHI_ID, 2, WORK_DATE)).thenReturn(rule);
        when(settlementRepository.saveAndFlush(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(settlementMapper.toResponse(any(Settlement.class))).thenReturn(response);

        assertThat(service.create(request)).isSameAs(response);

        ArgumentCaptor<Settlement> settlementCaptor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).saveAndFlush(settlementCaptor.capture());
        Settlement persisted = settlementCaptor.getValue();
        assertThat(persisted.getDriver()).isSameAs(morning.getDriver());
        assertThat(persisted.getPochi()).isSameAs(morning.getPochi());
        assertThat(persisted.getWorkDate()).isEqualTo(WORK_DATE);
        assertThat(persisted.getExpectedAmount()).isEqualByComparingTo("400.00");
        assertThat(persisted.getAcceptedAmount()).isEqualByComparingTo("400.00");
        assertThat(persisted.getPochiAssignments()).containsExactlyInAnyOrder(morning, afternoon);
        verify(settlementRuleService).resolveApplicableRule(POCHI_ID, 2, WORK_DATE);
    }

    @Test
    void createsSingleAssignmentSettlementAndAcceptsAdministratorAdjustment() {
        PochiAssignment assignment = assignment(ASSIGNMENT_ID, DRIVER_ID, POCHI_ID, WORK_DATE, Shift.MORNING);
        SettlementCreateRequest request = request(List.of(ASSIGNMENT_ID), "300.00", "Partial amount accepted.");
        when(assignmentRepository.findAllById(request.getPochiAssignmentIds())).thenReturn(List.of(assignment));
        when(settlementRepository.findSettledAssignmentIds(request.getPochiAssignmentIds())).thenReturn(Set.of());
        when(settlementRuleService.resolveApplicableRule(POCHI_ID, 1, WORK_DATE)).thenReturn(rule("400.00"));
        when(settlementRepository.saveAndFlush(any(Settlement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(settlementMapper.toResponse(any(Settlement.class))).thenReturn(SettlementResponse.builder()
                .expectedAmount(new BigDecimal("400.00"))
                .acceptedAmount(new BigDecimal("300.00"))
                .adjustmentReason("Partial amount accepted.")
                .build());

        SettlementResponse result = service.create(request);

        assertThat(result.getAcceptedAmount()).isEqualByComparingTo("300.00");
        assertThat(result.getAdjustmentReason()).isEqualTo("Partial amount accepted.");
        verify(settlementRuleService).resolveApplicableRule(POCHI_ID, 1, WORK_DATE);
    }

    @Test
    void rejectsAlreadySettledAssignmentBeforeRuleResolution() {
        SettlementCreateRequest request = request(List.of(ASSIGNMENT_ID), "400.00", null);
        when(assignmentRepository.findAllById(request.getPochiAssignmentIds()))
                .thenReturn(List.of(assignment(ASSIGNMENT_ID, DRIVER_ID, POCHI_ID, WORK_DATE, Shift.MORNING)));
        when(settlementRepository.findSettledAssignmentIds(request.getPochiAssignmentIds()))
                .thenReturn(Set.of(ASSIGNMENT_ID));

        assertThrows(PochiAssignmentAlreadySettledException.class, () -> service.create(request));

        verify(settlementRuleService, never()).resolveApplicableRule(any(), any(), any());
        verify(settlementRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsMissingAssignment() {
        SettlementCreateRequest request = request(List.of(ASSIGNMENT_ID), "400.00", null);
        when(assignmentRepository.findAllById(request.getPochiAssignmentIds())).thenReturn(List.of());

        assertThrows(PochiAssignmentNotFoundException.class, () -> service.create(request));
        verify(settlementRepository, never()).findSettledAssignmentIds(any());
    }

    @Test
    void rejectsDuplicateAssignmentIds() {
        SettlementCreateRequest request = request(List.of(ASSIGNMENT_ID, ASSIGNMENT_ID), "400.00", null);

        assertThrows(InvalidSettlementException.class, () -> service.create(request));
        verify(assignmentRepository, never()).findAllById(any());
    }

    @Test
    void rejectsAssignmentsFromDifferentDrivers() {
        rejectsIncompatibleAssignments(
                assignment(ASSIGNMENT_ID, DRIVER_ID, POCHI_ID, WORK_DATE, Shift.MORNING),
                assignment(102, DRIVER_ID + 1, POCHI_ID, WORK_DATE, Shift.AFTERNOON));
    }

    @Test
    void rejectsAssignmentsFromDifferentPochis() {
        rejectsIncompatibleAssignments(
                assignment(ASSIGNMENT_ID, DRIVER_ID, POCHI_ID, WORK_DATE, Shift.MORNING),
                assignment(102, DRIVER_ID, POCHI_ID + 1, WORK_DATE, Shift.AFTERNOON));
    }

    @Test
    void rejectsAssignmentsFromDifferentDates() {
        rejectsIncompatibleAssignments(
                assignment(ASSIGNMENT_ID, DRIVER_ID, POCHI_ID, WORK_DATE, Shift.MORNING),
                assignment(102, DRIVER_ID, POCHI_ID, WORK_DATE.plusDays(1), Shift.AFTERNOON));
    }

    @Test
    void rejectsAdjustmentWithoutReason() {
        PochiAssignment assignment = assignment(ASSIGNMENT_ID, DRIVER_ID, POCHI_ID, WORK_DATE, Shift.MORNING);
        SettlementCreateRequest request = request(List.of(ASSIGNMENT_ID), "300.00", " ");
        when(assignmentRepository.findAllById(request.getPochiAssignmentIds())).thenReturn(List.of(assignment));
        when(settlementRepository.findSettledAssignmentIds(request.getPochiAssignmentIds())).thenReturn(Set.of());
        when(settlementRuleService.resolveApplicableRule(POCHI_ID, 1, WORK_DATE)).thenReturn(rule("400.00"));

        assertThrows(InvalidSettlementException.class, () -> service.create(request));
        verify(settlementRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsNegativeAcceptedAmount() {
        assertThrows(InvalidSettlementException.class,
                () -> service.create(request(List.of(ASSIGNMENT_ID), "-0.01", null)));
        verify(assignmentRepository, never()).findAllById(any());
    }

    @Test
    void updatePreservesHistoricalExpectedAmountWithoutResolvingRulesAgain() {
        Settlement settlement = Settlement.builder()
                .id(45)
                .expectedAmount(new BigDecimal("400.00"))
                .acceptedAmount(new BigDecimal("400.00"))
                .build();
        SettlementUpdateRequest request = new SettlementUpdateRequest();
        request.setAcceptedAmount(new BigDecimal("300.00"));
        request.setAdjustmentReason("Partial amount accepted.");
        request.setNotes("Recorded adjustment.");
        when(settlementRepository.findById(45)).thenReturn(Optional.of(settlement));
        when(settlementRepository.save(settlement)).thenReturn(settlement);
        when(settlementMapper.toResponse(settlement)).thenReturn(SettlementResponse.builder()
                .expectedAmount(new BigDecimal("400.00"))
                .acceptedAmount(new BigDecimal("300.00"))
                .build());

        SettlementResponse result = service.update(45, request);

        assertThat(result.getExpectedAmount()).isEqualByComparingTo("400.00");
        assertThat(settlement.getExpectedAmount()).isEqualByComparingTo("400.00");
        verify(settlementMapper).updateEntity(request, settlement);
        verify(settlementRuleService, never()).resolveApplicableRule(any(), any(), any());
    }

    @Test
    void updateRejectsChangedAmountWithoutReason() {
        Settlement settlement = Settlement.builder()
                .id(45)
                .expectedAmount(new BigDecimal("400.00"))
                .acceptedAmount(new BigDecimal("400.00"))
                .build();
        SettlementUpdateRequest request = new SettlementUpdateRequest();
        request.setAcceptedAmount(new BigDecimal("300.00"));
        when(settlementRepository.findById(45)).thenReturn(Optional.of(settlement));

        assertThrows(InvalidSettlementException.class, () -> service.update(45, request));

        verify(settlementMapper, never()).updateEntity(any(), any());
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void rejectsSettlementUpdateWhenLinkedToAdministrationCharge() {
        Settlement settlement = Settlement.builder().id(45).build();
        SettlementUpdateRequest request = new SettlementUpdateRequest();
        request.setAcceptedAmount(new BigDecimal("400.00"));
        when(settlementRepository.findById(45)).thenReturn(Optional.of(settlement));
        when(chargeSettlementRepository.existsBySettlementId(45)).thenReturn(true);

        assertThrows(AdministrationChargeConflictException.class, () -> service.update(45, request));

        verify(settlementMapper, never()).updateEntity(any(), any());
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void rejectsSettlementDeletionWhenLinkedToAdministrationCharge() {
        Settlement settlement = Settlement.builder().id(45).build();
        when(settlementRepository.findById(45)).thenReturn(Optional.of(settlement));
        when(chargeSettlementRepository.existsBySettlementId(45)).thenReturn(true);

        assertThrows(AdministrationChargeConflictException.class, () -> service.delete(45));

        verify(settlementRepository, never()).delete(any(Settlement.class));
    }

    @Test
    void findByIdRejectsMissingSettlement() {
        when(settlementRepository.findById(45)).thenReturn(Optional.empty());

        assertThrows(SettlementNotFoundException.class, () -> service.findById(45));
    }

    private void rejectsIncompatibleAssignments(PochiAssignment first, PochiAssignment second) {
        SettlementCreateRequest request = request(List.of(first.getId(), second.getId()), "400.00", null);
        when(assignmentRepository.findAllById(request.getPochiAssignmentIds())).thenReturn(List.of(first, second));
        when(settlementRepository.findSettledAssignmentIds(request.getPochiAssignmentIds())).thenReturn(Set.of());

        assertThrows(InvalidSettlementException.class, () -> service.create(request));
        verify(settlementRuleService, never()).resolveApplicableRule(any(), any(), any());
        verify(settlementRepository, never()).saveAndFlush(any());
    }

    private SettlementCreateRequest request(List<Integer> ids, String accepted, String reason) {
        SettlementCreateRequest request = new SettlementCreateRequest();
        request.setPochiAssignmentIds(ids);
        request.setAcceptedAmount(new BigDecimal(accepted));
        request.setAdjustmentReason(reason);
        return request;
    }

    private PochiAssignment assignment(Integer id, Integer driverId, Integer pochiId, LocalDate date, Shift shift) {
        return PochiAssignment.builder()
                .id(id)
                .driver(Driver.builder().id(driverId).firstName("Ada").firstLastName("Driver").build())
                .pochi(Pochi.builder().id(pochiId).economicNumber("P-" + pochiId).build())
                .assignmentDate(date)
                .shift(shift)
                .build();
    }

    private SettlementRule rule(String amount) {
        return SettlementRule.builder().amount(new BigDecimal(amount)).build();
    }
}
