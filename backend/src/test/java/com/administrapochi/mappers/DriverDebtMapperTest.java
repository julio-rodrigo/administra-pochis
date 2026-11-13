package com.administrapochi.mappers;

import com.administrapochi.models.dtos.driverdebts.DriverDebtResponse;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.DriverDebt;
import com.administrapochi.models.entities.Settlement;
import com.administrapochi.models.enums.DriverDebtStatus;
import com.administrapochi.models.mappers.DriverDebtMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DriverDebtMapperTest {

    private final DriverDebtMapper mapper = Mappers.getMapper(DriverDebtMapper.class);

    @Test
    void mapsDebtAndCalculatesPaidAndRemainingAmounts() {
        DriverDebt debt = DriverDebt.builder()
                .id(1)
                .driver(Driver.builder()
                        .id(3)
                        .firstName("Ana")
                        .firstLastName("Lopez")
                        .secondLastName("Diaz")
                        .build())
                .settlement(Settlement.builder().id(8).build())
                .amount(new BigDecimal("500.00"))
                .status(DriverDebtStatus.PARTIALLY_PAID)
                .description("Manual debt")
                .debtDate(LocalDate.of(2026, 11, 13))
                .build();

        DriverDebtResponse response = mapper.toResponse(debt, new BigDecimal("125.50"));

        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getDriverId()).isEqualTo(3);
        assertThat(response.getDriverName()).isEqualTo("Ana Lopez Diaz");
        assertThat(response.getSettlementId()).isEqualTo(8);
        assertThat(response.getPaidAmount()).isEqualByComparingTo("125.50");
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("374.50");
    }

    @Test
    void debtWithoutSettlementOrPaymentsHasFullBalanceRemaining() {
        DriverDebt debt = DriverDebt.builder()
                .amount(new BigDecimal("75.00"))
                .driver(Driver.builder().id(3).firstName("Ana").firstLastName("Lopez").build())
                .status(DriverDebtStatus.PENDING)
                .build();

        DriverDebtResponse response = mapper.toResponse(debt, BigDecimal.ZERO);

        assertThat(response.getSettlementId()).isNull();
        assertThat(response.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("75.00");
    }
}
