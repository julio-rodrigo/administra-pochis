package com.administrapochi.services;

import com.administrapochi.exceptions.InvalidWeeklyClosingException;
import com.administrapochi.exceptions.UserNotFoundException;
import com.administrapochi.exceptions.WeeklyClosingConflictException;
import com.administrapochi.exceptions.WeeklyClosingIncompleteException;
import com.administrapochi.exceptions.WeeklyClosingNotFoundException;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingCreateRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingCommentUpdateRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingReopenRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingResponse;
import com.administrapochi.models.dtos.weeklyclosings.UnresolvedPochiDayResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyClosingServiceImpl implements WeeklyClosingService {

    private static final Integer TEMPORARY_USER_ID = 2;

    private final WeeklyClosingRepository weeklyClosingRepository;
    private final WeeklyClosingPochiRepository weeklyClosingPochiRepository;
    private final WeeklyClosingReopenRepository weeklyClosingReopenRepository;
    private final UserRepository userRepository;
    private final PochiRepository pochiRepository;
    private final PochiAssignmentRepository assignmentRepository;
    private final PochiDailyStatusRepository dailyStatusRepository;
    private final WeeklyClosingMapper mapper;

    @Override
    @Transactional
    public WeeklyClosingResponse create(WeeklyClosingCreateRequest request) {
        LocalDate weekStart = IsoWeeks.start(request.getWeekYear(), request.getWeekNumber());
        LocalDate weekEnd = weekStart.plusDays(6);
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new UserNotFoundException(request.getOwnerId()));

        if (weeklyClosingRepository.existsByOwner_IdAndWeekYearAndWeekNumber(
                request.getOwnerId(), request.getWeekYear(), request.getWeekNumber())) {
            throw new WeeklyClosingConflictException(
                    "A weekly closing already exists for owner " + request.getOwnerId()
                            + " and ISO week " + request.getWeekNumber() + "/" + request.getWeekYear());
        }

        List<Pochi> pochis = pochiRepository.findAllByOwner_IdOrderByIdAsc(owner.getId());
        if (pochis.isEmpty()) {
            throw new WeeklyClosingConflictException(
                    "Owner with id " + owner.getId() + " has no Pochis to include in a weekly closing");
        }

        WeeklyClosing weeklyClosing = mapper.toEntity(request);
        weeklyClosing.setOwner(owner);
        weeklyClosing.setWeekStart(weekStart);
        weeklyClosing.setWeekEnd(weekEnd);
        weeklyClosing.setStatus(WeeklyClosingStatus.OPEN);
        WeeklyClosing savedClosing = weeklyClosingRepository.save(weeklyClosing);

        List<WeeklyClosingPochi> snapshots = pochis.stream()
                .map(pochi -> WeeklyClosingPochi.builder()
                        .weeklyClosing(savedClosing)
                        .pochi(pochi)
                        .build())
                .toList();
        savedClosing.setPochiSnapshots(weeklyClosingPochiRepository.saveAll(snapshots));
        return toResponse(savedClosing);
    }

    @Override
    @Transactional
    public WeeklyClosingResponse closeByAdmin(Integer id) {
        WeeklyClosing closing = weeklyClosingRepository.findDetailsByIdForUpdate(id)
                .orElseThrow(() -> new WeeklyClosingNotFoundException(id));
        if (closing.getStatus() != WeeklyClosingStatus.OPEN) {
            throw new WeeklyClosingConflictException(
                    "Only an OPEN weekly closing can be closed by Admin");
        }
        if (!LocalDate.now().isAfter(closing.getWeekEnd())) {
            throw new WeeklyClosingConflictException(
                    "The weekly closing can be closed by Admin starting the Monday after "
                            + closing.getWeekEnd());
        }

        WeeklyClosing earlierOpenClosing = weeklyClosingRepository
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusOrderByWeekStartAscIdAsc(
                        closing.getOwner().getId(), closing.getWeekStart(), WeeklyClosingStatus.OPEN)
                .orElse(null);
        if (earlierOpenClosing != null) {
            throw new WeeklyClosingConflictException(
                    "Earlier week " + earlierOpenClosing.getWeekNumber() + "/"
                            + earlierOpenClosing.getWeekYear() + " must be Admin-closed first");
        }

        closing.setPochiSnapshots(
                weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(id));
        if (closing.getPochiSnapshots().isEmpty()) {
            throw new WeeklyClosingConflictException(
                    "A weekly closing must contain at least one Pochi before Admin can close it");
        }
        WeeklyClosingResponse completeness = toResponse(closing);
        if (!Boolean.TRUE.equals(completeness.getComplete())) {
            throw new WeeklyClosingIncompleteException(completeness.getUnresolvedDays());
        }

        User actor = userRepository.findById(TEMPORARY_USER_ID)
                .orElseThrow(() -> new UserNotFoundException(TEMPORARY_USER_ID));
        Instant closedAt = Instant.now();
        closing.setStatus(WeeklyClosingStatus.ADMIN_CLOSED);
        closing.setAdminClosedBy(actor);
        closing.setAdminClosedAt(closedAt);
        weeklyClosingRepository.save(closing);
        return completeness.toBuilder()
                .status(WeeklyClosingStatus.ADMIN_CLOSED)
                .adminClosedById(actor.getId())
                .adminClosedAt(closedAt)
                .build();
    }

    @Override
    @Transactional
    public WeeklyClosingResponse closeByOwner(Integer id) {
        WeeklyClosing closing = weeklyClosingRepository.findDetailsByIdForUpdate(id)
                .orElseThrow(() -> new WeeklyClosingNotFoundException(id));
        if (closing.getStatus() != WeeklyClosingStatus.ADMIN_CLOSED) {
            throw new WeeklyClosingConflictException(
                    "Only an ADMIN_CLOSED weekly closing can be closed by Owner");
        }

        WeeklyClosing earlierPendingClosing = weeklyClosingRepository
                .findFirstByOwner_IdAndWeekStartBeforeAndStatusInOrderByWeekStartAscIdAsc(
                        closing.getOwner().getId(),
                        closing.getWeekStart(),
                        List.of(WeeklyClosingStatus.OPEN, WeeklyClosingStatus.ADMIN_CLOSED))
                .orElse(null);
        if (earlierPendingClosing != null) {
            String requiredAction = earlierPendingClosing.getStatus() == WeeklyClosingStatus.OPEN
                    ? "must be Admin-closed before Owner closing"
                    : "must be Owner-closed first";
            throw new WeeklyClosingConflictException(
                    "Earlier week " + earlierPendingClosing.getWeekNumber() + "/"
                            + earlierPendingClosing.getWeekYear() + " "
                            + requiredAction);
        }

        User actor = userRepository.findById(TEMPORARY_USER_ID)
                .orElseThrow(() -> new UserNotFoundException(TEMPORARY_USER_ID));
        Instant closedAt = Instant.now();
        closing.setStatus(WeeklyClosingStatus.OWNER_CLOSED);
        closing.setOwnerClosedBy(actor);
        closing.setOwnerClosedAt(closedAt);
        weeklyClosingRepository.save(closing);
        closing.setPochiSnapshots(
                weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(id));
        return toResponse(closing).toBuilder()
                .reopens(weeklyClosingReopenRepository
                        .findAllByWeeklyClosing_IdOrderByReopenedAtAscIdAsc(id).stream()
                        .map(mapper::toReopenResponse)
                        .toList())
                .ownerClosedById(actor.getId())
                .ownerClosedAt(closedAt)
                .build();
    }

    @Override
    @Transactional
    public WeeklyClosingResponse reopen(Integer id, WeeklyClosingReopenRequest request) {
        String reason = request.getReason() == null ? "" : request.getReason().trim();
        if (reason.isEmpty()) {
            throw new InvalidWeeklyClosingException("A non-blank reopening reason is required.");
        }

        WeeklyClosing closing = weeklyClosingRepository.findDetailsByIdForUpdate(id)
                .orElseThrow(() -> new WeeklyClosingNotFoundException(id));
        if (closing.getStatus() != WeeklyClosingStatus.ADMIN_CLOSED) {
            throw new WeeklyClosingConflictException(
                    "Only an ADMIN_CLOSED weekly closing can be reopened");
        }

        User actor = userRepository.findById(TEMPORARY_USER_ID)
                .orElseThrow(() -> new UserNotFoundException(TEMPORARY_USER_ID));
        WeeklyClosingReopen reopen = WeeklyClosingReopen.builder()
                .weeklyClosing(closing)
                .reopenedBy(actor)
                .reopenedAt(Instant.now())
                .reason(reason)
                .build();
        weeklyClosingReopenRepository.save(reopen);

        closing.setStatus(WeeklyClosingStatus.OPEN);
        weeklyClosingRepository.save(closing);
        closing.setPochiSnapshots(
                weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(id));
        return toResponse(closing).toBuilder()
                .reopens(weeklyClosingReopenRepository
                        .findAllByWeeklyClosing_IdOrderByReopenedAtAscIdAsc(id).stream()
                        .map(mapper::toReopenResponse)
                        .toList())
                .build();
    }

    @Override
    @Transactional
    public WeeklyClosingResponse updateComment(Integer id, WeeklyClosingCommentUpdateRequest request) {
        WeeklyClosing closing = weeklyClosingRepository.findDetailsByIdForUpdate(id)
                .orElseThrow(() -> new WeeklyClosingNotFoundException(id));
        if (closing.getStatus() == WeeklyClosingStatus.OWNER_CLOSED) {
            throw new WeeklyClosingConflictException(
                    "The closing comment cannot be changed after Owner closing");
        }

        closing.setClosingComment(request.getClosingComment());
        weeklyClosingRepository.save(closing);
        closing.setPochiSnapshots(
                weeklyClosingPochiRepository.findAllByWeeklyClosing_IdOrderByPochi_IdAsc(id));
        return toResponse(closing).toBuilder()
                .reopens(weeklyClosingReopenRepository
                        .findAllByWeeklyClosing_IdOrderByReopenedAtAscIdAsc(id).stream()
                        .map(mapper::toReopenResponse)
                        .toList())
                .build();
    }

    @Override
    public WeeklyClosingResponse findById(Integer id) {
        WeeklyClosing closing = weeklyClosingRepository.findDetailsById(id)
                .orElseThrow(() -> new WeeklyClosingNotFoundException(id));
        closing.setPochiSnapshots(weeklyClosingPochiRepository
                .findAllByWeeklyClosing_IdOrderByPochi_IdAsc(id));
        return toResponse(closing).toBuilder()
                .reopens(weeklyClosingReopenRepository
                        .findAllByWeeklyClosing_IdOrderByReopenedAtAscIdAsc(id).stream()
                        .map(mapper::toReopenResponse)
                        .toList())
                .build();
    }

    @Override
    public List<WeeklyClosingResponse> findAll(
            Integer ownerId, Integer weekYear, Integer weekNumber, WeeklyClosingStatus status) {
        return weeklyClosingRepository.findAllFiltered(ownerId, weekYear, weekNumber, status).stream()
                .map(closing -> {
                    closing.setPochiSnapshots(weeklyClosingPochiRepository
                            .findAllByWeeklyClosing_IdOrderByPochi_IdAsc(closing.getId()));
                    return toResponse(closing);
                })
                .toList();
    }

    private WeeklyClosingResponse toResponse(WeeklyClosing closing) {
        List<WeeklyClosingPochi> snapshots = closing.getPochiSnapshots();
        int totalDayCount = snapshots.size() * 7;
        if (snapshots.isEmpty()) {
            return mapper.toResponse(closing).toBuilder()
                    .unresolvedDays(List.of())
                    .resolvedDayCount(0)
                    .totalDayCount(0)
                    .complete(false)
                    .build();
        }

        List<Integer> pochiIds = snapshots.stream()
                .map(snapshot -> snapshot.getPochi().getId())
                .toList();
        Set<PochiDay> resolvedDays = new HashSet<>();
        addResolvedDays(resolvedDays, assignmentRepository.findResolvedPochiDays(
                pochiIds, closing.getWeekStart(), closing.getWeekEnd()));
        addResolvedDays(resolvedDays, dailyStatusRepository.findResolvedPochiDays(
                pochiIds, closing.getWeekStart(), closing.getWeekEnd()));

        List<UnresolvedPochiDayResponse> unresolvedDays = new ArrayList<>();
        for (WeeklyClosingPochi snapshot : snapshots) {
            Integer pochiId = snapshot.getPochi().getId();
            for (LocalDate date = closing.getWeekStart(); !date.isAfter(closing.getWeekEnd()); date = date.plusDays(1)) {
                if (!resolvedDays.contains(new PochiDay(pochiId, date))) {
                    unresolvedDays.add(UnresolvedPochiDayResponse.builder()
                            .pochiId(pochiId)
                            .date(date)
                            .build());
                }
            }
        }

        return mapper.toResponse(closing).toBuilder()
                .unresolvedDays(unresolvedDays)
                .resolvedDayCount(totalDayCount - unresolvedDays.size())
                .totalDayCount(totalDayCount)
                .complete(unresolvedDays.isEmpty())
                .build();
    }

    private void addResolvedDays(Set<PochiDay> resolvedDays, List<PochiDayResolution> records) {
        records.stream()
                .map(record -> new PochiDay(record.getPochiId(), record.getResolvedDate()))
                .forEach(resolvedDays::add);
    }

    private record PochiDay(Integer pochiId, LocalDate date) {
    }
}
