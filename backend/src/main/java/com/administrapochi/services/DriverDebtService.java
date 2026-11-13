package com.administrapochi.services;

import com.administrapochi.models.dtos.driverdebts.DriverDebtCreateRequest;
import com.administrapochi.models.dtos.driverdebts.DriverDebtPaymentCreateRequest;
import com.administrapochi.models.dtos.driverdebts.DriverDebtPaymentResponse;
import com.administrapochi.models.dtos.driverdebts.DriverDebtResponse;
import com.administrapochi.models.enums.DriverDebtStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface DriverDebtService {

    DriverDebtResponse create(DriverDebtCreateRequest request);

    DriverDebtResponse findById(Integer id);

    Page<DriverDebtResponse> findAll(
            Integer driverId, Integer settlementId, DriverDebtStatus status,
            LocalDate debtDateFrom, LocalDate debtDateTo, Pageable pageable);

    DriverDebtPaymentResponse registerPayment(Integer debtId, DriverDebtPaymentCreateRequest request);

    List<DriverDebtPaymentResponse> findPayments(Integer debtId);

    DriverDebtResponse cancel(Integer debtId, String cancellationReason);
}
