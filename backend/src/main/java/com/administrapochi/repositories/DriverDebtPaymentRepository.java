package com.administrapochi.repositories;

import com.administrapochi.models.entities.DriverDebtPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DriverDebtPaymentRepository extends JpaRepository<DriverDebtPayment, Integer> {

    List<DriverDebtPayment> findAllByDriverDebtIdOrderByPaymentDateAscIdAsc(Integer driverDebtId);

    boolean existsByDriverDebtId(Integer driverDebtId);

    @Query("""
            SELECT COALESCE(SUM(payment.amount), 0)
            FROM DriverDebtPayment payment
            WHERE payment.driverDebt.id = :driverDebtId
            """)
    BigDecimal sumAmountByDriverDebtId(@Param("driverDebtId") Integer driverDebtId);
}
