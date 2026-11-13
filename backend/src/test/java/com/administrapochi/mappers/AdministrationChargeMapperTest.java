package com.administrapochi.mappers;

import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeResponse;
import com.administrapochi.models.entities.AdministrationCharge;
import com.administrapochi.models.entities.AdministrationChargeSettlement;
import com.administrapochi.models.entities.AdministrationChargeSettlementId;
import com.administrapochi.models.entities.AdministrationFeeRule;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.models.entities.Settlement;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.enums.Shift;
import com.administrapochi.models.mappers.AdministrationChargeMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AdministrationChargeMapperTest {

    private final AdministrationChargeMapper mapper = Mappers.getMapper(AdministrationChargeMapper.class);

    @Test
    void mapsChargeAuditAndSettlementSummariesWithoutExposingEntities() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 13, 14, 30);
        Pochi pochi = Pochi.builder().id(35).economicNumber("P-35").build();
        PochiAssignment morning = PochiAssignment.builder().id(101).shift(Shift.MORNING).build();
        PochiAssignment afternoon = PochiAssignment.builder().id(102).shift(Shift.AFTERNOON).build();
        Settlement settlement = Settlement.builder()
                .id(501)
                .pochiAssignments(Set.of(morning, afternoon))
                .expectedAmount(new BigDecimal("200.00"))
                .acceptedAmount(new BigDecimal("190.00"))
                .createdAt(createdAt)
                .build();
        AdministrationCharge charge = AdministrationCharge.builder()
                .id(700)
                .pochi(pochi)
                .chargeDate(LocalDate.of(2026, 11, 13))
                .administrationFeeRule(AdministrationFeeRule.builder()
                        .id(22)
                        .oneShiftAmount(new BigDecimal("70.00"))
                        .twoShiftsAmount(new BigDecimal("100.00"))
                        .build())
                .shiftCount((short) 2)
                .suggestedAmount(new BigDecimal("70.00"))
                .amount(new BigDecimal("70.0"))
                .createdBy(User.builder().id(9).name("Administrator").build())
                .createdAt(createdAt)
                .build();
        AdministrationChargeSettlement link = AdministrationChargeSettlement.builder()
                .id(new AdministrationChargeSettlementId(700, 501))
                .administrationCharge(charge)
                .settlement(settlement)
                .build();

        AdministrationChargeResponse response = mapper.toResponse(charge, List.of(link));

        assertThat(response.getPochiId()).isEqualTo(35);
        assertThat(response.getPochiIdentifier()).isEqualTo("P-35");
        assertThat(response.getAdministrationFeeRuleId()).isEqualTo(22);
        assertThat(response.getGlobalRule()).isTrue();
        assertThat(response.getAdjusted()).isFalse();
        assertThat(response.getCreatedById()).isEqualTo(9);
        assertThat(response.getCreatedByName()).isEqualTo("Administrator");
        assertThat(response.getSettlements()).singleElement().satisfies(summary -> {
            assertThat(summary.getSettlementId()).isEqualTo(501);
            assertThat(summary.getPochiAssignmentIds()).containsExactly(101, 102);
            assertThat(summary.getShifts()).isEqualTo("AFTERNOON,MORNING");
            assertThat(summary.getExpectedAmount()).isEqualByComparingTo("200.00");
            assertThat(summary.getAcceptedAmount()).isEqualByComparingTo("190.00");
            assertThat(summary.getSettledAt()).isEqualTo(createdAt);
        });

        charge.setAmount(new BigDecimal("60.00"));
        assertThat(mapper.toResponse(charge, List.of(link)).getAdjusted()).isTrue();
    }
}
