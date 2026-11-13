package com.administrapochi.services;

import com.administrapochi.exceptions.DriverDebtConflictException;
import com.administrapochi.exceptions.DriverNotFoundException;
import com.administrapochi.exceptions.InvalidDriverDebtException;
import com.administrapochi.exceptions.SettlementNotFoundException;
import com.administrapochi.models.dtos.driverdebts.DriverDebtCreateRequest;
import com.administrapochi.models.dtos.driverdebts.DriverDebtPaymentCreateRequest;
import com.administrapochi.models.dtos.driverdebts.DriverDebtPaymentResponse;
import com.administrapochi.models.dtos.driverdebts.DriverDebtResponse;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.DriverDebt;
import com.administrapochi.models.entities.DriverDebtPayment;
import com.administrapochi.models.entities.Settlement;
import com.administrapochi.models.enums.DriverDebtStatus;
import com.administrapochi.models.mappers.DriverDebtMapper;
import com.administrapochi.models.mappers.DriverDebtPaymentMapper;
import com.administrapochi.repositories.DriverDebtPaymentRepository;
import com.administrapochi.repositories.DriverDebtRepository;
import com.administrapochi.repositories.DriverRepository;
import com.administrapochi.repositories.SettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverDebtServiceImplTest {

    private static final Integer DEBT_ID = 12;

    @Mock
    private DriverDebtRepository debtRepository;
    @Mock
    private DriverDebtPaymentRepository paymentRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private DriverDebtMapper debtMapper;
    @Mock
    private DriverDebtPaymentMapper paymentMapper;

    @InjectMocks
    private DriverDebtServiceImpl service;

    @Test
    void createsOptionalSettlementDebtAsPendingWithoutStoringPaidBalance() {
        Driver driver = Driver.builder().id(4).build();
        DriverDebtCreateRequest request = debtRequest("500.00");
        when(driverRepository.findById(4)).thenReturn(Optional.of(driver));
        when(debtRepository.save(any(DriverDebt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(debtMapper.toResponse(any(DriverDebt.class), any(BigDecimal.class)))
                .thenReturn(DriverDebtResponse.builder().status(DriverDebtStatus.PENDING).build());

        DriverDebtResponse response = service.create(request);

        ArgumentCaptor<DriverDebt> debtCaptor = ArgumentCaptor.forClass(DriverDebt.class);
        verify(debtRepository).save(debtCaptor.capture());
        DriverDebt saved = debtCaptor.getValue();
        assertThat(response.getStatus()).isEqualTo(DriverDebtStatus.PENDING);
        assertThat(saved.getDriver()).isSameAs(driver);
        assertThat(saved.getSettlement()).isNull();
        assertThat(saved.getAmount()).isEqualByComparingTo("500.00");
        assertThat(saved.getStatus()).isEqualTo(DriverDebtStatus.PENDING);
        verify(debtMapper).toResponse(saved, BigDecimal.ZERO);
        verify(settlementRepository, never()).findById(any());
    }

    @Test
    void rejectsNonexistentDriverAndNonpositiveAmount() {
        DriverDebtCreateRequest request = debtRequest("10.00");
        when(driverRepository.findById(4)).thenReturn(Optional.empty());

        assertThrows(DriverNotFoundException.class, () -> service.create(request));

        request.setAmount(new BigDecimal("0.00"));
        assertThrows(InvalidDriverDebtException.class, () -> service.create(request));
        request.setAmount(new BigDecimal("-0.01"));
        assertThrows(InvalidDriverDebtException.class, () -> service.create(request));
        verify(debtRepository, never()).save(any());
    }

    @Test
    void rejectsNonexistentSettlement() {
        DriverDebtCreateRequest request = debtRequest("10.00");
        request.setSettlementId(55);
        when(driverRepository.findById(4)).thenReturn(Optional.of(Driver.builder().id(4).build()));
        when(settlementRepository.findById(55)).thenReturn(Optional.empty());

        assertThrows(SettlementNotFoundException.class, () -> service.create(request));

        verify(debtRepository, never()).save(any());
    }

    @Test
    void createsDebtAssociatedWithSettlementWithoutDerivingAmount() {
        Driver driver = Driver.builder().id(4).build();
        Settlement settlement = Settlement.builder().id(8).acceptedAmount(new BigDecimal("300.00")).build();
        DriverDebtCreateRequest request = debtRequest("125.00");
        request.setSettlementId(8);
        when(driverRepository.findById(4)).thenReturn(Optional.of(driver));
        when(settlementRepository.findById(8)).thenReturn(Optional.of(settlement));
        when(debtRepository.save(any(DriverDebt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(debtMapper.toResponse(any(DriverDebt.class), any(BigDecimal.class)))
                .thenReturn(DriverDebtResponse.builder().status(DriverDebtStatus.PENDING).build());

        service.create(request);

        ArgumentCaptor<DriverDebt> debtCaptor = ArgumentCaptor.forClass(DriverDebt.class);
        verify(debtRepository).save(debtCaptor.capture());
        assertThat(debtCaptor.getValue().getSettlement()).isSameAs(settlement);
        assertThat(debtCaptor.getValue().getAmount()).isEqualByComparingTo("125.00");
    }

    @Test
    void registersPartialAndFinalPaymentsAndTransitionsStatus() {
        DriverDebt debt = debt(DriverDebtStatus.PENDING, "500.00");
        when(debtRepository.findByIdForUpdate(DEBT_ID)).thenReturn(Optional.of(debt));
        when(paymentRepository.sumAmountByDriverDebtId(DEBT_ID))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("100.00"),
                        new BigDecimal("100.00"), new BigDecimal("500.00"));
        when(paymentRepository.saveAndFlush(any(DriverDebtPayment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toResponse(any(DriverDebtPayment.class)))
                .thenReturn(DriverDebtPaymentResponse.builder().build());

        service.registerPayment(DEBT_ID, paymentRequest("100.00"));

        assertThat(debt.getStatus()).isEqualTo(DriverDebtStatus.PARTIALLY_PAID);
        service.registerPayment(DEBT_ID, paymentRequest("400.00"));

        assertThat(debt.getStatus()).isEqualTo(DriverDebtStatus.PAID);
        verify(debtRepository, org.mockito.Mockito.times(2)).save(debt);
    }

    @Test
    void rejectsOverpaymentWithoutRecordingPayment() {
        DriverDebt debt = debt(DriverDebtStatus.PARTIALLY_PAID, "500.00");
        when(debtRepository.findByIdForUpdate(DEBT_ID)).thenReturn(Optional.of(debt));
        when(paymentRepository.sumAmountByDriverDebtId(DEBT_ID)).thenReturn(new BigDecimal("350.00"));

        DriverDebtConflictException exception = assertThrows(
                DriverDebtConflictException.class,
                () -> service.registerPayment(DEBT_ID, paymentRequest("150.01")));

        assertThat(exception).hasMessage("Payment amount cannot exceed the remaining debt balance.");
        verify(paymentRepository, never()).saveAndFlush(any());
        verify(debtRepository, never()).save(any());
    }

    @Test
    void rejectsZeroAndNegativePaymentsBeforeLoadingDebt() {
        assertThrows(InvalidDriverDebtException.class,
                () -> service.registerPayment(DEBT_ID, paymentRequest("0.00")));
        assertThrows(InvalidDriverDebtException.class,
                () -> service.registerPayment(DEBT_ID, paymentRequest("-1.00")));

        verify(debtRepository, never()).findByIdForUpdate(any());
        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void allowsDirectFullPaymentOfPendingDebt() {
        DriverDebt debt = debt(DriverDebtStatus.PENDING, "100.00");
        when(debtRepository.findByIdForUpdate(DEBT_ID)).thenReturn(Optional.of(debt));
        when(paymentRepository.sumAmountByDriverDebtId(DEBT_ID))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("100.00"));
        when(paymentRepository.saveAndFlush(any(DriverDebtPayment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toResponse(any(DriverDebtPayment.class)))
                .thenReturn(DriverDebtPaymentResponse.builder().build());

        service.registerPayment(DEBT_ID, paymentRequest("100.00"));

        assertThat(debt.getStatus()).isEqualTo(DriverDebtStatus.PAID);
    }

    @Test
    void rejectsPaymentForPaidOrCancelledDebt() {
        for (DriverDebtStatus status : new DriverDebtStatus[] {
                DriverDebtStatus.PAID, DriverDebtStatus.CANCELLED
        }) {
            when(debtRepository.findByIdForUpdate(DEBT_ID))
                    .thenReturn(Optional.of(debt(status, "100.00")));

            assertThrows(DriverDebtConflictException.class,
                    () -> service.registerPayment(DEBT_ID, paymentRequest("10.00")));
        }

        verify(paymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancellationRequiresReasonAndNoPaymentHistory() {
        DriverDebt debt = debt(DriverDebtStatus.PENDING, "100.00");
        when(debtRepository.findByIdForUpdate(DEBT_ID)).thenReturn(Optional.of(debt));
        when(paymentRepository.existsByDriverDebtId(DEBT_ID)).thenReturn(false);
        when(paymentRepository.sumAmountByDriverDebtId(DEBT_ID)).thenReturn(BigDecimal.ZERO);
        when(debtRepository.save(debt)).thenReturn(debt);
        when(debtMapper.toResponse(debt, BigDecimal.ZERO))
                .thenReturn(DriverDebtResponse.builder().status(DriverDebtStatus.CANCELLED).build());

        assertThrows(InvalidDriverDebtException.class, () -> service.cancel(DEBT_ID, " "));
        DriverDebtResponse response = service.cancel(DEBT_ID, "Entered in error");

        assertThat(response.getStatus()).isEqualTo(DriverDebtStatus.CANCELLED);
        assertThat(debt.getStatus()).isEqualTo(DriverDebtStatus.CANCELLED);
        assertThat(debt.getCancellationReason()).isEqualTo("Entered in error");
        assertThat(debt.getCancelledAt()).isNotNull();
    }

    @Test
    void rejectsCancellationAfterAnyPaymentRecord() {
        when(debtRepository.findByIdForUpdate(DEBT_ID))
                .thenReturn(Optional.of(debt(DriverDebtStatus.PARTIALLY_PAID, "100.00")));
        when(paymentRepository.existsByDriverDebtId(DEBT_ID)).thenReturn(true);

        DriverDebtConflictException exception = assertThrows(
                DriverDebtConflictException.class, () -> service.cancel(DEBT_ID, "Do not cancel"));

        assertThat(exception).hasMessage("A debt with registered payments cannot be cancelled.");
        verify(debtRepository, never()).save(any());
    }

    @Test
    void calculatesBalancesFromPaymentAggregate() {
        DriverDebt debt = debt(DriverDebtStatus.PARTIALLY_PAID, "500.00");
        when(debtRepository.findById(DEBT_ID)).thenReturn(Optional.of(debt));
        when(paymentRepository.sumAmountByDriverDebtId(DEBT_ID)).thenReturn(new BigDecimal("125.50"));
        when(debtMapper.toResponse(debt, new BigDecimal("125.50")))
                .thenReturn(DriverDebtResponse.builder()
                        .paidAmount(new BigDecimal("125.50"))
                        .remainingAmount(new BigDecimal("374.50"))
                        .build());

        DriverDebtResponse response = service.findById(DEBT_ID);

        assertThat(response.getPaidAmount()).isEqualByComparingTo("125.50");
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("374.50");
    }

    private DriverDebtCreateRequest debtRequest(String amount) {
        DriverDebtCreateRequest request = new DriverDebtCreateRequest();
        request.setDriverId(4);
        request.setAmount(new BigDecimal(amount));
        request.setDescription("Manual driver debt");
        request.setDebtDate(LocalDate.of(2026, 11, 13));
        return request;
    }

    private DriverDebtPaymentCreateRequest paymentRequest(String amount) {
        DriverDebtPaymentCreateRequest request = new DriverDebtPaymentCreateRequest();
        request.setAmount(new BigDecimal(amount));
        request.setPaymentDate(LocalDate.of(2026, 11, 13));
        return request;
    }

    private DriverDebt debt(DriverDebtStatus status, String amount) {
        return DriverDebt.builder()
                .id(DEBT_ID)
                .driver(Driver.builder().id(4).firstName("Ada").firstLastName("Driver").build())
                .amount(new BigDecimal(amount))
                .status(status)
                .description("Manual driver debt")
                .debtDate(LocalDate.of(2026, 11, 13))
                .build();
    }
}
