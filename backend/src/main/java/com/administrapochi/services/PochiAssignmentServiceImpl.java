package com.administrapochi.services;

import com.administrapochi.exceptions.DriverDeletedException;
import com.administrapochi.exceptions.DriverNotFoundException;
import com.administrapochi.exceptions.AdministrationChargeConflictException;
import com.administrapochi.exceptions.InvalidPochiAssignmentFilterException;
import com.administrapochi.exceptions.PochiAssignmentConflictException;
import com.administrapochi.exceptions.PochiAssignmentDriverInactiveException;
import com.administrapochi.exceptions.PochiAssignmentNotFoundException;
import com.administrapochi.exceptions.PochiAssignmentPochiInactiveException;
import com.administrapochi.exceptions.PochiAssignmentReferencedException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentCreateRequest;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentResponse;
import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentUpdateRequest;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.models.enums.AssignmentShiftOption;
import com.administrapochi.models.enums.DriverStatus;
import com.administrapochi.models.enums.Shift;
import com.administrapochi.models.mappers.PochiAssignmentMapper;
import com.administrapochi.repositories.DriverRepository;
import com.administrapochi.repositories.AdministrationChargeRepository;
import com.administrapochi.repositories.PochiAssignmentRepository;
import com.administrapochi.repositories.PochiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PochiAssignmentServiceImpl implements PochiAssignmentService {

    private static final String POCHI_CONFLICT =
            "The Pochi already has a Driver assigned for the selected date and shift.";
    private static final String DRIVER_CONFLICT =
            "The Driver is already assigned to another Pochi for the selected date and shift.";
    private static final String FULL_DAY_CONFLICT =
            "A full-day assignment cannot be created because one or both shifts are unavailable.";

    private final PochiAssignmentRepository assignmentRepository;
    private final AdministrationChargeRepository administrationChargeRepository;
    private final DriverRepository driverRepository;
    private final PochiRepository pochiRepository;
    private final PochiAssignmentMapper assignmentMapper;
    private final AssignmentPeriodLockPolicy periodLockPolicy;
    private final PochiAssignmentReferencePolicy referencePolicy;

    @Override
    @Transactional
    public List<PochiAssignmentResponse> create(PochiAssignmentCreateRequest request) {
        validatePeriodIsOpen(request.getAssignmentDate());
        Driver driver = findActiveDriver(request.getDriverId());
        Pochi pochi = findActivePochi(request.getPochiId());
        if (administrationChargeRepository.existsByPochiIdAndChargeDate(
                pochi.getId(), request.getAssignmentDate())) {
            throw new AdministrationChargeConflictException(
                    "A Pochi Assignment cannot be created because an Administration Charge already exists "
                            + "for Pochi " + pochi.getId() + " on " + request.getAssignmentDate() + ".");
        }
        List<Shift> shifts = requestedShifts(request.getShift());

        try {
            for (Shift shift : shifts) {
                validateAvailability(request.getAssignmentDate(), pochi.getId(), driver.getId(), shift, null,
                        shifts.size() == 2);
            }
            List<PochiAssignment> assignments = new ArrayList<>();
            for (Shift shift : shifts) {
                assignments.add(assignmentMapper.toEntity(request, pochi, driver, shift));
            }
            return assignmentRepository.saveAllAndFlush(assignments).stream()
                    .map(assignmentMapper::toResponse)
                    .toList();
        } catch (DataIntegrityViolationException exception) {
            throw new PochiAssignmentConflictException(
                    shifts.size() == 2 ? FULL_DAY_CONFLICT : "The assignment conflicts with an existing assignment.");
        }
    }

    @Override
    public PochiAssignmentResponse findById(Integer id) {
        return assignmentMapper.toResponse(findAssignment(id));
    }

    @Override
    public Page<PochiAssignmentResponse> findAll(
            LocalDate assignmentDate,
            LocalDate startDate,
            LocalDate endDate,
            Integer driverId,
            Integer pochiId,
            Shift shift,
            Pageable pageable) {
        if ((startDate != null && endDate != null && endDate.isBefore(startDate))
                || (assignmentDate != null && (startDate != null || endDate != null))) {
            throw new InvalidPochiAssignmentFilterException();
        }

        Specification<PochiAssignment> specification = (root, query, builder) -> builder.conjunction();
        if (assignmentDate != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("assignmentDate"), assignmentDate));
        }
        if (startDate != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("assignmentDate"), startDate));
        }
        if (endDate != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("assignmentDate"), endDate));
        }
        if (driverId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("driver").get("id"), driverId));
        }
        if (pochiId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("pochi").get("id"), pochiId));
        }
        if (shift != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("shift"), shift));
        }
        return assignmentRepository.findAll(specification, pageable).map(assignmentMapper::toResponse);
    }

    @Override
    @Transactional
    public PochiAssignmentResponse update(Integer id, PochiAssignmentUpdateRequest request) {
        PochiAssignment assignment = findAssignment(id);
        validatePeriodIsOpen(assignment.getAssignmentDate());
        validatePeriodIsOpen(request.getAssignmentDate());
        if (referencePolicy.isReferenced(assignment)) {
            throw new PochiAssignmentReferencedException(id);
        }
        Driver driver = findActiveDriver(request.getDriverId());
        Pochi pochi = findActivePochi(request.getPochiId());
        validateAvailability(request.getAssignmentDate(), pochi.getId(), driver.getId(), request.getShift(), id, false);

        assignmentMapper.updateEntity(request, assignment);
        assignment.setDriver(driver);
        assignment.setPochi(pochi);
        try {
            return assignmentMapper.toResponse(assignmentRepository.saveAndFlush(assignment));
        } catch (DataIntegrityViolationException exception) {
            throw new PochiAssignmentConflictException(
                    "The assignment conflicts with an existing assignment.");
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        PochiAssignment assignment = findAssignment(id);
        validatePeriodIsOpen(assignment.getAssignmentDate());
        if (referencePolicy.isReferenced(assignment)) {
            throw new PochiAssignmentReferencedException(id);
        }
        assignmentRepository.delete(assignment);
    }

    private void validatePeriodIsOpen(LocalDate date) {
        periodLockPolicy.validatePeriodIsOpen(date);
    }

    private void validateAvailability(
            LocalDate date, Integer pochiId, Integer driverId, Shift shift, Integer excludedId, boolean fullDay) {
        boolean pochiAssigned = excludedId == null
                ? assignmentRepository.existsByAssignmentDateAndPochiIdAndShift(date, pochiId, shift)
                : assignmentRepository.existsByAssignmentDateAndPochiIdAndShiftAndIdNot(date, pochiId, shift, excludedId);
        boolean driverAssigned = excludedId == null
                ? assignmentRepository.existsByAssignmentDateAndDriverIdAndShift(date, driverId, shift)
                : assignmentRepository.existsByAssignmentDateAndDriverIdAndShiftAndIdNot(date, driverId, shift, excludedId);
        if (pochiAssigned || driverAssigned) {
            if (fullDay) {
                throw new PochiAssignmentConflictException(FULL_DAY_CONFLICT);
            }
            throw new PochiAssignmentConflictException(pochiAssigned ? POCHI_CONFLICT : DRIVER_CONFLICT);
        }
    }

    private List<Shift> requestedShifts(AssignmentShiftOption shift) {
        if (shift == AssignmentShiftOption.FULL_DAY) {
            return List.of(Shift.MORNING, Shift.AFTERNOON);
        }
        return List.of(Shift.valueOf(shift.name()));
    }

    private Driver findActiveDriver(Integer id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException(id));
        if (Boolean.TRUE.equals(driver.getDeleted())) {
            throw new DriverDeletedException(id);
        }
        if (driver.getEmploymentStatus() != DriverStatus.ACTIVE) {
            throw new PochiAssignmentDriverInactiveException(id);
        }
        return driver;
    }

    private Pochi findActivePochi(Integer id) {
        Pochi pochi = pochiRepository.findById(id).orElseThrow(() -> new PochiNotFoundException(id));
        if (!Boolean.TRUE.equals(pochi.getActive())) {
            throw new PochiAssignmentPochiInactiveException(id);
        }
        return pochi;
    }

    private PochiAssignment findAssignment(Integer id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new PochiAssignmentNotFoundException(id));
    }
}
