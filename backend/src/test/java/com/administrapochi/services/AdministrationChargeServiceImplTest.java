package com.administrapochi.services;

import com.administrapochi.exceptions.AdministrationChargeConflictException;
import com.administrapochi.exceptions.AdministrationChargeNotFoundException;
import com.administrapochi.exceptions.InvalidAdministrationChargeException;
import com.administrapochi.exceptions.NoApplicableAdministrationFeeRuleException;
import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeCreateRequest;
import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeResponse;
import com.administrapochi.models.entities.AdministrationCharge;
import com.administrapochi.models.entities.AdministrationChargeSettlement;
import com.administrapochi.models.entities.AdministrationFeeRule;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.models.entities.Settlement;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.enums.Shift;
import com.administrapochi.models.mappers.AdministrationChargeMapper;
import com.administrapochi.repositories.AdministrationChargeRepository;
import com.administrapochi.repositories.AdministrationChargeSettlementRepository;
import com.administrapochi.repositories.PochiAssignmentRepository;
import com.administrapochi.repositories.PochiRepository;
import com.administrapochi.repositories.SettlementRepository;
import com.administrapochi.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministrationChargeServiceImplTest {

    private static final Integer POCHI_ID = 35;
    private static final Integer CREATOR_ID = 2;
    private static final LocalDate CHARGE_DATE = LocalDate.of(2026, 11, 13);

    @Mock
    private AdministrationChargeRepository chargeRepository;
    @Mock
    private AdministrationChargeSettlementRepository chargeSettlementRepository;
    @Mock
    private PochiRepository pochiRepository;
    @Mock
    private PochiAssignmentRepository assignmentRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private AdministrationFeeRuleService feeRuleService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AdministrationChargeMapper chargeMapper;

    @InjectMocks
    private AdministrationChargeServiceImpl service;

    private Pochi pochi;
    private User creator;

    @BeforeEach
    void setUp() {
        pochi = Pochi.builder().id(POCHI_ID).economicNumber("P-35").build();
        creator = User.builder().id(CREATOR_ID).name("Administrator").build();
        lenient().when(chargeRepository.existsByPochiIdAndChargeDate(POCHI_ID, CHARGE_DATE)).thenReturn(false);
        lenient().when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi));
        lenient().when(userRepository.findById(2)).thenReturn(Optional.of(creator));
        lenient().when(chargeRepository.saveAndFlush(any(AdministrationCharge.class))).thenAnswer(invocation -> {
            AdministrationCharge charge = invocation.getArgument(0);
            charge.setId(700);
            return charge;
        });
        lenient().when(chargeSettlementRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(chargeMapper.toResponse(any(AdministrationCharge.class), anyList()))
                .thenReturn(AdministrationChargeResponse.builder().id(700).build());
    }

    @Test
    void createsOneShiftChargeFromMorningAssignmentAndItsSettlement() {
        assertSingleShiftCharge(Shift.MORNING);
    }

    @Test
    void createsOneShiftChargeFromAfternoonAssignmentAndItsSettlement() {
        assertSingleShiftCharge(Shift.AFTERNOON);
    }

    @Test
    void createsTwoShiftChargeOnlyWhenBothAssignmentsHaveSettlements() {
        PochiAssignment morning = assignment(101, Shift.MORNING);
        PochiAssignment afternoon = assignment(102, Shift.AFTERNOON);
        Settlement morningSettlement = settlement(501, morning);
        Settlement afternoonSettlement = settlement(502, afternoon);
        when(assignmentRepository.findForAdministrationCharge(POCHI_ID, CHARGE_DATE))
                .thenReturn(List.of(morning, afternoon));
        when(settlementRepository.findByPochiAssignmentIds(List.of(101, 102)))
                .thenReturn(List.of(morningSettlement, afternoonSettlement));
        when(chargeSettlementRepository.findLinkedSettlementIds(List.of(501, 502))).thenReturn(Set.of());
        when(feeRuleService.resolveApplicableRuleEntity(POCHI_ID, CHARGE_DATE))
                .thenReturn(rule(null, "70.00", "100.00"));

        service.create(request(null, null));

        ArgumentCaptor<AdministrationCharge> chargeCaptor = ArgumentCaptor.forClass(AdministrationCharge.class);
        verify(chargeRepository).saveAndFlush(chargeCaptor.capture());
        assertThat(chargeCaptor.getValue().getShiftCount()).isEqualTo((short) 2);
        assertThat(chargeCaptor.getValue().getSuggestedAmount()).isEqualByComparingTo("100.00");
        ArgumentCaptor<List<AdministrationChargeSettlement>> linksCaptor = ArgumentCaptor.forClass(List.class);
        verify(chargeSettlementRepository).saveAllAndFlush(linksCaptor.capture());
        assertThat(linksCaptor.getValue()).extracting(link -> link.getSettlement().getId())
                .containsExactlyInAnyOrder(501, 502);
    }

    @Test
    void usesRuleResolvedForPochiAndChargeDateAndSetsAuthenticatedCreator() {
        PochiAssignment assignment = assignment(101, Shift.MORNING);
        when(assignmentRepository.findForAdministrationCharge(POCHI_ID, CHARGE_DATE))
                .thenReturn(List.of(assignment));
        when(settlementRepository.findByPochiAssignmentIds(List.of(101)))
                .thenReturn(List.of(settlement(501, assignment)));
        when(chargeSettlementRepository.findLinkedSettlementIds(List.of(501))).thenReturn(Set.of());
        AdministrationFeeRule specific = rule(POCHI_ID, "55.00", "75.00");
        when(feeRuleService.resolveApplicableRuleEntity(POCHI_ID, CHARGE_DATE)).thenReturn(specific);

        service.create(request(null, null));

        verify(feeRuleService).resolveApplicableRuleEntity(POCHI_ID, CHARGE_DATE);
        verify(userRepository).findById(CREATOR_ID);
        ArgumentCaptor<AdministrationCharge> chargeCaptor = ArgumentCaptor.forClass(AdministrationCharge.class);
        verify(chargeRepository).saveAndFlush(chargeCaptor.capture());
        assertThat(chargeCaptor.getValue().getAdministrationFeeRule()).isSameAs(specific);
        assertThat(chargeCaptor.getValue().getCreatedBy()).isSameAs(creator);
    }

    @Test
    void rejectsCreationWhenTemporaryUserDoesNotExist() {
        setupSingleSettlement(Shift.MORNING, "70.00");
        when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.empty());

        assertThrows(com.administrapochi.exceptions.UserNotFoundException.class,
                () -> service.create(request(null, null)));

        verify(chargeRepository, never()).saveAndFlush(any());
    }

    @Test
    void fallsBackToGlobalRuleWhenResolvedRuleHasNoPochiScope() {
        PochiAssignment assignment = assignment(101, Shift.MORNING);
        when(assignmentRepository.findForAdministrationCharge(POCHI_ID, CHARGE_DATE))
                .thenReturn(List.of(assignment));
        when(settlementRepository.findByPochiAssignmentIds(List.of(101)))
                .thenReturn(List.of(settlement(501, assignment)));
        when(chargeSettlementRepository.findLinkedSettlementIds(List.of(501))).thenReturn(Set.of());
        AdministrationFeeRule global = rule(null, "40.00", "80.00");
        when(feeRuleService.resolveApplicableRuleEntity(POCHI_ID, CHARGE_DATE)).thenReturn(global);

        service.create(request(null, null));

        ArgumentCaptor<AdministrationCharge> chargeCaptor = ArgumentCaptor.forClass(AdministrationCharge.class);
        verify(chargeRepository).saveAndFlush(chargeCaptor.capture());
        assertThat(chargeCaptor.getValue().getAdministrationFeeRule().getPochi()).isNull();
    }

    @Test
    void usesSuggestedAmountWhenRequestOmitsAmountAndTrimsAnOptionalNote() {
        setupSingleSettlement(Shift.MORNING, "70.00");
        service.create(request(null, "  noted  "));

        ArgumentCaptor<AdministrationCharge> chargeCaptor = ArgumentCaptor.forClass(AdministrationCharge.class);
        verify(chargeRepository).saveAndFlush(chargeCaptor.capture());
        assertThat(chargeCaptor.getValue().getAmount()).isEqualByComparingTo("70.00");
        assertThat(chargeCaptor.getValue().getSuggestedAmount()).isEqualByComparingTo("70.00");
        assertThat(chargeCaptor.getValue().getNote()).isEqualTo("noted");
    }

    @Test
    void acceptsAdjustedAmountWithNonblankNote() {
        setupSingleSettlement(Shift.MORNING, "70.00");
        service.create(request("60.00", "  Partial shift.  "));

        ArgumentCaptor<AdministrationCharge> chargeCaptor = ArgumentCaptor.forClass(AdministrationCharge.class);
        verify(chargeRepository).saveAndFlush(chargeCaptor.capture());
        assertThat(chargeCaptor.getValue().getAmount()).isEqualByComparingTo("60.00");
        assertThat(chargeCaptor.getValue().getNote()).isEqualTo("Partial shift.");
    }

    @Test
    void comparesAmountsNumericallyRatherThanByScale() {
        setupSingleSettlement(Shift.MORNING, "70.00");
        service.create(request("70.0", null));

        ArgumentCaptor<AdministrationCharge> chargeCaptor = ArgumentCaptor.forClass(AdministrationCharge.class);
        verify(chargeRepository).saveAndFlush(chargeCaptor.capture());
        assertThat(chargeCaptor.getValue().getAmount()).isEqualByComparingTo("70.0");
        assertThat(chargeCaptor.getValue().getNote()).isNull();
    }

    @Test
    void rejectsDuplicateDailyChargeBeforeLoadingReferences() {
        when(chargeRepository.existsByPochiIdAndChargeDate(POCHI_ID, CHARGE_DATE)).thenReturn(true);

        assertThrows(AdministrationChargeConflictException.class, () -> service.create(request(null, null)));

        verify(pochiRepository, never()).findById(any());
        verify(chargeRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsWhenNoAssignmentExists() {
        when(assignmentRepository.findForAdministrationCharge(POCHI_ID, CHARGE_DATE)).thenReturn(List.of());

        assertThrows(InvalidAdministrationChargeException.class, () -> service.create(request(null, null)));

        verify(settlementRepository, never()).findByPochiAssignmentIds(anyCollection());
        verify(chargeRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsTwoAssignmentsWhenOneHasNoSettlement() {
        PochiAssignment morning = assignment(101, Shift.MORNING);
        PochiAssignment afternoon = assignment(102, Shift.AFTERNOON);
        when(assignmentRepository.findForAdministrationCharge(POCHI_ID, CHARGE_DATE))
                .thenReturn(List.of(morning, afternoon));
        when(settlementRepository.findByPochiAssignmentIds(List.of(101, 102)))
                .thenReturn(List.of(settlement(501, morning)));

        assertThrows(InvalidAdministrationChargeException.class, () -> service.create(request(null, null)));

        verify(feeRuleService, never()).resolveApplicableRuleEntity(any(), any());
        verify(chargeRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateShiftsAndMoreThanTwoAssignments() {
        PochiAssignment morning = assignment(101, Shift.MORNING);
        when(assignmentRepository.findForAdministrationCharge(POCHI_ID, CHARGE_DATE))
                .thenReturn(List.of(morning, assignment(102, Shift.MORNING)));
        assertThrows(InvalidAdministrationChargeException.class, () -> service.create(request(null, null)));

        when(assignmentRepository.findForAdministrationCharge(POCHI_ID, CHARGE_DATE))
                .thenReturn(List.of(morning, assignment(102, Shift.AFTERNOON), assignment(103, Shift.MORNING)));
        assertThrows(InvalidAdministrationChargeException.class, () -> service.create(request(null, null)));
        verify(settlementRepository, never()).findByPochiAssignmentIds(anyCollection());
    }

    @Test
    void rejectsWhenNoApplicableRuleExists() {
        setupSingleSettlement(Shift.MORNING, "70.00");
        when(feeRuleService.resolveApplicableRuleEntity(POCHI_ID, CHARGE_DATE))
                .thenThrow(new NoApplicableAdministrationFeeRuleException(POCHI_ID, CHARGE_DATE));

        assertThrows(NoApplicableAdministrationFeeRuleException.class,
                () -> service.create(request(null, null)));
        verify(chargeRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAdjustedAmountWithoutNoteOrWithBlankNote() {
        setupSingleSettlement(Shift.MORNING, "70.00");
        assertThrows(InvalidAdministrationChargeException.class,
                () -> service.create(request("60.00", null)));
        assertThrows(InvalidAdministrationChargeException.class,
                () -> service.create(request("60.00", "  ")));
        verify(chargeRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsZeroNegativeAndOverPrecisionAmounts() {
        assertThrows(InvalidAdministrationChargeException.class,
                () -> service.create(request("0.00", null)));
        assertThrows(InvalidAdministrationChargeException.class,
                () -> service.create(request("-1.00", null)));
        assertThrows(InvalidAdministrationChargeException.class,
                () -> service.create(request("1.001", null)));
        verify(assignmentRepository, never()).findForAdministrationCharge(any(), any());
    }

    @Test
    void rejectsSettlementAlreadyLinkedToAnotherCharge() {
        PochiAssignment assignment = assignment(101, Shift.MORNING);
        when(assignmentRepository.findForAdministrationCharge(POCHI_ID, CHARGE_DATE))
                .thenReturn(List.of(assignment));
        when(settlementRepository.findByPochiAssignmentIds(List.of(101)))
                .thenReturn(List.of(settlement(501, assignment)));
        when(chargeSettlementRepository.findLinkedSettlementIds(List.of(501))).thenReturn(Set.of(501));

        assertThrows(AdministrationChargeConflictException.class, () -> service.create(request(null, null)));

        verify(feeRuleService, never()).resolveApplicableRuleEntity(any(), any());
        verify(chargeRepository, never()).saveAndFlush(any());
    }

    @Test
    void translatesAssociationPersistenceFailureAndRunsCreateTransactionally() throws NoSuchMethodException {
        setupSingleSettlement(Shift.MORNING, "70.00");
        when(chargeSettlementRepository.saveAllAndFlush(anyList()))
                .thenThrow(new DataIntegrityViolationException("database details"));

        assertThrows(AdministrationChargeConflictException.class, () -> service.create(request(null, null)));
        assertThat(AdministrationChargeServiceImpl.class
                .getMethod("create", AdministrationChargeCreateRequest.class)
                .getAnnotation(Transactional.class)).isNotNull();
    }

    @Test
    void findsChargeByIdAndListsPagedResults() {
        AdministrationCharge charge = AdministrationCharge.builder()
                .id(700)
                .chargeSettlements(new ArrayList<>())
                .build();
        AdministrationChargeResponse response = AdministrationChargeResponse.builder().id(700).build();
        when(chargeRepository.findWithDetailsById(700)).thenReturn(Optional.of(charge));
        when(chargeSettlementRepository.findAllByAdministrationChargeIdIn(List.of(700))).thenReturn(List.of());
        when(chargeMapper.toResponse(charge, List.of())).thenReturn(response);
        assertThat(service.findById(700)).isSameAs(response);

        PageRequest pageable = PageRequest.of(0, 20);
        when(chargeRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(charge)));
        when(chargeMapper.toResponse(charge, List.of())).thenReturn(response);
        Page<AdministrationChargeResponse> page =
                service.findAll(POCHI_ID, null, CHARGE_DATE.minusDays(1), CHARGE_DATE, CREATOR_ID, true, pageable);
        assertThat(page.getContent()).containsExactly(response);
        verify(chargeSettlementRepository, org.mockito.Mockito.times(2))
                .findAllByAdministrationChargeIdIn(List.of(700));
    }

    @Test
    void missingChargeAndContradictoryDateFiltersAreRejected() {
        when(chargeRepository.findWithDetailsById(701)).thenReturn(Optional.empty());
        assertThrows(AdministrationChargeNotFoundException.class, () -> service.findById(701));
        assertThrows(InvalidAdministrationChargeException.class, () -> service.findAll(
                null, CHARGE_DATE, CHARGE_DATE.minusDays(1), null, null, null, PageRequest.of(0, 20)));
    }

    @Test
    void hasNoUpdateOrDeleteServiceOperations() {
        assertThat(List.of(AdministrationChargeService.class.getMethods()))
                .extracting(Method::getName)
                .doesNotContain("update", "delete");
    }

    private void assertSingleShiftCharge(Shift shift) {
        setupSingleSettlement(shift, "55.00");
        service.create(request(null, null));

        ArgumentCaptor<AdministrationCharge> chargeCaptor = ArgumentCaptor.forClass(AdministrationCharge.class);
        verify(chargeRepository).saveAndFlush(chargeCaptor.capture());
        assertThat(chargeCaptor.getValue().getShiftCount()).isEqualTo((short) 1);
        assertThat(chargeCaptor.getValue().getAmount()).isEqualByComparingTo("55.00");
        ArgumentCaptor<List<AdministrationChargeSettlement>> linksCaptor = ArgumentCaptor.forClass(List.class);
        verify(chargeSettlementRepository).saveAllAndFlush(linksCaptor.capture());
        assertThat(linksCaptor.getValue()).hasSize(1);
        assertThat(linksCaptor.getValue().getFirst().getSettlement().getId()).isEqualTo(501);
    }

    private void setupSingleSettlement(Shift shift, String oneShiftAmount) {
        PochiAssignment assignment = assignment(101, shift);
        when(assignmentRepository.findForAdministrationCharge(POCHI_ID, CHARGE_DATE))
                .thenReturn(List.of(assignment));
        when(settlementRepository.findByPochiAssignmentIds(List.of(101)))
                .thenReturn(List.of(settlement(501, assignment)));
        when(chargeSettlementRepository.findLinkedSettlementIds(List.of(501))).thenReturn(Set.of());
        when(feeRuleService.resolveApplicableRuleEntity(POCHI_ID, CHARGE_DATE))
                .thenReturn(rule(POCHI_ID, oneShiftAmount, "100.00"));
    }

    private AdministrationChargeCreateRequest request(String amount, String note) {
        AdministrationChargeCreateRequest request = new AdministrationChargeCreateRequest();
        request.setPochiId(POCHI_ID);
        request.setChargeDate(CHARGE_DATE);
        request.setAmount(amount == null ? null : new BigDecimal(amount));
        request.setNote(note);
        return request;
    }

    private PochiAssignment assignment(Integer id, Shift shift) {
        return PochiAssignment.builder()
                .id(id)
                .pochi(pochi)
                .assignmentDate(CHARGE_DATE)
                .shift(shift)
                .build();
    }

    private Settlement settlement(Integer id, PochiAssignment... assignments) {
        return Settlement.builder()
                .id(id)
                .pochiAssignments(new HashSet<>(List.of(assignments)))
                .expectedAmount(new BigDecimal("200.00"))
                .acceptedAmount(new BigDecimal("200.00"))
                .build();
    }

    private AdministrationFeeRule rule(Integer pochiId, String oneShiftAmount, String twoShiftsAmount) {
        return AdministrationFeeRule.builder()
                .id(pochiId == null ? 22 : 21)
                .pochi(pochiId == null ? null : pochi)
                .oneShiftAmount(new BigDecimal(oneShiftAmount))
                .twoShiftsAmount(new BigDecimal(twoShiftsAmount))
                .build();
    }
}
