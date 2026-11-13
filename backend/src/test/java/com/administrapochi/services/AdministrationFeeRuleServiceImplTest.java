package com.administrapochi.services;

import com.administrapochi.exceptions.AdministrationFeeRuleNotFoundException;
import com.administrapochi.exceptions.AdministrationFeeRuleOverlapException;
import com.administrapochi.exceptions.InvalidAdministrationFeeRuleException;
import com.administrapochi.exceptions.NoApplicableAdministrationFeeRuleException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleCreateRequest;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleResponse;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleUpdateRequest;
import com.administrapochi.models.entities.AdministrationFeeRule;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.mappers.AdministrationFeeRuleMapper;
import com.administrapochi.repositories.AdministrationFeeRuleRepository;
import com.administrapochi.repositories.PochiRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministrationFeeRuleServiceImplTest {

    private static final Integer POCHI_ID = 82;
    private static final Integer RULE_ID = 12;
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate VALID_TO = LocalDate.of(2026, 12, 31);
    private static final LocalDate DATE = LocalDate.of(2026, 6, 1);

    @Mock
    private AdministrationFeeRuleRepository ruleRepository;
    @Mock
    private PochiRepository pochiRepository;
    @Mock
    private AdministrationFeeRuleMapper ruleMapper;

    @InjectMocks
    private AdministrationFeeRuleServiceImpl service;

    @Test
    void createsGlobalRuleAndDefaultsActiveToTrue() {
        AdministrationFeeRuleCreateRequest request = createRequest(null, "50.00", "70.00", VALID_FROM, null);
        AdministrationFeeRule rule = rule(null, "50.00", "70.00", VALID_FROM, null, true);
        when(ruleRepository.existsOverlappingGlobalRule(VALID_FROM, null, null)).thenReturn(false);
        when(ruleMapper.toEntity(request)).thenReturn(rule);
        when(ruleRepository.save(rule)).thenReturn(rule);
        when(ruleMapper.toResponse(rule)).thenReturn(response(RULE_ID, null, "50.00", "70.00", true));

        AdministrationFeeRuleResponse result = service.create(request);

        assertThat(result.getGlobal()).isTrue();
        assertThat(rule.getPochi()).isNull();
        assertThat(rule.getActive()).isTrue();
        verify(pochiRepository, never()).findById(any());
        verify(ruleRepository).existsOverlappingGlobalRule(VALID_FROM, null, null);
    }

    @Test
    void createsPochiSpecificRuleUsingResolvedPochiAndAllowsGlobalOverlap() {
        AdministrationFeeRuleCreateRequest request =
                createRequest(POCHI_ID, "60.00", "80.00", VALID_FROM, VALID_TO);
        Pochi pochi = pochi(POCHI_ID);
        AdministrationFeeRule rule = rule(POCHI_ID, "60.00", "80.00", VALID_FROM, VALID_TO, true);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi));
        when(ruleRepository.existsOverlappingPochiRule(POCHI_ID, VALID_FROM, VALID_TO, null))
                .thenReturn(false);
        when(ruleMapper.toEntity(request)).thenReturn(rule);
        when(ruleRepository.save(rule)).thenReturn(rule);
        when(ruleMapper.toResponse(rule)).thenReturn(response(RULE_ID, POCHI_ID, "60.00", "80.00", true));

        AdministrationFeeRuleResponse result = service.create(request);

        assertThat(result.getPochiId()).isEqualTo(POCHI_ID);
        assertThat(rule.getPochi()).isSameAs(pochi);
        verify(ruleRepository, never()).existsOverlappingGlobalRule(any(), any(), any());
    }

    @Test
    void rejectsUnknownPochiOnCreate() {
        AdministrationFeeRuleCreateRequest request =
                createRequest(POCHI_ID, "60.00", "80.00", VALID_FROM, null);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.empty());

        assertThrows(PochiNotFoundException.class, () -> service.create(request));

        verify(ruleRepository, never()).save(any());
        verify(ruleRepository, never()).existsOverlappingPochiRule(any(), any(), any(), any());
    }

    @Test
    void rejectsOverlappingGlobalRule() {
        AdministrationFeeRuleCreateRequest request =
                createRequest(null, "50.00", "70.00", DATE, null);
        when(ruleRepository.existsOverlappingGlobalRule(DATE, null, null)).thenReturn(true);

        AdministrationFeeRuleOverlapException exception = assertThrows(
                AdministrationFeeRuleOverlapException.class, () -> service.create(request));

        assertThat(exception).hasMessage(
                "An administration fee global rule already exists for the requested validity period.");
        verify(ruleMapper, never()).toEntity(request);
    }

    @Test
    void rejectsOverlappingRuleForSamePochi() {
        AdministrationFeeRuleCreateRequest request =
                createRequest(POCHI_ID, "60.00", "80.00", DATE, null);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi(POCHI_ID)));
        when(ruleRepository.existsOverlappingPochiRule(POCHI_ID, DATE, null, null)).thenReturn(true);

        AdministrationFeeRuleOverlapException exception = assertThrows(
                AdministrationFeeRuleOverlapException.class, () -> service.create(request));

        assertThat(exception).hasMessage(
                "An administration fee rule already exists for Pochi 82 during the requested validity period.");
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void allowsOverlappingRulesAcrossDifferentPochisAndGlobalScope() {
        Integer otherPochiId = POCHI_ID + 1;
        AdministrationFeeRuleCreateRequest request =
                createRequest(otherPochiId, "60.00", "80.00", VALID_FROM, null);
        when(pochiRepository.findById(otherPochiId)).thenReturn(Optional.of(pochi(otherPochiId)));
        when(ruleRepository.existsOverlappingPochiRule(otherPochiId, VALID_FROM, null, null))
                .thenReturn(false);
        when(ruleMapper.toEntity(request))
                .thenReturn(rule(otherPochiId, "60.00", "80.00", VALID_FROM, null, true));
        when(ruleRepository.save(any(AdministrationFeeRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ruleMapper.toResponse(any(AdministrationFeeRule.class)))
                .thenReturn(response(RULE_ID, otherPochiId, "60.00", "80.00", true));

        assertThat(service.create(request).getPochiId()).isEqualTo(otherPochiId);
        verify(ruleRepository).existsOverlappingPochiRule(otherPochiId, VALID_FROM, null, null);
        verify(ruleRepository, never()).existsOverlappingGlobalRule(any(), any(), any());
    }

    @Test
    void rejectsNegativeAmountsAndTwoShiftAmountBelowOneShiftAmount() {
        assertInvalidCreate(createRequest(null, "-0.01", "70.00", VALID_FROM, null));
        assertInvalidCreate(createRequest(null, "50.00", "-0.01", VALID_FROM, null));
        assertInvalidCreate(createRequest(null, "50.00", "49.99", VALID_FROM, null));
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void acceptsZeroAmountsWhenTwoShiftAmountIsNotLower() {
        AdministrationFeeRuleCreateRequest request =
                createRequest(null, "0.00", "0.00", VALID_FROM, null);
        when(ruleRepository.existsOverlappingGlobalRule(VALID_FROM, null, null)).thenReturn(false);
        when(ruleMapper.toEntity(request)).thenReturn(rule(null, "0.00", "0.00", VALID_FROM, null, true));
        when(ruleRepository.save(any(AdministrationFeeRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ruleMapper.toResponse(any(AdministrationFeeRule.class)))
                .thenReturn(response(RULE_ID, null, "0.00", "0.00", true));

        assertThat(service.create(request).getOneShiftAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsInvalidDateRangeOnCreate() {
        AdministrationFeeRuleCreateRequest request =
                createRequest(null, "50.00", "70.00", VALID_TO, VALID_FROM);

        assertThrows(InvalidAdministrationFeeRuleException.class, () -> service.create(request));

        verify(ruleRepository, never()).existsOverlappingGlobalRule(any(), any(), any());
    }

    @Test
    void findsRuleByIdAndThrowsWhenMissing() {
        AdministrationFeeRule rule = rule(null, "50.00", "70.00", VALID_FROM, null, true);
        AdministrationFeeRuleResponse response = response(RULE_ID, null, "50.00", "70.00", true);
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(ruleMapper.toResponse(rule)).thenReturn(response);

        assertThat(service.findById(RULE_ID)).isSameAs(response);

        when(ruleRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(AdministrationFeeRuleNotFoundException.class, () -> service.findById(99));
    }

    @Test
    void listsRulesWithOptionalFiltersAndSortsByValidity() {
        List<AdministrationFeeRule> rules = List.of(rule(null, "50.00", "70.00", VALID_FROM, null, true));
        List<AdministrationFeeRuleResponse> responses = List.of(response(1, null, "50.00", "70.00", true));
        when(ruleRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(rules);
        when(ruleMapper.toResponseList(rules)).thenReturn(responses);

        assertThat(service.findAll(POCHI_ID, false, true, DATE)).isSameAs(responses);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(ruleRepository).findAll(any(Specification.class), sortCaptor.capture());
        assertThat(sortCaptor.getValue().getOrderFor("validFrom").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sortCaptor.getValue().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void rejectsConflictingGlobalAndPochiFilters() {
        assertThrows(InvalidAdministrationFeeRuleException.class,
                () -> service.findAll(POCHI_ID, true, null, null));
        verify(ruleRepository, never()).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void resolvesSpecificRuleBeforeGlobalRule() {
        AdministrationFeeRule specific = rule(POCHI_ID, "60.00", "80.00", VALID_FROM, null, true);
        AdministrationFeeRuleResponse response = response(RULE_ID, POCHI_ID, "60.00", "80.00", true);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi(POCHI_ID)));
        when(ruleRepository.findApplicableForPochi(POCHI_ID, DATE)).thenReturn(List.of(specific));
        when(ruleMapper.toResponse(specific)).thenReturn(response);

        assertThat(service.findApplicableRule(POCHI_ID, DATE)).isSameAs(response);

        verify(ruleRepository, never()).findApplicableGlobalRule(DATE);
    }

    @Test
    void fallsBackToGlobalRuleWhenNoActiveSpecificRuleApplies() {
        AdministrationFeeRule global = rule(null, "50.00", "70.00", VALID_FROM, null, true);
        AdministrationFeeRuleResponse response = response(RULE_ID, null, "50.00", "70.00", true);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi(POCHI_ID)));
        when(ruleRepository.findApplicableForPochi(POCHI_ID, DATE)).thenReturn(List.of());
        when(ruleRepository.findApplicableGlobalRule(DATE)).thenReturn(List.of(global));
        when(ruleMapper.toResponse(global)).thenReturn(response);

        assertThat(service.findApplicableRule(POCHI_ID, DATE)).isSameAs(response);
    }

    @Test
    void returnsSpecificRuleOnInclusiveValidityBoundariesOnly() {
        AdministrationFeeRule specific = rule(POCHI_ID, "60.00", "80.00", VALID_FROM, VALID_TO, true);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi(POCHI_ID)));
        when(ruleRepository.findApplicableForPochi(POCHI_ID, VALID_FROM)).thenReturn(List.of(specific));
        when(ruleRepository.findApplicableForPochi(POCHI_ID, VALID_TO)).thenReturn(List.of(specific));
        when(ruleRepository.findApplicableForPochi(POCHI_ID, VALID_FROM.minusDays(1))).thenReturn(List.of());
        when(ruleRepository.findApplicableForPochi(POCHI_ID, VALID_TO.plusDays(1))).thenReturn(List.of());
        when(ruleRepository.findApplicableGlobalRule(VALID_FROM.minusDays(1))).thenReturn(List.of());
        when(ruleRepository.findApplicableGlobalRule(VALID_TO.plusDays(1))).thenReturn(List.of());
        when(ruleMapper.toResponse(specific)).thenReturn(response(RULE_ID, POCHI_ID, "60.00", "80.00", true));

        assertThat(service.findApplicableRule(POCHI_ID, VALID_FROM).getPochiId()).isEqualTo(POCHI_ID);
        assertThat(service.findApplicableRule(POCHI_ID, VALID_TO).getPochiId()).isEqualTo(POCHI_ID);
        assertThrows(NoApplicableAdministrationFeeRuleException.class,
                () -> service.findApplicableRule(POCHI_ID, VALID_FROM.minusDays(1)));
        assertThrows(NoApplicableAdministrationFeeRuleException.class,
                () -> service.findApplicableRule(POCHI_ID, VALID_TO.plusDays(1)));
    }

    @Test
    void throwsWhenNoRuleAppliesOrPochiDoesNotExist() {
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi(POCHI_ID)));
        when(ruleRepository.findApplicableForPochi(POCHI_ID, DATE)).thenReturn(List.of());
        when(ruleRepository.findApplicableGlobalRule(DATE)).thenReturn(List.of());

        NoApplicableAdministrationFeeRuleException exception = assertThrows(
                NoApplicableAdministrationFeeRuleException.class,
                () -> service.findApplicableRule(POCHI_ID, DATE));
        assertThat(exception).hasMessage("No administration fee rule applies to Pochi 82 on 2026-06-01.");

        when(pochiRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(PochiNotFoundException.class, () -> service.findApplicableRule(99, DATE));
        verify(ruleRepository, never()).findApplicableForPochi(99, DATE);
    }

    @Test
    void rejectsMultipleApplicableRulesInsteadOfChoosingOne() {
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi(POCHI_ID)));
        when(ruleRepository.findApplicableForPochi(POCHI_ID, DATE))
                .thenReturn(List.of(
                        rule(POCHI_ID, "60.00", "80.00", VALID_FROM, null, true),
                        rule(POCHI_ID, "65.00", "85.00", DATE, null, true)));

        assertThrows(AdministrationFeeRuleOverlapException.class,
                () -> service.findApplicableRule(POCHI_ID, DATE));

        verify(ruleRepository, never()).findApplicableGlobalRule(DATE);
    }

    @Test
    void updatesRuleWithoutChangingItsPochiScope() {
        Pochi pochi = pochi(POCHI_ID);
        AdministrationFeeRule rule = AdministrationFeeRule.builder()
                .id(RULE_ID)
                .pochi(pochi)
                .oneShiftAmount(new BigDecimal("60.00"))
                .twoShiftsAmount(new BigDecimal("80.00"))
                .validFrom(VALID_FROM)
                .active(true)
                .build();
        AdministrationFeeRuleUpdateRequest request = updateRequest("65.00", "85.00", VALID_FROM, VALID_TO, true);
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(ruleRepository.existsOverlappingPochiRule(POCHI_ID, VALID_FROM, VALID_TO, RULE_ID))
                .thenReturn(false);
        doAnswer(invocation -> {
            AdministrationFeeRuleUpdateRequest update = invocation.getArgument(0);
            AdministrationFeeRule target = invocation.getArgument(1);
            target.setOneShiftAmount(update.getOneShiftAmount());
            target.setTwoShiftsAmount(update.getTwoShiftsAmount());
            target.setValidFrom(update.getValidFrom());
            target.setValidTo(update.getValidTo());
            target.setActive(update.getActive());
            return null;
        }).when(ruleMapper).updateEntity(request, rule);
        when(ruleRepository.save(rule)).thenReturn(rule);
        when(ruleMapper.toResponse(rule)).thenReturn(response(RULE_ID, POCHI_ID, "65.00", "85.00", true));

        AdministrationFeeRuleResponse result = service.update(RULE_ID, request);

        assertThat(rule.getPochi()).isSameAs(pochi);
        assertThat(result.getOneShiftAmount()).isEqualByComparingTo("65.00");
        verify(pochiRepository, never()).findById(any());
        verify(ruleRepository).existsOverlappingPochiRule(POCHI_ID, VALID_FROM, VALID_TO, RULE_ID);
    }

    @Test
    void updatingGlobalRuleCannotConvertItToPochiSpecific() {
        AdministrationFeeRule rule = rule(null, "50.00", "70.00", VALID_FROM, null, true);
        rule.setId(RULE_ID);
        AdministrationFeeRuleUpdateRequest request =
                updateRequest("55.00", "75.00", VALID_FROM, VALID_TO, true);
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(ruleRepository.existsOverlappingGlobalRule(VALID_FROM, VALID_TO, RULE_ID))
                .thenReturn(false);
        doAnswer(invocation -> {
            AdministrationFeeRuleUpdateRequest update = invocation.getArgument(0);
            AdministrationFeeRule target = invocation.getArgument(1);
            target.setOneShiftAmount(update.getOneShiftAmount());
            target.setTwoShiftsAmount(update.getTwoShiftsAmount());
            target.setValidFrom(update.getValidFrom());
            target.setValidTo(update.getValidTo());
            target.setActive(update.getActive());
            return null;
        }).when(ruleMapper).updateEntity(request, rule);
        when(ruleRepository.save(rule)).thenReturn(rule);
        when(ruleMapper.toResponse(rule)).thenReturn(response(RULE_ID, null, "55.00", "75.00", true));

        assertThat(service.update(RULE_ID, request).getGlobal()).isTrue();
        assertThat(rule.getPochi()).isNull();
        verify(pochiRepository, never()).findById(any());
        verify(ruleRepository).existsOverlappingGlobalRule(VALID_FROM, VALID_TO, RULE_ID);
    }

    @Test
    void rejectsUpdateThatWouldOverlapAnotherRule() {
        AdministrationFeeRule rule = rule(null, "50.00", "70.00", VALID_FROM, null, true);
        rule.setId(RULE_ID);
        AdministrationFeeRuleUpdateRequest request =
                updateRequest("55.00", "75.00", VALID_FROM, null, true);
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(ruleRepository.existsOverlappingGlobalRule(VALID_FROM, null, RULE_ID)).thenReturn(true);

        assertThrows(AdministrationFeeRuleOverlapException.class,
                () -> service.update(RULE_ID, request));

        verify(ruleMapper, never()).updateEntity(request, rule);
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidUpdateValuesAndDateRange() {
        AdministrationFeeRule rule = rule(null, "50.00", "70.00", VALID_FROM, null, true);
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));

        assertThrows(InvalidAdministrationFeeRuleException.class,
                () -> service.update(RULE_ID,
                        updateRequest("-1.00", "70.00", VALID_FROM, null, true)));
        assertThrows(InvalidAdministrationFeeRuleException.class,
                () -> service.update(RULE_ID,
                        updateRequest("50.00", "49.00", VALID_FROM, null, true)));
        assertThrows(InvalidAdministrationFeeRuleException.class,
                () -> service.update(RULE_ID,
                        updateRequest("50.00", "70.00", VALID_TO, VALID_FROM, true)));

        verify(ruleRepository, never()).save(any());
    }

    @Test
    void throwsWhenUpdatingOrDeactivatingMissingRule() {
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.empty());

        assertThrows(AdministrationFeeRuleNotFoundException.class,
                () -> service.update(RULE_ID, updateRequest("50.00", "70.00", VALID_FROM, null, true)));
        assertThrows(AdministrationFeeRuleNotFoundException.class, () -> service.deactivate(RULE_ID));
    }

    @Test
    void deactivatesActiveRuleIdempotentlyAndUsesGlobalRuleAfterSpecificDeactivation() {
        AdministrationFeeRule specific = rule(POCHI_ID, "60.00", "80.00", VALID_FROM, null, true);
        specific.setId(RULE_ID);
        AdministrationFeeRule global = rule(null, "50.00", "70.00", VALID_FROM, null, true);
        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(specific));
        when(ruleRepository.save(specific)).thenReturn(specific);
        when(ruleMapper.toResponse(specific)).thenReturn(response(RULE_ID, POCHI_ID, "60.00", "80.00", false));

        assertThat(service.deactivate(RULE_ID).getActive()).isFalse();
        assertThat(specific.getActive()).isFalse();

        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi(POCHI_ID)));
        when(ruleRepository.findApplicableForPochi(POCHI_ID, DATE)).thenReturn(List.of());
        when(ruleRepository.findApplicableGlobalRule(DATE)).thenReturn(List.of(global));
        when(ruleMapper.toResponse(global)).thenReturn(response(13, null, "50.00", "70.00", true));

        assertThat(service.findApplicableRule(POCHI_ID, DATE).getGlobal()).isTrue();

        when(ruleRepository.findById(RULE_ID)).thenReturn(Optional.of(specific));
        service.deactivate(RULE_ID);
        verify(ruleRepository).save(specific);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsInvalidPochiIdentifiers(int id) {
        assertThrows(InvalidAdministrationFeeRuleException.class,
                () -> service.create(createRequest(id, "50.00", "70.00", VALID_FROM, null)));
        assertThrows(InvalidAdministrationFeeRuleException.class,
                () -> service.findApplicableRule(id, DATE));
        verify(pochiRepository, never()).findById(id);
    }

    private void assertInvalidCreate(AdministrationFeeRuleCreateRequest request) {
        assertThrows(InvalidAdministrationFeeRuleException.class, () -> service.create(request));
    }

    private AdministrationFeeRuleCreateRequest createRequest(
            Integer pochiId, String oneAmount, String twoAmount, LocalDate from, LocalDate to) {
        AdministrationFeeRuleCreateRequest request = new AdministrationFeeRuleCreateRequest();
        request.setPochiId(pochiId);
        request.setOneShiftAmount(new BigDecimal(oneAmount));
        request.setTwoShiftsAmount(new BigDecimal(twoAmount));
        request.setValidFrom(from);
        request.setValidTo(to);
        return request;
    }

    private AdministrationFeeRuleUpdateRequest updateRequest(
            String oneAmount, String twoAmount, LocalDate from, LocalDate to, boolean active) {
        AdministrationFeeRuleUpdateRequest request = new AdministrationFeeRuleUpdateRequest();
        request.setOneShiftAmount(new BigDecimal(oneAmount));
        request.setTwoShiftsAmount(new BigDecimal(twoAmount));
        request.setValidFrom(from);
        request.setValidTo(to);
        request.setActive(active);
        return request;
    }

    private AdministrationFeeRule rule(
            Integer pochiId, String oneAmount, String twoAmount,
            LocalDate from, LocalDate to, boolean active) {
        return AdministrationFeeRule.builder()
                .pochi(pochiId == null ? null : pochi(pochiId))
                .oneShiftAmount(new BigDecimal(oneAmount))
                .twoShiftsAmount(new BigDecimal(twoAmount))
                .validFrom(from)
                .validTo(to)
                .active(active)
                .build();
    }

    private Pochi pochi(Integer id) {
        return Pochi.builder().id(id).economicNumber("P-" + id).build();
    }

    private AdministrationFeeRuleResponse response(
            Integer id, Integer pochiId, String oneAmount, String twoAmount, boolean active) {
        return AdministrationFeeRuleResponse.builder()
                .id(id)
                .pochiId(pochiId)
                .pochiIdentifier(pochiId == null ? null : "P-" + pochiId)
                .oneShiftAmount(new BigDecimal(oneAmount))
                .twoShiftsAmount(new BigDecimal(twoAmount))
                .validFrom(VALID_FROM)
                .active(active)
                .global(pochiId == null)
                .build();
    }
}
