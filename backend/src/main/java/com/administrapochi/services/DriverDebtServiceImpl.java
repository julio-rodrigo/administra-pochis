package com.administrapochi.services;

import com.administrapochi.exceptions.DriverDebtConflictException;
import com.administrapochi.exceptions.DriverDebtNotFoundException;
import com.administrapochi.exceptions.DriverNotFoundException;
import com.administrapochi.exceptions.InvalidDriverDebtException;
import com.administrapochi.exceptions.InvalidDriverDebtFilterException;
import com.administrapochi.exceptions.SettlementNotFoundException;
import com.administrapochi.models.dtos.driverdebts.DriverDebtCreateRequest;
import com.administrapochi.models.dtos.driverdebts.DriverDebtPaymentCreateRequest;
import com.administrapochi.models.dtos.driverdebts.DriverDebtPaymentResponse;
import com.administrapochi.models.dtos.driverdebts.DriverDebtResponse;
import com.administrapochi.models.entities.DriverDebt;
import com.administrapochi.models.entities.DriverDebtPayment;
import com.administrapochi.models.enums.DriverDebtStatus;
import com.administrapochi.models.mappers.DriverDebtMapper;
import com.administrapochi.models.mappers.DriverDebtPaymentMapper;
import com.administrapochi.repositories.DriverDebtPaymentRepository;
import com.administrapochi.repositories.DriverDebtRepository;
import com.administrapochi.repositories.DriverRepository;
import com.administrapochi.repositories.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverDebtServiceImpl implements DriverDebtService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final DriverDebtRepository driverDebtRepository;
    private final DriverDebtPaymentRepository paymentRepository;
    private final DriverRepository driverRepository;
    private final SettlementRepository settlementRepository;
    private final DriverDebtMapper driverDebtMapper;
    private final DriverDebtPaymentMapper paymentMapper;

    @Override
    @Transactional
    public DriverDebtResponse create(DriverDebtCreateRequest request) {
        validateDebtRequest(request);
        var driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new DriverNotFoundException(request.getDriverId()));
        var settlement = request.getSettlementId() == null
                ? null
                : settlementRepository.findById(request.getSettlementId())
                        .orElseThrow(() -> new SettlementNotFoundException(request.getSettlementId()));

        DriverDebt debt = DriverDebt.builder()
                .driver(driver)
                .settlement(settlement)
                .amount(request.getAmount())
                .status(DriverDebtStatus.PENDING)
                .description(request.getDescription().trim())
                .debtDate(request.getDebtDate())
                .build();
        DriverDebt saved = driverDebtRepository.save(debt);
        return toResponse(saved, ZERO);
    }

    @Override
    public DriverDebtResponse findById(Integer id) {
        DriverDebt debt = findDebt(id);
        return toResponse(debt, paidAmount(id));
    }

    @Override
    public Page<DriverDebtResponse> findAll(
            Integer driverId,
            Integer settlementId,
            DriverDebtStatus status,
            LocalDate debtDateFrom,
            LocalDate debtDateTo,
            Pageable pageable) {
        if (debtDateFrom != null && debtDateTo != null && debtDateTo.isBefore(debtDateFrom)) {
            throw new InvalidDriverDebtFilterException();
        }

        Specification<DriverDebt> specification = (root, query, builder) -> builder.conjunction();
        if (driverId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("driver").get("id"), driverId));
        }
        if (settlementId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("settlement").get("id"), settlementId));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (debtDateFrom != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("debtDate"), debtDateFrom));
        }
        if (debtDateTo != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("debtDate"), debtDateTo));
        }
        return driverDebtRepository.findAll(specification, pageable).map(debt ->
                toResponse(debt, paidAmount(debt.getId())));
    }

    @Override
    @Transactional
    public DriverDebtPaymentResponse registerPayment(
            Integer debtId, DriverDebtPaymentCreateRequest request) {
        validatePaymentRequest(request);
        DriverDebt debt = driverDebtRepository.findByIdForUpdate(debtId)
                .orElseThrow(() -> new DriverDebtNotFoundException(debtId));

        if (debt.getStatus() == DriverDebtStatus.PAID) {
            throw new DriverDebtConflictException("Debt is already fully paid.");
        }
        if (debt.getStatus() == DriverDebtStatus.CANCELLED) {
            throw new DriverDebtConflictException("Cancelled debts cannot receive payments.");
        }

        BigDecimal currentPaid = paidAmount(debtId);
        BigDecimal remaining = debt.getAmount().subtract(currentPaid);
        if (request.getAmount().compareTo(remaining) > 0) {
            throw new DriverDebtConflictException(
                    "Payment amount cannot exceed the remaining debt balance.");
        }

        DriverDebtPayment payment = DriverDebtPayment.builder()
                .driverDebt(debt)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .notes(request.getNotes())
                .build();
        DriverDebtPayment savedPayment = paymentRepository.saveAndFlush(payment);

        BigDecimal totalPaid = paidAmount(debtId);
        if (totalPaid.compareTo(debt.getAmount()) == 0) {
            debt.setStatus(DriverDebtStatus.PAID);
        } else if (totalPaid.compareTo(ZERO) > 0) {
            debt.setStatus(DriverDebtStatus.PARTIALLY_PAID);
        }
        driverDebtRepository.save(debt);
        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    public List<DriverDebtPaymentResponse> findPayments(Integer debtId) {
        findDebt(debtId);
        return paymentRepository.findAllByDriverDebtIdOrderByPaymentDateAscIdAsc(debtId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DriverDebtResponse cancel(Integer debtId, String cancellationReason) {
        if (cancellationReason == null || cancellationReason.isBlank()
                || cancellationReason.length() > 500) {
            throw new InvalidDriverDebtException("cancellationReason must not be blank and must be at most 500 characters.");
        }
        DriverDebt debt = driverDebtRepository.findByIdForUpdate(debtId)
                .orElseThrow(() -> new DriverDebtNotFoundException(debtId));
        if (paymentRepository.existsByDriverDebtId(debtId)
                || paidAmount(debtId).compareTo(ZERO) != 0) {
            throw new DriverDebtConflictException(
                    "A debt with registered payments cannot be cancelled.");
        }
        if (debt.getStatus() != DriverDebtStatus.PENDING) {
            throw new DriverDebtConflictException("Only pending debts without payments can be cancelled.");
        }

        debt.setStatus(DriverDebtStatus.CANCELLED);
        debt.setCancellationReason(cancellationReason.trim());
        debt.setCancelledAt(LocalDateTime.now());
        return toResponse(driverDebtRepository.save(debt), ZERO);
    }

    private DriverDebt findDebt(Integer id) {
        return driverDebtRepository.findById(id)
                .orElseThrow(() -> new DriverDebtNotFoundException(id));
    }

    private BigDecimal paidAmount(Integer debtId) {
        BigDecimal amount = paymentRepository.sumAmountByDriverDebtId(debtId);
        return amount == null ? ZERO : amount;
    }

    private DriverDebtResponse toResponse(DriverDebt debt, BigDecimal paidAmount) {
        return driverDebtMapper.toResponse(debt, paidAmount);
    }

    private void validateDebtRequest(DriverDebtCreateRequest request) {
        if (request.getDriverId() == null || request.getDriverId() <= 0) {
            throw new InvalidDriverDebtException("driverId must be a positive integer.");
        }
        if (request.getSettlementId() != null && request.getSettlementId() <= 0) {
            throw new InvalidDriverDebtException("settlementId must be a positive integer.");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(ZERO) <= 0
                || request.getAmount().scale() > 2 || request.getAmount().precision() - request.getAmount().scale() > 8) {
            throw new InvalidDriverDebtException("amount must be greater than zero and have at most 8 integer and 2 decimal digits.");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()
                || request.getDescription().length() > 500) {
            throw new InvalidDriverDebtException("description must not be blank and must be at most 500 characters.");
        }
        if (request.getDebtDate() == null) {
            throw new InvalidDriverDebtException("debtDate is required.");
        }
    }

    private void validatePaymentRequest(DriverDebtPaymentCreateRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(ZERO) <= 0
                || request.getAmount().scale() > 2 || request.getAmount().precision() - request.getAmount().scale() > 8) {
            throw new InvalidDriverDebtException("amount must be greater than zero and have at most 8 integer and 2 decimal digits.");
        }
        if (request.getPaymentDate() == null) {
            throw new InvalidDriverDebtException("paymentDate is required.");
        }
        if (request.getNotes() != null && request.getNotes().length() > 500) {
            throw new InvalidDriverDebtException("notes must be at most 500 characters.");
        }
    }
}
