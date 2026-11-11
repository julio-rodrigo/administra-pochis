package com.administrapochi.services;

import com.administrapochi.exceptions.DriverDeletedException;
import com.administrapochi.exceptions.DriverNotFoundException;
import com.administrapochi.exceptions.InvalidPochiAssignmentFilterException;
import com.administrapochi.exceptions.PochiAssignmentConflictException;
import com.administrapochi.exceptions.PochiAssignmentDriverInactiveException;
import com.administrapochi.exceptions.PochiAssignmentNotFoundException;
import com.administrapochi.exceptions.PochiAssignmentPeriodClosedException;
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
import com.administrapochi.models.enums.BloodType;
import com.administrapochi.models.enums.DriverStatus;
import com.administrapochi.models.enums.Shift;
import com.administrapochi.models.mappers.PochiAssignmentMapper;
import com.administrapochi.repositories.DriverRepository;
import com.administrapochi.repositories.PochiAssignmentRepository;
import com.administrapochi.repositories.PochiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PochiAssignmentServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

    @Mock
    private PochiAssignmentRepository assignmentRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private PochiRepository pochiRepository;
    @Mock
    private PochiAssignmentMapper assignmentMapper;
    @Mock
    private AssignmentPeriodLockPolicy periodLockPolicy;
    @Mock
    private PochiAssignmentReferencePolicy referencePolicy;

    @InjectMocks
    private PochiAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(driverRepository.findById(3)).thenReturn(Optional.of(driver()));
        lenient().when(pochiRepository.findById(82)).thenReturn(Optional.of(pochi()));
    }

    @Test
    void createSingleShiftPersistsOneAssignment() {
        PochiAssignment assignment = new PochiAssignment();
        PochiAssignmentResponse response = PochiAssignmentResponse.builder().id(1).shift("MORNING").build();
        PochiAssignmentCreateRequest request = createRequest(AssignmentShiftOption.MORNING);
        when(assignmentMapper.toEntity(eq(request), any(Pochi.class), any(Driver.class), eq(Shift.MORNING)))
                .thenReturn(assignment);
        when(assignmentRepository.saveAllAndFlush(anyList())).thenReturn(List.of(assignment));
        when(assignmentMapper.toResponse(assignment)).thenReturn(response);

        assertEquals(List.of(response), service.create(request));

        ArgumentCaptor<List<PochiAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(assignmentRepository).saveAllAndFlush(captor.capture());
        assertEquals(1, captor.getValue().size());
        verify(periodLockPolicy).validatePeriodIsOpen(DATE);
    }

    @Test
    void allowsSamePochiAndDriverAcrossDifferentShifts() {
        PochiAssignment afternoon = new PochiAssignment();
        PochiAssignmentCreateRequest request = createRequest(AssignmentShiftOption.AFTERNOON);
        lenient().when(assignmentRepository.existsByAssignmentDateAndPochiIdAndShift(DATE, 82, Shift.MORNING))
                .thenReturn(true);
        lenient().when(assignmentRepository.existsByAssignmentDateAndDriverIdAndShift(DATE, 3, Shift.MORNING))
                .thenReturn(true);
        when(assignmentMapper.toEntity(eq(request), any(Pochi.class), any(Driver.class), eq(Shift.AFTERNOON)))
                .thenReturn(afternoon);
        when(assignmentRepository.saveAllAndFlush(anyList())).thenReturn(List.of(afternoon));
        when(assignmentMapper.toResponse(afternoon))
                .thenReturn(PochiAssignmentResponse.builder().shift("AFTERNOON").build());

        assertEquals("AFTERNOON", service.create(request).getFirst().getShift());

        verify(assignmentRepository).existsByAssignmentDateAndPochiIdAndShift(DATE, 82, Shift.AFTERNOON);
        verify(assignmentRepository).existsByAssignmentDateAndDriverIdAndShift(DATE, 3, Shift.AFTERNOON);
    }

    @Test
    void allowsSameDriverOnDifferentPochisAcrossDifferentShifts() {
        Pochi pochi = Pochi.builder().id(83).economicNumber("P-83").active(true).build();
        PochiAssignment assignment = new PochiAssignment();
        PochiAssignmentCreateRequest request = createRequest(AssignmentShiftOption.AFTERNOON);
        request.setPochiId(83);
        when(pochiRepository.findById(83)).thenReturn(Optional.of(pochi));
        lenient().when(assignmentRepository.existsByAssignmentDateAndDriverIdAndShift(DATE, 3, Shift.MORNING))
                .thenReturn(true);
        when(assignmentMapper.toEntity(eq(request), eq(pochi), any(Driver.class), eq(Shift.AFTERNOON)))
                .thenReturn(assignment);
        when(assignmentRepository.saveAllAndFlush(anyList())).thenReturn(List.of(assignment));
        when(assignmentMapper.toResponse(assignment))
                .thenReturn(PochiAssignmentResponse.builder().shift("AFTERNOON").build());

        assertEquals(1, service.create(request).size());

        verify(assignmentRepository).existsByAssignmentDateAndDriverIdAndShift(DATE, 3, Shift.AFTERNOON);
    }

    @Test
    void createFullDayPersistsBothShiftsInOneTransactionalOperation() throws NoSuchMethodException {
        PochiAssignment morning = new PochiAssignment();
        PochiAssignment afternoon = new PochiAssignment();
        PochiAssignmentCreateRequest request = createRequest(AssignmentShiftOption.FULL_DAY);
        when(assignmentMapper.toEntity(eq(request), any(Pochi.class), any(Driver.class), eq(Shift.MORNING)))
                .thenReturn(morning);
        when(assignmentMapper.toEntity(eq(request), any(Pochi.class), any(Driver.class), eq(Shift.AFTERNOON)))
                .thenReturn(afternoon);
        when(assignmentRepository.saveAllAndFlush(anyList())).thenReturn(List.of(morning, afternoon));
        when(assignmentMapper.toResponse(morning)).thenReturn(PochiAssignmentResponse.builder().shift("MORNING").build());
        when(assignmentMapper.toResponse(afternoon))
                .thenReturn(PochiAssignmentResponse.builder().shift("AFTERNOON").build());

        assertEquals(2, service.create(request).size());

        ArgumentCaptor<List<PochiAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(assignmentRepository).saveAllAndFlush(captor.capture());
        assertEquals(List.of(morning, afternoon), captor.getValue());
        assertTrue(PochiAssignmentServiceImpl.class
                .getMethod("create", PochiAssignmentCreateRequest.class)
                .isAnnotationPresent(Transactional.class));
    }

    @Test
    void createFullDayRejectsUnavailableShiftBeforeSavingAnyRecord() {
        PochiAssignmentCreateRequest request = createRequest(AssignmentShiftOption.FULL_DAY);
        lenient().when(assignmentRepository.existsByAssignmentDateAndPochiIdAndShift(DATE, 82, Shift.AFTERNOON))
                .thenReturn(true);

        assertThrows(PochiAssignmentConflictException.class, () -> service.create(request));

        verify(assignmentRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void createChecksPeriodBeforeLookingUpResources() {
        PochiAssignmentCreateRequest request = createRequest(AssignmentShiftOption.MORNING);
        org.mockito.Mockito.doThrow(new PochiAssignmentPeriodClosedException())
                .when(periodLockPolicy).validatePeriodIsOpen(DATE);

        assertThrows(PochiAssignmentPeriodClosedException.class, () -> service.create(request));

        InOrder order = inOrder(periodLockPolicy, driverRepository, pochiRepository);
        order.verify(periodLockPolicy).validatePeriodIsOpen(DATE);
        order.verifyNoMoreInteractions();
        verify(assignmentRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void createRejectsMissingOrInactiveResources() {
        PochiAssignmentCreateRequest request = createRequest(AssignmentShiftOption.MORNING);
        when(driverRepository.findById(99)).thenReturn(Optional.empty());
        request.setDriverId(99);
        assertThrows(DriverNotFoundException.class, () -> service.create(request));

        request.setDriverId(3);
        when(pochiRepository.findById(99)).thenReturn(Optional.empty());
        request.setPochiId(99);
        assertThrows(PochiNotFoundException.class, () -> service.create(request));

        request.setPochiId(82);
        when(driverRepository.findById(4)).thenReturn(Optional.of(driver(DriverStatus.INACTIVE, false)));
        request.setDriverId(4);
        assertThrows(PochiAssignmentDriverInactiveException.class, () -> service.create(request));

        request.setDriverId(3);
        when(pochiRepository.findById(83)).thenReturn(Optional.of(Pochi.builder().id(83).active(false).build()));
        request.setPochiId(83);
        assertThrows(PochiAssignmentPochiInactiveException.class, () -> service.create(request));
    }

    @Test
    void createRejectsDeletedDriverAndBothSchedulingConflicts() {
        PochiAssignmentCreateRequest request = createRequest(AssignmentShiftOption.MORNING);
        when(driverRepository.findById(4)).thenReturn(Optional.of(driver(DriverStatus.ACTIVE, true)));
        request.setDriverId(4);
        assertThrows(DriverDeletedException.class, () -> service.create(request));

        request.setDriverId(3);
        when(assignmentRepository.existsByAssignmentDateAndPochiIdAndShift(DATE, 82, Shift.MORNING))
                .thenReturn(true);
        assertThrows(PochiAssignmentConflictException.class, () -> service.create(request));
        when(assignmentRepository.existsByAssignmentDateAndPochiIdAndShift(DATE, 82, Shift.MORNING))
                .thenReturn(false);
        when(assignmentRepository.existsByAssignmentDateAndDriverIdAndShift(DATE, 3, Shift.MORNING))
                .thenReturn(true);
        assertThrows(PochiAssignmentConflictException.class, () -> service.create(request));
    }

    @Test
    void databaseUniquenessViolationIsTranslatedToConflict() {
        PochiAssignmentCreateRequest request = createRequest(AssignmentShiftOption.MORNING);
        when(assignmentRepository.saveAllAndFlush(anyList())).thenThrow(new DataIntegrityViolationException("database"));

        assertThrows(PochiAssignmentConflictException.class, () -> service.create(request));
    }

    @Test
    void updateChecksBothPeriodDatesAndExcludesCurrentIdFromCollisionChecks() {
        PochiAssignment assignment = PochiAssignment.builder().id(5).assignmentDate(DATE).build();
        PochiAssignmentUpdateRequest request = updateRequest();
        when(assignmentRepository.findById(5)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.saveAndFlush(assignment)).thenReturn(assignment);
        when(assignmentMapper.toResponse(assignment)).thenReturn(PochiAssignmentResponse.builder().id(5).build());

        assertEquals(5, service.update(5, request).getId());

        InOrder order = inOrder(periodLockPolicy);
        order.verify(periodLockPolicy).validatePeriodIsOpen(DATE);
        order.verify(periodLockPolicy).validatePeriodIsOpen(LocalDate.of(2026, 11, 11));
        verify(assignmentRepository).existsByAssignmentDateAndPochiIdAndShiftAndIdNot(
                LocalDate.of(2026, 11, 11), 82, Shift.AFTERNOON, 5);
        verify(assignmentRepository).existsByAssignmentDateAndDriverIdAndShiftAndIdNot(
                LocalDate.of(2026, 11, 11), 3, Shift.AFTERNOON, 5);
    }

    @Test
    void updateRejectsClosedOriginalOrNewPeriod() {
        PochiAssignment assignment = PochiAssignment.builder().id(5).assignmentDate(DATE).build();
        PochiAssignmentUpdateRequest request = updateRequest();
        when(assignmentRepository.findById(5)).thenReturn(Optional.of(assignment));
        org.mockito.Mockito.doThrow(new PochiAssignmentPeriodClosedException())
                .when(periodLockPolicy).validatePeriodIsOpen(DATE);
        assertThrows(PochiAssignmentPeriodClosedException.class, () -> service.update(5, request));

        org.mockito.Mockito.doNothing().when(periodLockPolicy).validatePeriodIsOpen(DATE);
        org.mockito.Mockito.doThrow(new PochiAssignmentPeriodClosedException())
                .when(periodLockPolicy).validatePeriodIsOpen(LocalDate.of(2026, 11, 11));
        assertThrows(PochiAssignmentPeriodClosedException.class, () -> service.update(5, request));
        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateRejectsAssignmentReferencedByAnotherBusinessRecord() {
        PochiAssignment assignment = PochiAssignment.builder().id(5).assignmentDate(DATE).build();
        when(assignmentRepository.findById(5)).thenReturn(Optional.of(assignment));
        when(referencePolicy.isReferenced(assignment)).thenReturn(true);

        assertThrows(PochiAssignmentReferencedException.class, () -> service.update(5, updateRequest()));

        verify(assignmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void deleteRequiresOpenPeriodAndUnreferencedAssignment() {
        PochiAssignment assignment = PochiAssignment.builder().id(5).assignmentDate(DATE).build();
        when(assignmentRepository.findById(5)).thenReturn(Optional.of(assignment));
        when(referencePolicy.isReferenced(assignment)).thenReturn(true);
        assertThrows(PochiAssignmentReferencedException.class, () -> service.delete(5));

        when(referencePolicy.isReferenced(assignment)).thenReturn(false);
        service.delete(5);
        verify(assignmentRepository).delete(assignment);
        verify(periodLockPolicy, org.mockito.Mockito.times(2)).validatePeriodIsOpen(DATE);
    }

    @Test
    void deleteRejectsClosedPeriodAndMissingAssignment() {
        when(assignmentRepository.findById(5)).thenReturn(Optional.empty());
        assertThrows(PochiAssignmentNotFoundException.class, () -> service.delete(5));

        PochiAssignment assignment = PochiAssignment.builder().id(5).assignmentDate(DATE).build();
        when(assignmentRepository.findById(5)).thenReturn(Optional.of(assignment));
        org.mockito.Mockito.doThrow(new PochiAssignmentPeriodClosedException())
                .when(periodLockPolicy).validatePeriodIsOpen(DATE);
        assertThrows(PochiAssignmentPeriodClosedException.class, () -> service.delete(5));
        verify(assignmentRepository, never()).delete(any(PochiAssignment.class));
    }

    @Test
    void queriesSupportExactDatesRangesAndOtherFiltersWhileAllowingHistoricalReads() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<PochiAssignment> assignments = new PageImpl<>(List.of(PochiAssignment.builder().build()));
        when(assignmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(assignments);
        when(assignmentMapper.toResponse(any())).thenReturn(PochiAssignmentResponse.builder().build());

        Page<PochiAssignmentResponse> page = service.findAll(
                null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 3, 82, Shift.MORNING, pageable);

        assertEquals(1, page.getTotalElements());
        verify(assignmentRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
        verify(periodLockPolicy, never()).validatePeriodIsOpen(any());
        assertThrows(InvalidPochiAssignmentFilterException.class,
                () -> service.findAll(null, LocalDate.of(2026, 11, 11), DATE, null, null, null, pageable));
        assertThrows(InvalidPochiAssignmentFilterException.class,
                () -> service.findAll(DATE, DATE, null, null, null, null, pageable));
    }

    @Test
    void findByIdMapsAssignmentOrThrowsNotFound() {
        PochiAssignment assignment = new PochiAssignment();
        PochiAssignmentResponse response = PochiAssignmentResponse.builder().id(5).build();
        when(assignmentRepository.findById(5)).thenReturn(Optional.of(assignment));
        when(assignmentMapper.toResponse(assignment)).thenReturn(response);

        assertEquals(response, service.findById(5));

        when(assignmentRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(PochiAssignmentNotFoundException.class, () -> service.findById(99));
    }

    private PochiAssignmentCreateRequest createRequest(AssignmentShiftOption shift) {
        PochiAssignmentCreateRequest request = new PochiAssignmentCreateRequest();
        request.setAssignmentDate(DATE);
        request.setPochiId(82);
        request.setDriverId(3);
        request.setShift(shift);
        return request;
    }

    private PochiAssignmentUpdateRequest updateRequest() {
        PochiAssignmentUpdateRequest request = new PochiAssignmentUpdateRequest();
        request.setAssignmentDate(LocalDate.of(2026, 11, 11));
        request.setPochiId(82);
        request.setDriverId(3);
        request.setShift(Shift.AFTERNOON);
        return request;
    }

    private Driver driver() {
        return driver(DriverStatus.ACTIVE, false);
    }

    private Driver driver(DriverStatus status, boolean deleted) {
        return Driver.builder()
                .id(3)
                .firstName("Test")
                .firstLastName("Driver")
                .curp("ABCD123456EFGHIJKL")
                .bloodType(BloodType.O_POSITIVE)
                .employmentStatus(status)
                .deleted(deleted)
                .build();
    }

    private Pochi pochi() {
        return Pochi.builder().id(82).economicNumber("P-82").active(true).build();
    }
}
