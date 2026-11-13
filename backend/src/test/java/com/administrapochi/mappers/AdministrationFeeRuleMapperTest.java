package com.administrapochi.mappers;

import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleCreateRequest;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleResponse;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleUpdateRequest;
import com.administrapochi.models.entities.AdministrationFeeRule;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.mappers.AdministrationFeeRuleMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AdministrationFeeRuleMapperTest {

    private final AdministrationFeeRuleMapper mapper =
            Mappers.getMapper(AdministrationFeeRuleMapper.class);

    @Test
    void mapsGlobalAndPochiSpecificRuleResponses() {
        LocalDate validFrom = LocalDate.of(2026, 11, 13);
        AdministrationFeeRule global = AdministrationFeeRule.builder()
                .id(1)
                .oneShiftAmount(new BigDecimal("50.00"))
                .twoShiftsAmount(new BigDecimal("70.00"))
                .validFrom(validFrom)
                .active(true)
                .build();
        AdministrationFeeRuleResponse globalResponse = mapper.toResponse(global);

        AdministrationFeeRule specific = AdministrationFeeRule.builder()
                .id(2)
                .pochi(Pochi.builder().id(16).economicNumber("P-16").build())
                .oneShiftAmount(new BigDecimal("60.00"))
                .twoShiftsAmount(new BigDecimal("80.00"))
                .validFrom(validFrom)
                .active(true)
                .build();
        AdministrationFeeRuleResponse specificResponse = mapper.toResponse(specific);

        assertThat(globalResponse.getGlobal()).isTrue();
        assertThat(globalResponse.getPochiId()).isNull();
        assertThat(globalResponse.getPochiIdentifier()).isNull();
        assertThat(specificResponse.getGlobal()).isFalse();
        assertThat(specificResponse.getPochiId()).isEqualTo(16);
        assertThat(specificResponse.getPochiIdentifier()).isEqualTo("P-16");
    }

    @Test
    void mapsCreateScalarsButLeavesPochiAndGeneratedFieldsForService() {
        AdministrationFeeRuleCreateRequest request = new AdministrationFeeRuleCreateRequest();
        request.setPochiId(16);
        request.setOneShiftAmount(new BigDecimal("60.00"));
        request.setTwoShiftsAmount(new BigDecimal("80.00"));
        request.setValidFrom(LocalDate.of(2026, 11, 13));
        request.setActive(null);

        AdministrationFeeRule rule = mapper.toEntity(request);

        assertThat(rule.getPochi()).isNull();
        assertThat(rule.getId()).isNull();
        assertThat(rule.getOneShiftAmount()).isEqualByComparingTo("60.00");
        assertThat(rule.getTwoShiftsAmount()).isEqualByComparingTo("80.00");
        assertThat(rule.getCreatedAt()).isNull();
        assertThat(rule.getUpdatedAt()).isNull();
    }

    @Test
    void updatePreservesScopeAndTimestamps() {
        LocalDate created = LocalDate.of(2026, 11, 1);
        Pochi pochi = Pochi.builder().id(16).economicNumber("P-16").build();
        AdministrationFeeRule rule = AdministrationFeeRule.builder()
                .id(2)
                .pochi(pochi)
                .oneShiftAmount(new BigDecimal("60.00"))
                .twoShiftsAmount(new BigDecimal("80.00"))
                .validFrom(created)
                .createdAt(created.atStartOfDay())
                .updatedAt(created.atStartOfDay())
                .build();
        AdministrationFeeRuleUpdateRequest request = new AdministrationFeeRuleUpdateRequest();
        request.setOneShiftAmount(new BigDecimal("65.00"));
        request.setTwoShiftsAmount(new BigDecimal("85.00"));
        request.setValidFrom(created.plusDays(1));
        request.setValidTo(null);
        request.setActive(false);

        mapper.updateEntity(request, rule);

        assertThat(rule.getId()).isEqualTo(2);
        assertThat(rule.getPochi()).isSameAs(pochi);
        assertThat(rule.getOneShiftAmount()).isEqualByComparingTo("65.00");
        assertThat(rule.getTwoShiftsAmount()).isEqualByComparingTo("85.00");
        assertThat(rule.getValidTo()).isNull();
        assertThat(rule.getActive()).isFalse();
        assertThat(rule.getCreatedAt()).isEqualTo(created.atStartOfDay());
        assertThat(rule.getUpdatedAt()).isEqualTo(created.atStartOfDay());
    }
}
