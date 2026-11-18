package com.administrapochi.services;

import com.administrapochi.exceptions.InvalidWeeklyClosingException;
import com.administrapochi.exceptions.UserNotFoundException;
import com.administrapochi.exceptions.WeeklyClosingConflictException;
import com.administrapochi.exceptions.WeeklyClosingIncompleteException;
import com.administrapochi.exceptions.WeeklyClosingNotFoundException;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingCommentUpdateRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingCreateRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingReopenRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingResponse;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.entities.WeeklyClosing;
import com.administrapochi.models.entities.WeeklyClosingPochi;
import com.administrapochi.models.entities.WeeklyClosingReopen;
import com.administrapochi.models.enums.WeeklyClosingStatus;
import com.administrapochi.models.mappers.WeeklyClosingMapper;
import com.administrapochi.repositories.PochiAssignmentRepository;
import com.administrapochi.repositories.PochiDailyStatusRepository;
import com.administrapochi.repositories.PochiDayResolution;
import com.administrapochi.repositories.PochiRepository;
import com.administrapochi.repositories.UserRepository;
import com.administrapochi.repositories.WeeklyClosingPochiRepository;
import com.administrapochi.repositories.WeeklyClosingRepository;
import com.administrapochi.repositories.WeeklyClosingReopenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyClosingServiceImplTest {

    @Mock
    private WeeklyClosingRepository weeklyClosingRepository;

    @Mock
    private WeeklyClosingPochiRepository weeklyClosingPochiRepository;

    @Mock
    private WeeklyClosingReopenRepository weeklyClosingReopenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PochiRepository pochiRepository;

    @Mock
    private PochiAssignmentRepository assignmentRepository;

    @Mock
    private PochiDailyStatusRepository dailyStatusRepository;

    @Mock
    private WeeklyClosingMapper mapper;

    @InjectMocks
    private WeeklyClosingServiceImpl weeklyClosingService;

    @Test
    void createStoresOpenClosingAndPochiSnapshotForCalculatedIsoWeek() {
        WeeklyClosingCreateRequest request = request(2026, 1);
        User owner = User.builder().id(2).build();
        List<Pochi> pochis = List.of(Pochi.builder().id(10).build(), Pochi.builder().id(12).build());
        WeeklyClosing closing = new WeeklyClosing();
        WeeklyClosing savedClosing = WeeklyClosing.builder()
                .id(5)
                .weekStart(LocalDate.of(2025, 12, 29))
                .weekEnd(LocalDate.of(2026, 1, 4))
                .build();
        List<WeeklyClosingPochi> snapshots = List.of(
                WeeklyClosingPochi.builder().weeklyClosing(savedClosing).pochi(pochis.get(0)).build(),
                WeeklyClosingPochi.builder().weeklyClosing(savedClosing).pochi(pochis.get(1)).build());
        WeeklyClosingResponse response = WeeklyClosingResponse.builder().id(5).build();
        when(userRepository.findById(2)).thenReturn(Optional.of(owner));
        when(weeklyClosingRepository.existsByOwner_IdAndWeekYearAndWeekNumber(2, 2026, 1)).thenReturn(false);
        when(pochiRepository.findAllByOwner_IdOrderByIdAsc(2)).thenReturn(pochis);
        when(mapper.toEntity(request)).thenReturn(closing);
        when(weeklyClosingRepository.save(closing)).thenReturn(savedClosing);
        when(weeklyClosingPochiRepository.saveAll(anyList())).thenReturn(snapshots);
        when(mapper.toResponse(savedClosing)).thenReturn(response);

        WeeklyClosingResponse result = weeklyClosingService.create(request);

        assertEquals(5, result.getId());
        assertAll(
                () -> assertSame(owner, closing.getOwner()),
                () -> assertEquals(LocalDate.of(2025, 12, 29), closing.getWeekStart()),
                () -> assertEquals(LocalDate.of(2026, 1, 4), closing.getWeekEnd()),
                () -> assertEquals(WeeklyClosingStatus.OPEN, closing.getStatus()),
                () -> assertEquals(snapshots, savedClosing.getPochiSnapshots()),
                () -> assertEquals(pochis, snapshots.stream().map(WeeklyClosingPochi::getPochi).toList()));
        var ordered = inOrder(weeklyClosingRepository, weeklyClosingPochiRepository, mapper);
        ordered.verify(weeklyClosingRepository).save(closing);
        ordered.verify(weeklyClosingPochiRepository).saveAll(argThat(savedSnapshots -> {
            var iterator = savedSnapshots.iterator();
            if (!iterator.hasNext()) {
                return false;
            }
            WeeklyClosingPochi first = iterator.next();
            if (!iterator.hasNext()) {
                return false;
            }
            WeeklyClosingPochi second = iterator.next();
            return !iterator.hasNext()
                    && first.getWeeklyClosing() == savedClosing
                    && first.getPochi() == pochis.get(0)
                    && second.getPochi() == pochis.get(1);
        }));
        ordered.verify(mapper).toResponse(savedClosing);
    }

    @Test
    void createCalculatesWeek53WhenItExists() {
        WeeklyClosingCreateRequest request = request(2020, 53);
        configureSuccessfulCreate(request, 2020, 53, LocalDate.of(2020, 12, 28), LocalDate.of(2021, 1, 3));

        weeklyClosingService.create(request);

        verify(weeklyClosingRepository).save(org.mockito.ArgumentMatchers.argThat(closing ->
                closing.getWeekStart().equals(LocalDate.of(2020, 12, 28))
                        && closing.getWeekEnd().equals(LocalDate.of(2021, 1, 3))));
    }

    @Test
    void createRejectsWeek53WhenItDoesNotExist() {
        WeeklyClosingCreateRequest request = request(2021, 53);

        assertThrows(InvalidWeeklyClosingException.class, () -> weeklyClosingService.create(request));

        verifyNoInteractions(userRepository, weeklyClosingRepository, pochiRepository,
                weeklyClosingPochiRepository, mapper);
    }

    @Test
    void createRejectsDuplicateOwnerAndWeek() {
        WeeklyClosingCreateRequest request = request(2026, 47);
        when(userRepository.findById(2)).thenReturn(Optional.of(User.builder().id(2).build()));
        when(weeklyClosingRepository.existsByOwner_IdAndWeekYearAndWeekNumber(2, 2026, 47))
                .thenReturn(true);

        assertThrows(WeeklyClosingConflictException.class, () -> weeklyClosingService.create(request));

        verify(pochiRepository, never()).findAllByOwner_IdOrderByIdAsc(2);
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(weeklyClosingPochiRepository, mapper);
    }

    @Test
    void createRejectsWhenOwnerDoesNotExist() {
        WeeklyClosingCreateRequest request = request(2026, 47);
        when(userRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> weeklyClosingService.create(request));

        verify(weeklyClosingRepository, never())
                .existsByOwner_IdAndWeekYearAndWeekNumber(2, 2026, 47);
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(pochiRepository, weeklyClosingPochiRepository, mapper);
    }

    @Test
    void createRejectsOwnerWithoutPochis() {
        WeeklyClosingCreateRequest request = request(2026, 47);
        when(userRepository.findById(2)).thenReturn(Optional.of(User.builder().id(2).build()));
        when(weeklyClosingRepository.existsByOwner_IdAndWeekYearAndWeekNumber(2, 2026, 47))
                .thenReturn(false);
        when(pochiRepository.findAllByOwner_IdOrderByIdAsc(2)).thenReturn(List.of());

        assertThrows(WeeklyClosingConflictException.class, () -> weeklyClosingService.create(request));

        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(weeklyClosingPochiRepository, mapper);
    }

    @Test
    void findByIdReturnsEveryUnresolvedPochiDateAndCountsEitherResolutionSource() {
        LocalDate weekStart = LocalDate.of(2026, 11, 16);
        LocalDate weekEnd = weekStart.plusDays(6);
        WeeklyClosing closing = WeeklyClosing.builder()
                .id(31)
                .owner(User.builder().id(2).name("Owner").build())
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .build();
        List<WeeklyClosingPochi> snapshots = List.of(
                WeeklyClosingPochi.builder().pochi(Pochi.builder().id(10).build()).build(),
                WeeklyClosingPochi.builder().pochi(Pochi.builder().id(12).build()).build());
        when(weeklyClosingRepository.findDetailsById(31)).thenReturn(Optional.of(closing));
        when(weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(31))
                .thenReturn(snapshots);
        when(assignmentRepository.findResolvedPochiDays(List.of(10, 12), weekStart, weekEnd))
                .thenReturn(List.of(resolution(10, weekStart)));
        when(dailyStatusRepository.findResolvedPochiDays(List.of(10, 12), weekStart, weekEnd))
                .thenReturn(List.of(resolution(10, weekStart), resolution(12, weekStart.plusDays(2))));
        when(mapper.toResponse(closing)).thenReturn(WeeklyClosingResponse.builder().id(31).build());
        when(weeklyClosingReopenRepository.findAllByWeeklyClosing_IdOrderByReopenedAtAscIdAsc(31))
                .thenReturn(List.of());

        WeeklyClosingResponse response = weeklyClosingService.findById(31);

        Set<String> missingPochiDates = response.getUnresolvedDays().stream()
                .map(day -> day.getPochiId() + ":" + day.getDate())
                .collect(Collectors.toSet());
        assertAll(
                () -> assertEquals(12, response.getUnresolvedDays().size()),
                () -> assertEquals(2, response.getResolvedDayCount()),
                () -> assertEquals(14, response.getTotalDayCount()),
                () -> assertEquals(Boolean.FALSE, response.getComplete()),
                () -> assertEquals(weekStart.plusDays(1), response.getUnresolvedDays().getFirst().getDate()),
                () -> assertEquals(Set.of(
                        "10:2026-11-17", "10:2026-11-18", "10:2026-11-19", "10:2026-11-20",
                        "10:2026-11-21", "10:2026-11-22",
                        "12:2026-11-16", "12:2026-11-17", "12:2026-11-19",
                        "12:2026-11-20", "12:2026-11-21", "12:2026-11-22"),
                        missingPochiDates));
        verify(assignmentRepository).findResolvedPochiDays(List.of(10, 12), weekStart, weekEnd);
        verify(dailyStatusRepository).findResolvedPochiDays(List.of(10, 12), weekStart, weekEnd);
    }

    @Test
    void findAllReturnsFilteredClosingsWithCompleteness() {
        LocalDate weekStart = LocalDate.of(2026, 11, 16);
        WeeklyClosing closing = WeeklyClosing.builder()
                .id(8)
                .owner(User.builder().id(2).name("Owner").build())
                .weekStart(weekStart)
                .weekEnd(weekStart.plusDays(6))
                .build();
        when(weeklyClosingRepository.findAllFiltered(2, 2026, 47, WeeklyClosingStatus.OPEN))
                .thenReturn(List.of(closing));
        when(weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(8))
                .thenReturn(List.of());
        when(mapper.toResponse(closing)).thenReturn(WeeklyClosingResponse.builder().id(8).build());

        List<WeeklyClosingResponse> result = weeklyClosingService.findAll(2, 2026, 47, WeeklyClosingStatus.OPEN);

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(8, result.getFirst().getId()),
                () -> assertEquals(0, result.getFirst().getTotalDayCount()),
                () -> assertEquals(Boolean.FALSE, result.getFirst().getComplete()));
    }

    @Test
    void findByIdThrowsWhenClosingDoesNotExist() {
        when(weeklyClosingRepository.findDetailsById(404)).thenReturn(Optional.empty());

        assertThrows(com.administrapochi.exceptions.WeeklyClosingNotFoundException.class,
                () -> weeklyClosingService.findById(404));

        verifyNoInteractions(weeklyClosingPochiRepository, assignmentRepository, dailyStatusRepository, mapper);
    }

    @Test
    void closeByAdminClosesCompletePastWeekWithInternalActorAndTimestamp() {
        LocalDate weekStart = pastWeekStart();
        WeeklyClosing closing = openClosing(40, 3, weekStart);
        List<WeeklyClosingPochi> snapshots = List.of(
                WeeklyClosingPochi.builder().pochi(Pochi.builder().id(10).build()).build());
        User actor = User.builder().id(2).build();
        when(weeklyClosingRepository.findDetailsByIdForUpdate(40)).thenReturn(Optional.of(closing));
        when(weeklyClosingRepository
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusOrderByWeekStartAscIdAsc(
                        3, weekStart, WeeklyClosingStatus.OPEN))
                .thenReturn(Optional.empty());
        when(weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(40))
                .thenReturn(snapshots);
        when(assignmentRepository.findResolvedPochiDays(List.of(10), weekStart, weekStart.plusDays(6)))
                .thenReturn(java.util.stream.IntStream.range(0, 7)
                        .mapToObj(day -> resolution(10, weekStart.plusDays(day)))
                        .toList());
        when(dailyStatusRepository.findResolvedPochiDays(List.of(10), weekStart, weekStart.plusDays(6)))
                .thenReturn(List.of());
        when(userRepository.findById(2)).thenReturn(Optional.of(actor));
        when(mapper.toResponse(closing)).thenReturn(WeeklyClosingResponse.builder().id(40).build());
        when(weeklyClosingRepository.save(closing)).thenReturn(closing);

        WeeklyClosingResponse response = weeklyClosingService.closeByAdmin(40);

        assertAll(
                () -> assertEquals(WeeklyClosingStatus.ADMIN_CLOSED, closing.getStatus()),
                () -> assertSame(actor, closing.getAdminClosedBy()),
                () -> assertEquals(2, response.getAdminClosedById()),
                () -> assertEquals(WeeklyClosingStatus.ADMIN_CLOSED, response.getStatus()),
                () -> assertEquals(7, response.getResolvedDayCount()),
                () -> assertEquals(7, response.getTotalDayCount()),
                () -> assertEquals(Boolean.TRUE, response.getComplete()),
                () -> org.junit.jupiter.api.Assertions.assertNotNull(closing.getAdminClosedAt()),
                () -> assertEquals(closing.getAdminClosedAt(), response.getAdminClosedAt()));
        verify(weeklyClosingRepository).save(closing);
        verify(userRepository).findById(2);
    }

    @Test
    void closeByAdminRejectsWhenWeekHasNotEnded() {
        LocalDate currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        WeeklyClosing closing = openClosing(41, 3, currentWeekStart);
        when(weeklyClosingRepository.findDetailsByIdForUpdate(41)).thenReturn(Optional.of(closing));

        assertThrows(WeeklyClosingConflictException.class, () -> weeklyClosingService.closeByAdmin(41));

        verify(weeklyClosingRepository, never())
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusOrderByWeekStartAscIdAsc(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(userRepository, weeklyClosingPochiRepository, assignmentRepository,
                dailyStatusRepository, mapper);
    }

    @Test
    void closeByAdminRejectsNonOpenClosing() {
        WeeklyClosing closing = openClosing(42, 3, pastWeekStart());
        closing.setStatus(WeeklyClosingStatus.ADMIN_CLOSED);
        when(weeklyClosingRepository.findDetailsByIdForUpdate(42)).thenReturn(Optional.of(closing));

        assertThrows(WeeklyClosingConflictException.class, () -> weeklyClosingService.closeByAdmin(42));

        verify(weeklyClosingRepository, never())
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusOrderByWeekStartAscIdAsc(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(userRepository, weeklyClosingPochiRepository, assignmentRepository,
                dailyStatusRepository, mapper);
    }

    @Test
    void closeByAdminRejectsIncompleteWeekWithEveryMissingPochiDay() {
        LocalDate weekStart = pastWeekStart();
        WeeklyClosing closing = openClosing(43, 3, weekStart);
        List<WeeklyClosingPochi> snapshots = List.of(
                WeeklyClosingPochi.builder().pochi(Pochi.builder().id(10).build()).build(),
                WeeklyClosingPochi.builder().pochi(Pochi.builder().id(12).build()).build());
        when(weeklyClosingRepository.findDetailsByIdForUpdate(43)).thenReturn(Optional.of(closing));
        when(weeklyClosingRepository
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusOrderByWeekStartAscIdAsc(
                        3, weekStart, WeeklyClosingStatus.OPEN))
                .thenReturn(Optional.empty());
        when(weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(43))
                .thenReturn(snapshots);
        when(assignmentRepository.findResolvedPochiDays(List.of(10, 12), weekStart, weekStart.plusDays(6)))
                .thenReturn(List.of(resolution(10, weekStart)));
        when(dailyStatusRepository.findResolvedPochiDays(List.of(10, 12), weekStart, weekStart.plusDays(6)))
                .thenReturn(List.of(resolution(12, weekStart.plusDays(1))));
        when(mapper.toResponse(closing)).thenReturn(WeeklyClosingResponse.builder().id(43).build());

        WeeklyClosingIncompleteException exception = assertThrows(
                WeeklyClosingIncompleteException.class, () -> weeklyClosingService.closeByAdmin(43));

        assertEquals(12, exception.getUnresolvedDays().size());
        assertEquals(Set.of(
                "10:" + weekStart.plusDays(1), "10:" + weekStart.plusDays(2),
                "10:" + weekStart.plusDays(3), "10:" + weekStart.plusDays(4),
                "10:" + weekStart.plusDays(5), "10:" + weekStart.plusDays(6),
                "12:" + weekStart, "12:" + weekStart.plusDays(2),
                "12:" + weekStart.plusDays(3), "12:" + weekStart.plusDays(4),
                "12:" + weekStart.plusDays(5), "12:" + weekStart.plusDays(6)),
                exception.getUnresolvedDays().stream()
                        .map(day -> day.getPochiId() + ":" + day.getDate())
                        .collect(Collectors.toSet()));
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void closeByAdminRejectsSkippingEarlierOpenWeek() {
        LocalDate weekStart = pastWeekStart();
        WeeklyClosing closing = openClosing(44, 3, weekStart);
        WeeklyClosing earlierClosing = openClosing(45, 3, weekStart.minusWeeks(1));
        when(weeklyClosingRepository.findDetailsByIdForUpdate(44)).thenReturn(Optional.of(closing));
        when(weeklyClosingRepository
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusOrderByWeekStartAscIdAsc(
                        3, weekStart, WeeklyClosingStatus.OPEN))
                .thenReturn(Optional.of(earlierClosing));

        WeeklyClosingConflictException exception = assertThrows(
                WeeklyClosingConflictException.class, () -> weeklyClosingService.closeByAdmin(44));

        assertEquals("Earlier week " + earlierClosing.getWeekNumber() + "/"
                + earlierClosing.getWeekYear() + " must be Admin-closed first", exception.getMessage());
        verify(weeklyClosingPochiRepository, never()).findAllByWeeklyClosing_IdOrderByPochi_IdAsc(44);
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(userRepository, assignmentRepository, dailyStatusRepository, mapper);
    }

    @Test
    void closeByAdminRejectsClosingWithNoPochiSnapshots() {
        LocalDate weekStart = pastWeekStart();
        WeeklyClosing closing = openClosing(46, 3, weekStart);
        when(weeklyClosingRepository.findDetailsByIdForUpdate(46)).thenReturn(Optional.of(closing));
        when(weeklyClosingRepository
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusOrderByWeekStartAscIdAsc(
                        3, weekStart, WeeklyClosingStatus.OPEN))
                .thenReturn(Optional.empty());
        when(weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(46))
                .thenReturn(List.of());

        assertThrows(WeeklyClosingConflictException.class, () -> weeklyClosingService.closeByAdmin(46));

        verifyNoInteractions(userRepository, assignmentRepository, dailyStatusRepository, mapper);
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void closeByOwnerClosesEarliestPendingWeekWithInternalActorAndTimestamp() {
        LocalDate weekStart = pastWeekStart();
        WeeklyClosing closing = openClosing(55, 3, weekStart);
        closing.setStatus(WeeklyClosingStatus.ADMIN_CLOSED);
        closing.setClosingComment("Accepted");
        User actor = User.builder().id(2).build();
        List<WeeklyClosingPochi> snapshots = List.of(
                WeeklyClosingPochi.builder().pochi(Pochi.builder().id(20).build()).build());
        when(weeklyClosingRepository.findDetailsByIdForUpdate(55)).thenReturn(Optional.of(closing));
        when(weeklyClosingRepository
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusInOrderByWeekStartAscIdAsc(
                        3, weekStart, List.of(WeeklyClosingStatus.OPEN, WeeklyClosingStatus.ADMIN_CLOSED)))
                .thenReturn(Optional.empty());
        when(userRepository.findById(2)).thenReturn(Optional.of(actor));
        when(weeklyClosingRepository.save(closing)).thenReturn(closing);
        when(weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(55))
                .thenReturn(snapshots);
        when(assignmentRepository.findResolvedPochiDays(List.of(20), weekStart, weekStart.plusDays(6)))
                .thenReturn(java.util.stream.IntStream.range(0, 7)
                        .mapToObj(day -> resolution(20, weekStart.plusDays(day)))
                        .toList());
        when(dailyStatusRepository.findResolvedPochiDays(List.of(20), weekStart, weekStart.plusDays(6)))
                .thenReturn(List.of());
        when(weeklyClosingReopenRepository.findAllByWeeklyClosing_IdOrderByReopenedAtAscIdAsc(55))
                .thenReturn(List.of());
        when(mapper.toResponse(closing)).thenReturn(WeeklyClosingResponse.builder()
                .id(55)
                .closingComment("Accepted")
                .status(WeeklyClosingStatus.OWNER_CLOSED)
                .build());

        WeeklyClosingResponse response = weeklyClosingService.closeByOwner(55);

        assertAll(
                () -> assertEquals(WeeklyClosingStatus.OWNER_CLOSED, closing.getStatus()),
                () -> assertSame(actor, closing.getOwnerClosedBy()),
                () -> assertEquals(2, response.getOwnerClosedById()),
                () -> assertEquals(WeeklyClosingStatus.OWNER_CLOSED, response.getStatus()),
                () -> assertEquals("Accepted", response.getClosingComment()),
                () -> org.junit.jupiter.api.Assertions.assertNotNull(closing.getOwnerClosedAt()),
                () -> assertEquals(closing.getOwnerClosedAt(), response.getOwnerClosedAt()));
        verify(weeklyClosingRepository).save(closing);
        verify(userRepository).findById(2);
    }

    @Test
    void closeByOwnerRejectsOpenClosing() {
        WeeklyClosing closing = openClosing(56, 3, pastWeekStart());
        when(weeklyClosingRepository.findDetailsByIdForUpdate(56)).thenReturn(Optional.of(closing));

        assertThrows(WeeklyClosingConflictException.class, () -> weeklyClosingService.closeByOwner(56));

        verify(weeklyClosingRepository, never())
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusInOrderByWeekStartAscIdAsc(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyList());
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(userRepository, weeklyClosingPochiRepository, assignmentRepository,
                dailyStatusRepository, weeklyClosingReopenRepository, mapper);
    }

    @Test
    void closeByOwnerRejectsOwnerClosedTerminalStatus() {
        WeeklyClosing closing = openClosing(57, 3, pastWeekStart());
        closing.setStatus(WeeklyClosingStatus.OWNER_CLOSED);
        when(weeklyClosingRepository.findDetailsByIdForUpdate(57)).thenReturn(Optional.of(closing));

        assertThrows(WeeklyClosingConflictException.class, () -> weeklyClosingService.closeByOwner(57));

        verify(weeklyClosingRepository, never())
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusInOrderByWeekStartAscIdAsc(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyList());
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(userRepository, weeklyClosingPochiRepository, assignmentRepository,
                dailyStatusRepository, weeklyClosingReopenRepository, mapper);
    }

    @Test
    void closeByOwnerRejectsWhenEarlierAdminClosedWeekIsPending() {
        LocalDate weekStart = pastWeekStart();
        WeeklyClosing closing = openClosing(58, 3, weekStart);
        closing.setStatus(WeeklyClosingStatus.ADMIN_CLOSED);
        WeeklyClosing earlierClosing = openClosing(59, 3, weekStart.minusWeeks(1));
        earlierClosing.setStatus(WeeklyClosingStatus.ADMIN_CLOSED);
        when(weeklyClosingRepository.findDetailsByIdForUpdate(58)).thenReturn(Optional.of(closing));
        when(weeklyClosingRepository
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusInOrderByWeekStartAscIdAsc(
                        3, weekStart, List.of(WeeklyClosingStatus.OPEN, WeeklyClosingStatus.ADMIN_CLOSED)))
                .thenReturn(Optional.of(earlierClosing));

        WeeklyClosingConflictException exception = assertThrows(
                WeeklyClosingConflictException.class, () -> weeklyClosingService.closeByOwner(58));

        assertEquals("Earlier week " + earlierClosing.getWeekNumber() + "/"
                + earlierClosing.getWeekYear() + " must be Owner-closed first", exception.getMessage());
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(userRepository, weeklyClosingPochiRepository, assignmentRepository,
                dailyStatusRepository, weeklyClosingReopenRepository, mapper);
    }

    @Test
    void closeByOwnerRejectsWhenEarlierWeekIsStillOpen() {
        LocalDate weekStart = pastWeekStart();
        WeeklyClosing closing = openClosing(60, 3, weekStart);
        closing.setStatus(WeeklyClosingStatus.ADMIN_CLOSED);
        WeeklyClosing earlierClosing = openClosing(61, 3, weekStart.minusWeeks(1));
        when(weeklyClosingRepository.findDetailsByIdForUpdate(60)).thenReturn(Optional.of(closing));
        when(weeklyClosingRepository
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusInOrderByWeekStartAscIdAsc(
                        3, weekStart, List.of(WeeklyClosingStatus.OPEN, WeeklyClosingStatus.ADMIN_CLOSED)))
                .thenReturn(Optional.of(earlierClosing));

        WeeklyClosingConflictException exception = assertThrows(
                WeeklyClosingConflictException.class, () -> weeklyClosingService.closeByOwner(60));

        assertEquals("Earlier week " + earlierClosing.getWeekNumber() + "/"
                + earlierClosing.getWeekYear() + " must be Admin-closed before Owner closing",
                exception.getMessage());
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(userRepository, weeklyClosingPochiRepository, assignmentRepository,
                dailyStatusRepository, weeklyClosingReopenRepository, mapper);
    }

    @Test
    void reopenAdminClosedClosingStoresActorReasonAndEveryHistoricalReopen() {
        LocalDate weekStart = pastWeekStart();
        WeeklyClosing closing = openClosing(47, 3, weekStart);
        closing.setStatus(WeeklyClosingStatus.ADMIN_CLOSED);
        User adminCloser = User.builder().id(9).name("Admin").build();
        closing.setAdminClosedBy(adminCloser);
        closing.setAdminClosedAt(java.time.Instant.parse("2026-01-01T03:04:05Z"));
        User actor = User.builder().id(2).name("Temporary user").build();
        WeeklyClosingReopen previousReopen = WeeklyClosingReopen.builder()
                .id(11)
                .weeklyClosing(closing)
                .reopenedBy(actor)
                .reopenedAt(java.time.Instant.parse("2026-01-02T03:04:05Z"))
                .reason("Earlier correction")
                .build();
        WeeklyClosingReopen currentReopen = WeeklyClosingReopen.builder()
                .id(12)
                .weeklyClosing(closing)
                .reopenedBy(actor)
                .reopenedAt(java.time.Instant.parse("2026-01-03T03:04:05Z"))
                .reason("Updated settlement")
                .build();
        WeeklyClosingReopenRequest request = reopenRequest("  Updated settlement  ");
        when(weeklyClosingRepository.findDetailsByIdForUpdate(47)).thenReturn(Optional.of(closing));
        when(userRepository.findById(2)).thenReturn(Optional.of(actor));
        when(weeklyClosingReopenRepository.save(org.mockito.ArgumentMatchers.any(WeeklyClosingReopen.class)))
                .thenReturn(currentReopen);
        when(weeklyClosingRepository.save(closing)).thenReturn(closing);
        when(weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(47))
                .thenReturn(List.of());
        when(mapper.toResponse(closing)).thenReturn(WeeklyClosingResponse.builder().id(47).build());
        when(weeklyClosingReopenRepository.findAllByWeeklyClosing_IdOrderByReopenedAtAscIdAsc(47))
                .thenReturn(List.of(previousReopen, currentReopen));
        when(mapper.toReopenResponse(previousReopen)).thenReturn(
                com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingReopenResponse.builder()
                        .id(11).reason("Earlier correction").build());
        when(mapper.toReopenResponse(currentReopen)).thenReturn(
                com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingReopenResponse.builder()
                        .id(12).reason("Updated settlement").build());

        WeeklyClosingResponse response = weeklyClosingService.reopen(47, request);

        assertAll(
                () -> assertEquals(WeeklyClosingStatus.OPEN, closing.getStatus()),
                () -> assertSame(adminCloser, closing.getAdminClosedBy()),
                () -> assertEquals(java.time.Instant.parse("2026-01-01T03:04:05Z"), closing.getAdminClosedAt()),
                () -> assertEquals(2, response.getReopens().size()),
                () -> assertEquals(List.of(11, 12),
                        response.getReopens().stream()
                                .map(com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingReopenResponse::getId)
                                .toList()),
                () -> assertEquals("Updated settlement",
                        response.getReopens().getLast().getReason()));
        var ordered = inOrder(weeklyClosingReopenRepository, weeklyClosingRepository);
        ordered.verify(weeklyClosingReopenRepository).save(argThat(reopen ->
                reopen.getWeeklyClosing() == closing
                        && reopen.getReopenedBy() == actor
                        && reopen.getReason().equals("Updated settlement")
                        && reopen.getReopenedAt() != null));
        ordered.verify(weeklyClosingRepository).save(closing);
    }

    @Test
    void reopenRejectsBlankReasonWithoutChangingClosingOrWritingHistory() {
        WeeklyClosingReopenRequest request = reopenRequest("   ");

        assertThrows(InvalidWeeklyClosingException.class, () -> weeklyClosingService.reopen(48, request));

        verifyNoInteractions(weeklyClosingRepository, weeklyClosingReopenRepository, userRepository);
    }

    @Test
    void reopenRejectsOpenOrOwnerClosedStatus() {
        for (WeeklyClosingStatus status : List.of(WeeklyClosingStatus.OPEN, WeeklyClosingStatus.OWNER_CLOSED)) {
            WeeklyClosing closing = openClosing(49, 3, pastWeekStart());
            closing.setStatus(status);
            when(weeklyClosingRepository.findDetailsByIdForUpdate(49)).thenReturn(Optional.of(closing));

            assertThrows(WeeklyClosingConflictException.class,
                    () -> weeklyClosingService.reopen(49, reopenRequest("Valid reason")));

            verify(weeklyClosingReopenRepository, never())
                    .save(org.mockito.ArgumentMatchers.any(WeeklyClosingReopen.class));
            verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
            verifyNoInteractions(userRepository);
        }
    }

    @Test
    void reopenFailsWhenTemporaryActorDoesNotExist() {
        WeeklyClosing closing = openClosing(50, 3, pastWeekStart());
        closing.setStatus(WeeklyClosingStatus.ADMIN_CLOSED);
        when(weeklyClosingRepository.findDetailsByIdForUpdate(50)).thenReturn(Optional.of(closing));
        when(userRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> weeklyClosingService.reopen(50, reopenRequest("Valid reason")));

        verifyNoInteractions(weeklyClosingReopenRepository);
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateCommentAllowsOpenClosingAndClearsCommentWhenNullIsProvided() {
        WeeklyClosing closing = openClosing(51, 3, pastWeekStart());
        closing.setClosingComment("Old comment");
        WeeklyClosingCommentUpdateRequest request = commentRequest(null);
        configureCommentUpdate(closing);

        WeeklyClosingResponse response = weeklyClosingService.updateComment(51, request);

        assertNull(closing.getClosingComment());
        assertNull(response.getClosingComment());
        verify(weeklyClosingRepository).save(closing);
    }

    @Test
    void updateCommentAllowsAdminClosedClosing() {
        WeeklyClosing closing = openClosing(52, 3, pastWeekStart());
        closing.setStatus(WeeklyClosingStatus.ADMIN_CLOSED);
        WeeklyClosingCommentUpdateRequest request = commentRequest("Reserved for repairs");
        configureCommentUpdate(closing);

        WeeklyClosingResponse response = weeklyClosingService.updateComment(52, request);

        assertEquals("Reserved for repairs", closing.getClosingComment());
        assertEquals("Reserved for repairs", response.getClosingComment());
        assertEquals(WeeklyClosingStatus.ADMIN_CLOSED, response.getStatus());
        verify(weeklyClosingRepository).save(closing);
    }

    @Test
    void updateCommentRejectsOwnerClosedClosingWithoutSaving() {
        WeeklyClosing closing = openClosing(53, 3, pastWeekStart());
        closing.setStatus(WeeklyClosingStatus.OWNER_CLOSED);
        closing.setClosingComment("Accepted comment");
        when(weeklyClosingRepository.findDetailsByIdForUpdate(53)).thenReturn(Optional.of(closing));

        assertThrows(WeeklyClosingConflictException.class,
                () -> weeklyClosingService.updateComment(53, commentRequest("Changed comment")));

        assertEquals("Accepted comment", closing.getClosingComment());
        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(weeklyClosingPochiRepository, weeklyClosingReopenRepository, mapper);
    }

    @Test
    void updateCommentThrowsWhenClosingDoesNotExist() {
        when(weeklyClosingRepository.findDetailsByIdForUpdate(54)).thenReturn(Optional.empty());

        assertThrows(WeeklyClosingNotFoundException.class,
                () -> weeklyClosingService.updateComment(54, commentRequest("Comment")));

        verify(weeklyClosingRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(userRepository, weeklyClosingPochiRepository, weeklyClosingReopenRepository,
                assignmentRepository, dailyStatusRepository, mapper);
    }

    private PochiDayResolution resolution(Integer pochiId, LocalDate date) {
        return new PochiDayResolution() {
            @Override
            public Integer getPochiId() {
                return pochiId;
            }

            @Override
            public LocalDate getResolvedDate() {
                return date;
            }
        };
    }

    private WeeklyClosing openClosing(Integer id, Integer ownerId, LocalDate weekStart) {
        return WeeklyClosing.builder()
                .id(id)
                .owner(User.builder().id(ownerId).build())
                .weekYear(weekStart.get(WeekFields.ISO.weekBasedYear()))
                .weekNumber(weekStart.get(WeekFields.ISO.weekOfWeekBasedYear()))
                .weekStart(weekStart)
                .weekEnd(weekStart.plusDays(6))
                .status(WeeklyClosingStatus.OPEN)
                .build();
    }

    private LocalDate pastWeekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(3);
    }

    private WeeklyClosingReopenRequest reopenRequest(String reason) {
        WeeklyClosingReopenRequest request = new WeeklyClosingReopenRequest();
        request.setReason(reason);
        return request;
    }

    private WeeklyClosingCommentUpdateRequest commentRequest(String comment) {
        WeeklyClosingCommentUpdateRequest request = new WeeklyClosingCommentUpdateRequest();
        request.setClosingComment(comment);
        return request;
    }

    private void configureCommentUpdate(WeeklyClosing closing) {
        when(weeklyClosingRepository.findDetailsByIdForUpdate(closing.getId())).thenReturn(Optional.of(closing));
        when(weeklyClosingRepository.save(closing)).thenReturn(closing);
        when(weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(closing.getId()))
                .thenReturn(List.of());
        when(weeklyClosingReopenRepository.findAllByWeeklyClosing_IdOrderByReopenedAtAscIdAsc(closing.getId()))
                .thenReturn(List.of());
        when(mapper.toResponse(closing)).thenAnswer(invocation -> WeeklyClosingResponse.builder()
                .id(closing.getId())
                .closingComment(closing.getClosingComment())
                .status(closing.getStatus())
                .build());
    }

    private void configureSuccessfulCreate(
            WeeklyClosingCreateRequest request,
            int weekYear,
            int weekNumber,
            LocalDate weekStart,
            LocalDate weekEnd) {
        User owner = User.builder().id(2).build();
        Pochi pochi = Pochi.builder().id(10).build();
        WeeklyClosing closing = new WeeklyClosing();
        WeeklyClosing savedClosing = new WeeklyClosing();
        when(userRepository.findById(2)).thenReturn(Optional.of(owner));
        when(weeklyClosingRepository.existsByOwner_IdAndWeekYearAndWeekNumber(2, weekYear, weekNumber))
                .thenReturn(false);
        when(pochiRepository.findAllByOwner_IdOrderByIdAsc(2)).thenReturn(List.of(pochi));
        when(mapper.toEntity(request)).thenReturn(closing);
        when(weeklyClosingRepository.save(closing)).thenAnswer(invocation -> {
            closing.setWeekStart(weekStart);
            closing.setWeekEnd(weekEnd);
            return savedClosing;
        });
        when(weeklyClosingPochiRepository.saveAll(anyList())).thenReturn(List.of());
        when(mapper.toResponse(savedClosing)).thenReturn(WeeklyClosingResponse.builder().build());
    }

    private WeeklyClosingCreateRequest request(int weekYear, int weekNumber) {
        WeeklyClosingCreateRequest request = new WeeklyClosingCreateRequest();
        request.setOwnerId(2);
        request.setWeekYear(weekYear);
        request.setWeekNumber(weekNumber);
        return request;
    }
}
