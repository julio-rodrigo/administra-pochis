package com.administrapochi.repositories;

import com.administrapochi.models.entities.DriverDebt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DriverDebtRepository extends
        JpaRepository<DriverDebt, Integer>,
        JpaSpecificationExecutor<DriverDebt> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT debt FROM DriverDebt debt WHERE debt.id = :id")
    Optional<DriverDebt> findByIdForUpdate(@Param("id") Integer id);
}
