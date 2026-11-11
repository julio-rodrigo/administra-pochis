package com.administrapochi.services;

import com.administrapochi.exceptions.InvalidSettlementRuleDateRangeException;
import com.administrapochi.exceptions.NoApplicableSettlementRuleException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.exceptions.SettlementRuleNotFoundException;
import com.administrapochi.exceptions.SettlementRuleOverlapException;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleCreateRequest;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleResponse;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleUpdateRequest;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleValidToUpdateRequest;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.SettlementRule;
import com.administrapochi.models.mappers.SettlementRuleMapper;
import com.administrapochi.repositories.PochiRepository;
import com.administrapochi.repositories.SettlementRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementRuleServiceImplTest {

    private static final Integer POCHI_ID = 82;
    private static final Integer RULE_ID = 12;
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate VALID_TO = LocalDate.of(2026, 11, 30);
    private static final LocalDate DATE = LocalDate.of(2026, 11, 11);

    @Mock
    private SettlementRuleRepository settlementRuleRepository;
    @Mock
    private PochiRepository pochiRepository;
    @Mock
    private SettlementRuleMapper settlementRuleMapper;

    @InjectMocks
    private SettlementRuleServiceImpl service;

    @Test
    void createsGeneralOneShiftRule() {
        SettlementRuleCreateRequest request = createRequest(null, 1, "250.00", VALID_FROM, null);
        SettlementRule rule = rule(null, 1, "250.00", VALID_FROM, null);
        SettlementRuleResponse response = response(RULE_ID, null, "250.00");
        when(settlementRuleRepository.existsOverlappingGeneralRule(
                (short) 1, VALID_FROM, null, null)).thenReturn(false);
        when(settlementRuleMapper.toEntity(request)).thenReturn(rule);
        when(settlementRuleRepository.save(rule)).thenReturn(rule);
        when(settlementRuleMapper.toResponse(rule)).thenReturn(response);

        assertSame(response, service.create(request));
        assertEquals(null, rule.getPochi());
        verify(settlementRuleRepository).save(rule);
    }

    @Test
    void createsGeneralTwoShiftRule() {
        SettlementRuleCreateRequest request = createRequest(null, 2, "400.00", VALID_FROM, null);
        SettlementRule rule = rule(null, 2, "400.00", VALID_FROM, null);
        when(settlementRuleRepository.existsOverlappingGeneralRule(
                (short) 2, VALID_FROM, null, null)).thenReturn(false);
        when(settlementRuleMapper.toEntity(request)).thenReturn(rule);
        when(settlementRuleRepository.save(rule)).thenReturn(rule);
        when(settlementRuleMapper.toResponse(rule)).thenReturn(response(RULE_ID, null, "400.00"));

        assertEquals(new BigDecimal("400.00"), service.create(request).getAmount());
        verify(settlementRuleRepository).existsOverlappingGeneralRule(
                (short) 2, VALID_FROM, null, null);
    }

    @Test
    void createsPochiSpecificRuleUsingResolvedPochi() {
        SettlementRuleCreateRequest request = createRequest(POCHI_ID, 2, "350.00", VALID_FROM, VALID_TO);
        Pochi pochi = pochi();
        SettlementRule rule = rule(POCHI_ID, 2, "350.00", VALID_FROM, VALID_TO);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi));
        when(settlementRuleRepository.existsOverlappingPochiRule(
                POCHI_ID, (short) 2, VALID_FROM, VALID_TO, null)).thenReturn(false);
        when(settlementRuleMapper.toEntity(request)).thenReturn(rule);
        when(settlementRuleRepository.save(rule)).thenReturn(rule);
        when(settlementRuleMapper.toResponse(rule)).thenReturn(response(RULE_ID, POCHI_ID, "350.00"));

        assertEquals(POCHI_ID, service.create(request).getPochiId());
        assertSame(pochi, rule.getPochi());
    }

    @Test
    void rejectsInvalidDateRangeOnCreate() {
        SettlementRuleCreateRequest request = createRequest(
                null, 2, "400.00", VALID_TO, VALID_FROM);

        assertThrows(InvalidSettlementRuleDateRangeException.class, () -> service.create(request));

        verify(settlementRuleRepository, never()).existsOverlappingGeneralRule(any(), any(), any(), any());
        verify(settlementRuleRepository, never()).existsOverlappingPochiRule(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsUnknownPochiOnCreate() {
        SettlementRuleCreateRequest request = createRequest(POCHI_ID, 2, "350.00", VALID_FROM, null);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.empty());

        assertThrows(PochiNotFoundException.class, () -> service.create(request));

        verify(settlementRuleRepository, never()).existsOverlappingGeneralRule(any(), any(), any(), any());
        verify(settlementRuleRepository, never()).existsOverlappingPochiRule(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsOverlappingGeneralRule() {
        SettlementRuleCreateRequest request = createRequest(null, 2, "450.00", DATE, null);
        when(settlementRuleRepository.existsOverlappingGeneralRule(
                (short) 2, DATE, null, null)).thenReturn(true);

        assertThrows(SettlementRuleOverlapException.class, () -> service.create(request));

        verify(settlementRuleMapper, never()).toEntity(request);
    }

    @Test
    void rejectsOverlappingPochiSpecificRule() {
        SettlementRuleCreateRequest request = createRequest(POCHI_ID, 2, "300.00", DATE, null);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi()));
        when(settlementRuleRepository.existsOverlappingPochiRule(
                POCHI_ID, (short) 2, DATE, null, null)).thenReturn(true);

        assertThrows(SettlementRuleOverlapException.class, () -> service.create(request));

        verify(settlementRuleMapper, never()).toEntity(request);
    }

    @Test
    void findsRuleById() {
        SettlementRule rule = rule(null, 1, "250.00", VALID_FROM, null);
        SettlementRuleResponse response = response(RULE_ID, null, "250.00");
        when(settlementRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(settlementRuleMapper.toResponse(rule)).thenReturn(response);

        assertSame(response, service.findById(RULE_ID));
    }

    @Test
    void findByIdRejectsMissingRule() {
        when(settlementRuleRepository.findById(RULE_ID)).thenReturn(Optional.empty());

        assertThrows(SettlementRuleNotFoundException.class, () -> service.findById(RULE_ID));
    }

    @Test
    void updatesRuleAndExcludesCurrentRuleFromOverlapCheck() {
        SettlementRuleUpdateRequest request = updateRequest(null, 2, "425.00", VALID_FROM, VALID_TO);
        SettlementRule rule = rule(null, 2, "400.00", VALID_FROM, VALID_TO);
        rule.setId(RULE_ID);
        when(settlementRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(settlementRuleRepository.existsOverlappingGeneralRule(
                (short) 2, VALID_FROM, VALID_TO, RULE_ID)).thenReturn(false);
        when(settlementRuleRepository.save(rule)).thenReturn(rule);
        when(settlementRuleMapper.toResponse(rule)).thenReturn(response(RULE_ID, null, "425.00"));

        assertEquals(new BigDecimal("425.00"), service.update(RULE_ID, request).getAmount());

        verify(settlementRuleRepository).existsOverlappingGeneralRule(
                (short) 2, VALID_FROM, VALID_TO, RULE_ID);
        verify(settlementRuleMapper).updateEntity(request, rule);
    }

    @Test
    void rejectsUpdateThatWouldOverlapAnotherRule() {
        SettlementRuleUpdateRequest request = updateRequest(null, 2, "425.00", VALID_FROM, null);
        SettlementRule rule = rule(null, 2, "400.00", VALID_FROM, null);
        rule.setId(RULE_ID);
        when(settlementRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(settlementRuleRepository.existsOverlappingGeneralRule(
                (short) 2, VALID_FROM, null, RULE_ID)).thenReturn(true);

        assertThrows(SettlementRuleOverlapException.class, () -> service.update(RULE_ID, request));

        verify(settlementRuleMapper, never()).updateEntity(request, rule);
        verify(settlementRuleRepository, never()).save(any(SettlementRule.class));
    }

    @Test
    void rejectsUnknownPochiOnUpdate() {
        SettlementRuleUpdateRequest request = updateRequest(POCHI_ID, 2, "350.00", VALID_FROM, null);
        SettlementRule rule = rule(null, 2, "400.00", VALID_FROM, null);
        when(settlementRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.empty());

        assertThrows(PochiNotFoundException.class, () -> service.update(RULE_ID, request));

        verify(settlementRuleMapper, never()).updateEntity(request, rule);
    }

    @Test
    void updatesOnlyValidToAndExcludesCurrentRuleFromOverlapCheck() {
        SettlementRule rule = rule(null, 2, "400.00", VALID_FROM, null);
        rule.setId(RULE_ID);
        SettlementRuleValidToUpdateRequest request = validToRequest(VALID_TO);
        when(settlementRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(settlementRuleRepository.existsOverlappingGeneralRule(
                (short) 2, VALID_FROM, VALID_TO, RULE_ID)).thenReturn(false);
        when(settlementRuleRepository.save(rule)).thenReturn(rule);
        when(settlementRuleMapper.toResponse(rule)).thenReturn(SettlementRuleResponse.builder()
                .id(RULE_ID)
                .amount(new BigDecimal("400.00"))
                .validTo(VALID_TO)
                .build());

        SettlementRuleResponse result = service.updateValidTo(RULE_ID, request);

        assertEquals(VALID_TO, result.getValidTo());
        assertEquals(VALID_TO, rule.getValidTo());
        assertEquals(new BigDecimal("400.00"), rule.getAmount());
        assertEquals(VALID_FROM, rule.getValidFrom());
        verify(settlementRuleRepository).existsOverlappingGeneralRule(
                (short) 2, VALID_FROM, VALID_TO, RULE_ID);
        verify(settlementRuleRepository).save(rule);
        verify(settlementRuleMapper, never()).updateEntity(any(), any());
    }

    @Test
    void patchValidToRejectsDateBeforeValidFrom() {
        SettlementRule rule = rule(null, 2, "400.00", VALID_FROM, null);
        when(settlementRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));

        assertThrows(InvalidSettlementRuleDateRangeException.class,
                () -> service.updateValidTo(RULE_ID, validToRequest(VALID_FROM.minusDays(1))));

        verify(settlementRuleRepository, never()).save(any(SettlementRule.class));
    }

    @Test
    void patchValidToRejectsOverlappingRule() {
        SettlementRule rule = rule(null, 2, "400.00", VALID_FROM, null);
        rule.setId(RULE_ID);
        when(settlementRuleRepository.findById(RULE_ID)).thenReturn(Optional.of(rule));
        when(settlementRuleRepository.existsOverlappingGeneralRule(
                (short) 2, VALID_FROM, VALID_TO, RULE_ID)).thenReturn(true);

        assertThrows(SettlementRuleOverlapException.class,
                () -> service.updateValidTo(RULE_ID, validToRequest(VALID_TO)));

        assertEquals(null, rule.getValidTo());
        verify(settlementRuleRepository, never()).save(any(SettlementRule.class));
    }

    @Test
    void patchValidToRejectsMissingRule() {
        when(settlementRuleRepository.findById(RULE_ID)).thenReturn(Optional.empty());

        assertThrows(SettlementRuleNotFoundException.class,
                () -> service.updateValidTo(RULE_ID, validToRequest(VALID_TO)));
    }

    @Test
    void resolvesSpecificRuleBeforeGeneralRule() {
        Pochi pochi = pochi();
        SettlementRule specific = rule(POCHI_ID, 2, "350.00", VALID_FROM, null);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi));
        when(settlementRuleRepository.findApplicableForPochi(
                POCHI_ID, (short) 2, DATE)).thenReturn(List.of(specific));

        assertSame(specific, service.resolveApplicableRule(POCHI_ID, 2, DATE));

        verify(settlementRuleRepository, never()).findApplicableGeneral((short) 2, DATE);
    }

    @Test
    void resolvesGeneralRuleWhenSpecificRuleDoesNotExist() {
        Pochi pochi = pochi();
        SettlementRule general = rule(null, 2, "400.00", VALID_FROM, null);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi));
        when(settlementRuleRepository.findApplicableForPochi(
                POCHI_ID, (short) 2, DATE)).thenReturn(List.of());
        when(settlementRuleRepository.findApplicableGeneral((short) 2, DATE)).thenReturn(List.of(general));

        assertSame(general, service.resolveApplicableRule(POCHI_ID, 2, DATE));
    }

    @Test
    void throwsWhenNoApplicableRuleExists() {
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi()));
        when(settlementRuleRepository.findApplicableForPochi(
                POCHI_ID, (short) 2, DATE)).thenReturn(List.of());
        when(settlementRuleRepository.findApplicableGeneral((short) 2, DATE)).thenReturn(List.of());

        assertThrows(
                NoApplicableSettlementRuleException.class,
                () -> service.resolveApplicableRule(POCHI_ID, 2, DATE));
    }

    @Test
    void rejectsUnknownPochiDuringResolution() {
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.empty());

        assertThrows(
                PochiNotFoundException.class,
                () -> service.resolveApplicableRule(POCHI_ID, 2, DATE));

        verify(settlementRuleRepository, never()).findApplicableForPochi(any(), any(), any());
    }

    @Test
    void resolutionQueriesRespectInclusiveDateBoundariesAndOpenEndedRules() {
        Pochi pochi = pochi();
        LocalDate afterOpenEndedStart = LocalDate.of(2027, 1, 1);
        SettlementRule startsAtBoundary = rule(POCHI_ID, 2, "350.00", VALID_FROM, VALID_TO);
        SettlementRule endsAtBoundary = rule(null, 2, "400.00", VALID_FROM, VALID_TO);
        SettlementRule openEnded = rule(null, 2, "450.00", VALID_TO.plusDays(1), null);
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi));
        when(settlementRuleRepository.findApplicableForPochi(POCHI_ID, (short) 2, VALID_FROM))
                .thenReturn(List.of(startsAtBoundary));
        when(settlementRuleRepository.findApplicableForPochi(POCHI_ID, (short) 2, VALID_TO))
                .thenReturn(List.of(startsAtBoundary));
        when(settlementRuleRepository.findApplicableForPochi(POCHI_ID, (short) 2, VALID_FROM.minusDays(1)))
                .thenReturn(List.of());
        when(settlementRuleRepository.findApplicableForPochi(POCHI_ID, (short) 2, VALID_TO.plusDays(1)))
                .thenReturn(List.of());
        when(settlementRuleRepository.findApplicableGeneral((short) 2, VALID_FROM.minusDays(1)))
                .thenReturn(List.of());
        when(settlementRuleRepository.findApplicableGeneral((short) 2, VALID_TO.plusDays(1)))
                .thenReturn(List.of());
        when(settlementRuleRepository.findApplicableGeneral((short) 2, VALID_TO))
                .thenReturn(List.of(endsAtBoundary));
        when(settlementRuleRepository.findApplicableGeneral((short) 2, afterOpenEndedStart))
                .thenReturn(List.of(openEnded));

        assertSame(startsAtBoundary, service.resolveApplicableRule(POCHI_ID, 2, VALID_FROM));
        assertSame(startsAtBoundary, service.resolveApplicableRule(POCHI_ID, 2, VALID_TO));
        assertThrows(NoApplicableSettlementRuleException.class,
                () -> service.resolveApplicableRule(POCHI_ID, 2, VALID_FROM.minusDays(1)));
        assertThrows(NoApplicableSettlementRuleException.class,
                () -> service.resolveApplicableRule(POCHI_ID, 2, VALID_TO.plusDays(1)));
        assertSame(endsAtBoundary, service.resolveApplicableRule(null, 2, VALID_TO));
        assertSame(openEnded, service.resolveApplicableRule(null, 2, afterOpenEndedStart));
    }

    private SettlementRuleCreateRequest createRequest(
            Integer pochiId, Integer shiftCount, String amount, LocalDate validFrom, LocalDate validTo) {
        SettlementRuleCreateRequest request = new SettlementRuleCreateRequest();
        request.setPochiId(pochiId);
        request.setShiftCount(shiftCount);
        request.setAmount(new BigDecimal(amount));
        request.setValidFrom(validFrom);
        request.setValidTo(validTo);
        return request;
    }

    private SettlementRuleUpdateRequest updateRequest(
            Integer pochiId, Integer shiftCount, String amount, LocalDate validFrom, LocalDate validTo) {
        SettlementRuleUpdateRequest request = new SettlementRuleUpdateRequest();
        request.setPochiId(pochiId);
        request.setShiftCount(shiftCount);
        request.setAmount(new BigDecimal(amount));
        request.setValidFrom(validFrom);
        request.setValidTo(validTo);
        return request;
    }

    private SettlementRuleValidToUpdateRequest validToRequest(LocalDate validTo) {
        SettlementRuleValidToUpdateRequest request = new SettlementRuleValidToUpdateRequest();
        request.setValidTo(validTo);
        return request;
    }

    private SettlementRule rule(
            Integer pochiId, Integer shiftCount, String amount, LocalDate validFrom, LocalDate validTo) {
        SettlementRule rule = SettlementRule.builder()
                .shiftCount(shiftCount.shortValue())
                .amount(new BigDecimal(amount))
                .validFrom(validFrom)
                .validTo(validTo)
                .build();
        if (pochiId != null) {
            rule.setPochi(pochi());
        }
        return rule;
    }

    private Pochi pochi() {
        return Pochi.builder().id(POCHI_ID).economicNumber("P-82").build();
    }

    private SettlementRuleResponse response(Integer id, Integer pochiId, String amount) {
        return SettlementRuleResponse.builder()
                .id(id)
                .pochiId(pochiId)
                .amount(new BigDecimal(amount))
                .build();
    }
}
