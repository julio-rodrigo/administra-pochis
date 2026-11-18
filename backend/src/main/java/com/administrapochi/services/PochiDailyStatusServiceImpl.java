package com.administrapochi.services;

import com.administrapochi.exceptions.InvalidPochiDailyStatusException;
import com.administrapochi.exceptions.PochiDailyStatusConflictException;
import com.administrapochi.exceptions.PochiDailyStatusNotFoundException;
import com.administrapochi.exceptions.UserNotFoundException;
import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusCreateRequest;
import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusResponse;
import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusUpdateRequest;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.PochiDailyStatus;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.enums.PochiDailyStatusType;
import com.administrapochi.models.mappers.PochiDailyStatusMapper;
import com.administrapochi.repositories.AdministrationChargeRepository;
import com.administrapochi.repositories.PochiAssignmentRepository;
import com.administrapochi.repositories.PochiDailyStatusRepository;
import com.administrapochi.repositories.SettlementRepository;
import com.administrapochi.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PochiDailyStatusServiceImpl implements PochiDailyStatusService {

    private static final Integer TEMPORARY_USER_ID = 2;
    private static final String UNIQUE_CONSTRAINT = "uq_pochi_daily_status_pochi_date";

    private final PochiDailyStatusRepository dailyStatusRepository;
    private final PochiAssignmentRepository assignmentRepository;
    private final SettlementRepository settlementRepository;
    private final AdministrationChargeRepository chargeRepository;
    private final UserRepository userRepository;
    private final PochiDailyStatusMapper mapper;
    private final PochiDailyStatusPolicy dailyStatusPolicy;
    private final WeeklyClosingMovementLockPolicy weeklyClosingMovementLockPolicy;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public PochiDailyStatusResponse create(PochiDailyStatusCreateRequest request) {
        if (request.getPochiId() == null || request.getPochiId() <= 0) {
            throw new InvalidPochiDailyStatusException("pochiId must be a positive integer.");
        }
        Pochi pochi = dailyStatusPolicy.lockPochi(request.getPochiId());
        weeklyClosingMovementLockPolicy.validateUnlocked(pochi.getId(), request.getStatusDate());
        String comment = validateAndNormalize(request.getStatusDate(), request.getStatus(), request.getComment());
        if (dailyStatusRepository.existsByPochiIdAndStatusDate(pochi.getId(), request.getStatusDate())) {
            throw duplicateStatus();
        }
        validateCompatibility(pochi.getId(), request.getStatusDate(), request.getStatus());
        User user = findTemporaryUser();
        PochiDailyStatus dailyStatus = mapper.toEntity(request);
        dailyStatus.setPochi(pochi);
        dailyStatus.setComment(comment);
        dailyStatus.setCreatedBy(user);
        dailyStatus.setCreatedAt(LocalDateTime.now());
        return mapper.toResponse(save(dailyStatus));
    }

    @Override
    public PochiDailyStatusResponse findById(Integer id) {
        return mapper.toResponse(findDailyStatus(id));
    }

    @Override
    public Page<PochiDailyStatusResponse> findAll(Integer pochiId, PochiDailyStatusType status,
            LocalDate statusDate, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        if ((pochiId != null && pochiId <= 0)
                || (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo))
                || (statusDate != null && (dateFrom != null || dateTo != null))) {
            throw new InvalidPochiDailyStatusException("Pochi daily status filters contain invalid values.");
        }
        Specification<PochiDailyStatus> specification = (root, query, builder) -> builder.conjunction();
        if (pochiId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("pochi").get("id"), pochiId));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (statusDate != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("statusDate"), statusDate));
        }
        if (dateFrom != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("statusDate"), dateFrom));
        }
        if (dateTo != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("statusDate"), dateTo));
        }
        return dailyStatusRepository.findAll(specification, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public PochiDailyStatusResponse update(Integer id, PochiDailyStatusUpdateRequest request) {
        PochiDailyStatus dailyStatus = findDailyStatus(id);
        Integer pochiId = dailyStatus.getPochi().getId();
        dailyStatusPolicy.lockPochi(pochiId);
        refreshDailyStatus(dailyStatus, id);
        weeklyClosingMovementLockPolicy.validateUnlocked(pochiId, dailyStatus.getStatusDate());
        weeklyClosingMovementLockPolicy.validateUnlocked(pochiId, request.getStatusDate());
        String comment = validateAndNormalize(request.getStatusDate(), request.getStatus(), request.getComment());
        if (dailyStatusRepository.existsByPochiIdAndStatusDateAndIdNot(pochiId, request.getStatusDate(), id)) {
            throw duplicateStatus();
        }
        validateCompatibility(pochiId, request.getStatusDate(), request.getStatus());
        User user = findTemporaryUser();
        mapper.updateEntity(request, dailyStatus);
        dailyStatus.setComment(comment);
        dailyStatus.setUpdatedBy(user);
        dailyStatus.setUpdatedAt(LocalDateTime.now());
        return mapper.toResponse(save(dailyStatus));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        PochiDailyStatus dailyStatus = findDailyStatus(id);
        dailyStatusPolicy.lockPochi(dailyStatus.getPochi().getId());
        refreshDailyStatus(dailyStatus, id);
        weeklyClosingMovementLockPolicy.validateUnlocked(
                dailyStatus.getPochi().getId(), dailyStatus.getStatusDate());
        dailyStatusRepository.delete(dailyStatus);
    }

    private PochiDailyStatus findDailyStatus(Integer id) {
        return dailyStatusRepository.findById(id)
                .orElseThrow(() -> new PochiDailyStatusNotFoundException(id));
    }

    private User findTemporaryUser() {
        return userRepository.findById(TEMPORARY_USER_ID)
                .orElseThrow(() -> new UserNotFoundException(TEMPORARY_USER_ID));
    }

    private void refreshDailyStatus(PochiDailyStatus dailyStatus, Integer id) {
        try {
            entityManager.refresh(dailyStatus);
        } catch (EntityNotFoundException exception) {
            throw new PochiDailyStatusNotFoundException(id);
        }
    }

    private String validateAndNormalize(LocalDate date, PochiDailyStatusType status, String comment) {
        if (date == null || status == null) {
            throw new InvalidPochiDailyStatusException("statusDate and status are required.");
        }
        if (comment != null && comment.length() > 500) {
            throw new InvalidPochiDailyStatusException("comment cannot exceed 500 characters.");
        }
        String normalized = comment == null || comment.isBlank() ? null : comment.trim();
        if (status == PochiDailyStatusType.NOT_WORKED && normalized == null) {
            throw new InvalidPochiDailyStatusException("A comment is required when the status is NOT_WORKED.");
        }
        return normalized;
    }

    private void validateCompatibility(Integer pochiId, LocalDate date, PochiDailyStatusType status) {
        if (status != PochiDailyStatusType.DAY_OFF
                && assignmentRepository.existsByPochiIdAndAssignmentDate(pochiId, date)) {
            throw new PochiDailyStatusConflictException(
                    status + " cannot be registered because assignments already exist for this Pochi and date.");
        }
        if (settlementRepository.existsByPochiIdAndWorkDate(pochiId, date)
                || chargeRepository.existsByPochiIdAndChargeDate(pochiId, date)) {
            throw new PochiDailyStatusConflictException(
                    "A Pochi daily status cannot be registered for a date with a Settlement or Administration Charge.");
        }
    }

    private PochiDailyStatus save(PochiDailyStatus dailyStatus) {
        try {
            return dailyStatusRepository.saveAndFlush(dailyStatus);
        } catch (DataIntegrityViolationException exception) {
            Throwable cause = exception;
            while (cause != null) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                        && UNIQUE_CONSTRAINT.equals(violation.getConstraintName())) {
                    throw duplicateStatus();
                }
                cause = cause.getCause();
            }
            throw exception;
        }
    }

    private PochiDailyStatusConflictException duplicateStatus() {
        return new PochiDailyStatusConflictException("A daily status already exists for this Pochi and date.");
    }
}
