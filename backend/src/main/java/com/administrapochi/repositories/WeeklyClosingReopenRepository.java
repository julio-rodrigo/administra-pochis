package com.administrapochi.repositories;

import com.administrapochi.models.entities.WeeklyClosingReopen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeeklyClosingReopenRepository extends JpaRepository<WeeklyClosingReopen, Integer> {

    List<WeeklyClosingReopen> findAllByWeeklyClosing_IdOrderByReopenedAtAscIdAsc(Integer weeklyClosingId);
}
