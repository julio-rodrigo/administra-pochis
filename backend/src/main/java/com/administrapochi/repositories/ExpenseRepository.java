package com.administrapochi.repositories;

import com.administrapochi.models.entities.Expense;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Integer>,
        JpaSpecificationExecutor<Expense> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT expense FROM Expense expense WHERE expense.id = :id")
    Optional<Expense> findByIdForUpdate(@Param("id") Integer id);
}
