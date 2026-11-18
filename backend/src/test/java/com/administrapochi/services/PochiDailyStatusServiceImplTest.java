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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PochiDailyStatusServiceImplTest {

    private static final Integer POCHI_ID = 82;
    private static final Integer STATUS_ID = 7;
    private static final LocalDate DATE = LocalDate.of(2026, 10, 19);
    private final Pochi pochi = Pochi.builder().id(POCHI_ID).economicNumber("P-82").build();
    private final User user = User.builder().id(2).build();

    @Mock private PochiDailyStatusRepository dailyStatusRepository;
    @Mock private PochiAssignmentRepository assignmentRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private AdministrationChargeRepository chargeRepository;
    @Mock private UserRepository userRepository;
    @Mock private PochiDailyStatusMapper mapper;
    @Mock private PochiDailyStatusPolicy dailyStatusPolicy;
    @Mock private EntityManager entityManager;
    @InjectMocks private PochiDailyStatusServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(dailyStatusPolicy.lockPochi(POCHI_ID)).thenReturn(pochi);
        lenient().when(userRepository.findById(2)).thenReturn(Optional.of(user));
    }

    @ParameterizedTest
    @EnumSource(PochiDailyStatusType.class)
    void createsEveryStatusAndSetsCreationAudit(PochiDailyStatusType status) {
        PochiDailyStatusCreateRequest request = request(status,
                status == PochiDailyStatusType.NOT_WORKED ? "  Workshop  " : "  ");
        PochiDailyStatus entity = PochiDailyStatus.builder().status(status).statusDate(DATE).build();
        PochiDailyStatusResponse response = PochiDailyStatusResponse.builder().status(status).build();
        when(mapper.toEntity(request)).thenReturn(entity);
        when(dailyStatusRepository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);
        if (status == PochiDailyStatusType.DAY_OFF) {
            lenient().when(assignmentRepository.existsByPochiIdAndAssignmentDate(POCHI_ID, DATE)).thenReturn(true);
        }

        assertThat(service.create(request)).isSameAs(response);
        assertThat(entity.getPochi()).isSameAs(pochi);
        assertThat(entity.getCreatedBy()).isSameAs(user);
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedBy()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
        assertThat(entity.getComment()).isEqualTo(status == PochiDailyStatusType.NOT_WORKED ? "Workshop" : null);
        if (status == PochiDailyStatusType.DAY_OFF) {
            verify(assignmentRepository, never()).existsByPochiIdAndAssignmentDate(any(), any());
        } else {
            verify(assignmentRepository).existsByPochiIdAndAssignmentDate(POCHI_ID, DATE);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void rejectsNotWorkedWithBlankComment(String comment) {
        assertThrows(InvalidPochiDailyStatusException.class,
                () -> service.create(request(PochiDailyStatusType.NOT_WORKED, comment)));
        verify(dailyStatusRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @EnumSource(value = PochiDailyStatusType.class, names = {"UNION_REST", "NOT_WORKED"})
    void rejectsNonWorkingStatusWhenAssignmentsExist(PochiDailyStatusType status) {
        when(assignmentRepository.existsByPochiIdAndAssignmentDate(POCHI_ID, DATE)).thenReturn(true);
        assertThrows(PochiDailyStatusConflictException.class,
                () -> service.create(request(status, "Reason")));
        verify(dailyStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsDuplicateStatusAndExistingFinancialRecords() {
        PochiDailyStatusCreateRequest request = request(PochiDailyStatusType.DAY_OFF, null);
        when(dailyStatusRepository.existsByPochiIdAndStatusDate(POCHI_ID, DATE)).thenReturn(true, false, false);
        when(settlementRepository.existsByPochiIdAndWorkDate(POCHI_ID, DATE)).thenReturn(true, false);
        when(chargeRepository.existsByPochiIdAndChargeDate(POCHI_ID, DATE)).thenReturn(true);

        assertThrows(PochiDailyStatusConflictException.class, () -> service.create(request));
        assertThrows(PochiDailyStatusConflictException.class, () -> service.create(request));
        assertThrows(PochiDailyStatusConflictException.class, () -> service.create(request));
        verify(dailyStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatesStatusPreservingCreationFieldsAndSetsUpdateAudit() {
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
        PochiDailyStatus entity = PochiDailyStatus.builder().id(STATUS_ID).pochi(pochi)
                .status(PochiDailyStatusType.DAY_OFF).statusDate(DATE).createdBy(user).createdAt(created).build();
        PochiDailyStatusUpdateRequest request = updateRequest(PochiDailyStatusType.NOT_WORKED, "  Workshop  ");
        when(dailyStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(entity));
        when(dailyStatusRepository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(PochiDailyStatusResponse.builder().build());

        service.update(STATUS_ID, request);

        verify(mapper).updateEntity(request, entity);
        assertThat(entity.getCreatedBy()).isSameAs(user);
        assertThat(entity.getCreatedAt()).isEqualTo(created);
        assertThat(entity.getUpdatedBy()).isSameAs(user);
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getComment()).isEqualTo("Workshop");
        verify(assignmentRepository).existsByPochiIdAndAssignmentDate(POCHI_ID, DATE);
    }

    @Test
    void updateRejectsBlankCommentConflictingAssignmentAndDuplicateDate() {
        PochiDailyStatus entity = PochiDailyStatus.builder().id(STATUS_ID).pochi(pochi).build();
        when(dailyStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(entity));
        assertThrows(InvalidPochiDailyStatusException.class,
                () -> service.update(STATUS_ID, updateRequest(PochiDailyStatusType.NOT_WORKED, "")));
        when(assignmentRepository.existsByPochiIdAndAssignmentDate(POCHI_ID, DATE)).thenReturn(true);
        assertThrows(PochiDailyStatusConflictException.class,
                () -> service.update(STATUS_ID, updateRequest(PochiDailyStatusType.UNION_REST, null)));
        when(dailyStatusRepository.existsByPochiIdAndStatusDateAndIdNot(POCHI_ID, DATE, STATUS_ID))
                .thenReturn(true);
        assertThrows(PochiDailyStatusConflictException.class,
                () -> service.update(STATUS_ID, updateRequest(PochiDailyStatusType.DAY_OFF, null)));
        verify(dailyStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    void handlesLookupDeleteAndMissingUser() {
        PochiDailyStatus entity = PochiDailyStatus.builder().id(STATUS_ID).pochi(pochi).build();
        PochiDailyStatusResponse response = PochiDailyStatusResponse.builder().id(STATUS_ID).build();
        when(dailyStatusRepository.findById(STATUS_ID)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);
        assertThat(service.findById(STATUS_ID)).isSameAs(response);
        service.delete(STATUS_ID);
        verify(dailyStatusRepository).delete(entity);
        assertThrows(PochiDailyStatusNotFoundException.class, () -> service.findById(99));
        when(userRepository.findById(2)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> service.create(request(PochiDailyStatusType.DAY_OFF, null)));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void filtersAreExecutedAsInclusiveDatabasePredicates() {
        PageRequest pageable = PageRequest.of(0, 20);
        PochiDailyStatus entity = PochiDailyStatus.builder().id(STATUS_ID).build();
        when(dailyStatusRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toResponse(entity)).thenReturn(PochiDailyStatusResponse.builder().id(STATUS_ID).build());
        LocalDate end = DATE.plusDays(6);

        Page<PochiDailyStatusResponse> result = service.findAll(
                POCHI_ID, PochiDailyStatusType.NOT_WORKED, null, DATE, end, pageable);
        assertThat(result.getContent()).extracting(PochiDailyStatusResponse::getId).containsExactly(STATUS_ID);
        ArgumentCaptor<Specification<PochiDailyStatus>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(dailyStatusRepository).findAll(captor.capture(), eq(pageable));
        Root<PochiDailyStatus> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path pochiPath = mock(Path.class);
        Path idPath = mock(Path.class);
        Path statusPath = mock(Path.class);
        Path datePath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get("pochi")).thenReturn(pochiPath);
        when(pochiPath.get("id")).thenReturn(idPath);
        when(root.get("status")).thenReturn(statusPath);
        when(root.get("statusDate")).thenReturn(datePath);
        when(builder.conjunction()).thenReturn(predicate);
        when(builder.equal(idPath, POCHI_ID)).thenReturn(predicate);
        when(builder.equal(statusPath, PochiDailyStatusType.NOT_WORKED)).thenReturn(predicate);
        when(builder.greaterThanOrEqualTo(any(), any(LocalDate.class))).thenReturn(predicate);
        when(builder.lessThanOrEqualTo(any(), any(LocalDate.class))).thenReturn(predicate);
        when(builder.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        captor.getValue().toPredicate(root, query, builder);

        verify(builder).equal(idPath, POCHI_ID);
        verify(builder).equal(statusPath, PochiDailyStatusType.NOT_WORKED);
        verify(builder).greaterThanOrEqualTo(datePath, DATE);
        verify(builder).lessThanOrEqualTo(datePath, end);
        assertThrows(InvalidPochiDailyStatusException.class,
                () -> service.findAll(null, null, null, end, DATE, pageable));
        assertThrows(InvalidPochiDailyStatusException.class,
                () -> service.findAll(null, null, DATE, DATE, null, pageable));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void filtersByExactStatusDateInTheDatabaseQuery() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(dailyStatusRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));
        service.findAll(null, null, DATE, null, null, pageable);
        ArgumentCaptor<Specification<PochiDailyStatus>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(dailyStatusRepository).findAll(captor.capture(), eq(pageable));
        Root<PochiDailyStatus> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path datePath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get("statusDate")).thenReturn(datePath);
        when(builder.conjunction()).thenReturn(predicate);
        when(builder.equal(datePath, DATE)).thenReturn(predicate);
        when(builder.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        captor.getValue().toPredicate(root, query, builder);
        verify(builder).equal(datePath, DATE);
    }

    private PochiDailyStatusCreateRequest request(PochiDailyStatusType status, String comment) {
        PochiDailyStatusCreateRequest request = new PochiDailyStatusCreateRequest();
        request.setPochiId(POCHI_ID);
        request.setStatusDate(DATE);
        request.setStatus(status);
        request.setComment(comment);
        return request;
    }

    private PochiDailyStatusUpdateRequest updateRequest(PochiDailyStatusType status, String comment) {
        PochiDailyStatusUpdateRequest request = new PochiDailyStatusUpdateRequest();
        request.setStatusDate(DATE);
        request.setStatus(status);
        request.setComment(comment);
        return request;
    }
}
