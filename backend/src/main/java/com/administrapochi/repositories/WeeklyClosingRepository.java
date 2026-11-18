package com.administrapochi.repositories;

import com.administrapochi.models.entities.WeeklyClosing;
import com.administrapochi.models.enums.WeeklyClosingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyClosingRepository extends JpaRepository<WeeklyClosing, Integer> {

    boolean existsByOwner_IdAndWeekYearAndWeekNumber(Integer ownerId, Integer weekYear, Integer weekNumber);

    @EntityGraph(attributePaths = {"owner", "adminClosedBy", "ownerClosedBy"})
    Optional<WeeklyClosing> findDetailsById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT closing FROM WeeklyClosing closing WHERE closing.id = :id")
    Optional<WeeklyClosing> findDetailsByIdForUpdate(@Param("id") Integer id);

    Optional<WeeklyClosing> findFirstByOwner_IdAndWeekStartBeforeAndStatusOrderByWeekStartAscIdAsc(
            Integer ownerId, LocalDate weekStart, WeeklyClosingStatus status);

    Optional<WeeklyClosing> findFirstByOwner_IdAndWeekStartBeforeAndStatusInOrderByWeekStartAscIdAsc(
            Integer ownerId, LocalDate weekStart, List<WeeklyClosingStatus> statuses);

    @Query("""
            SELECT CASE WHEN COUNT(closing) > 0 THEN true ELSE false END
            FROM WeeklyClosing closing
            JOIN closing.pochiSnapshots snapshot
            WHERE snapshot.pochi.id = :pochiId
              AND closing.weekStart <= :date
              AND closing.weekEnd >= :date
              AND closing.status IN :statuses
            """)
    boolean existsProtectedMovementForPochiAndDate(
            @Param("pochiId") Integer pochiId,
            @Param("date") LocalDate date,
            @Param("statuses") List<WeeklyClosingStatus> statuses);

    @Query("""
            SELECT closing FROM WeeklyClosing closing
            WHERE (:ownerId IS NULL OR closing.owner.id = :ownerId)
              AND (:weekYear IS NULL OR closing.weekYear = :weekYear)
              AND (:weekNumber IS NULL OR closing.weekNumber = :weekNumber)
              AND (:status IS NULL OR closing.status = :status)
            ORDER BY closing.weekStart DESC, closing.id DESC
            """)
    List<WeeklyClosing> findAllFiltered(
            @Param("ownerId") Integer ownerId,
            @Param("weekYear") Integer weekYear,
            @Param("weekNumber") Integer weekNumber,
            @Param("status") WeeklyClosingStatus status);
}
