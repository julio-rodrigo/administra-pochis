package com.administrapochi.repositories;

import com.administrapochi.models.entities.WeeklyClosingPochi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeeklyClosingPochiRepository extends JpaRepository<WeeklyClosingPochi, Integer> {

    List<WeeklyClosingPochi> findAllByWeeklyClosing_IdOrderByPochi_IdAsc(Integer weeklyClosingId);
}
