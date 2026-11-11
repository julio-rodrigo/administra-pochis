package com.administrapochi.repositories;

import com.administrapochi.models.entities.SettlementRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SettlementRuleRepository extends
        JpaRepository<SettlementRule, Integer>,
        JpaSpecificationExecutor<SettlementRule> {

    @Query("""
            SELECT rule FROM SettlementRule rule
            WHERE rule.pochi.id = :pochiId
              AND rule.shiftCount = :shiftCount
              AND rule.validFrom <= :date
              AND (rule.validTo IS NULL OR rule.validTo >= :date)
            ORDER BY rule.validFrom DESC
            """)
    List<SettlementRule> findApplicableForPochi(
            @Param("pochiId") Integer pochiId,
            @Param("shiftCount") Short shiftCount,
            @Param("date") LocalDate date);

    @Query("""
            SELECT rule FROM SettlementRule rule
            WHERE rule.pochi IS NULL
              AND rule.shiftCount = :shiftCount
              AND rule.validFrom <= :date
              AND (rule.validTo IS NULL OR rule.validTo >= :date)
            ORDER BY rule.validFrom DESC
            """)
    List<SettlementRule> findApplicableGeneral(
            @Param("shiftCount") Short shiftCount,
            @Param("date") LocalDate date);

    @Query("""
            SELECT CASE WHEN COUNT(rule) > 0 THEN true ELSE false END
            FROM SettlementRule rule
            WHERE rule.pochi IS NULL
              AND rule.shiftCount = :shiftCount
              AND (:excludedId IS NULL OR rule.id <> :excludedId)
              AND (:validTo IS NULL OR rule.validFrom <= :validTo)
              AND (rule.validTo IS NULL OR rule.validTo >= :validFrom)
            """)
    boolean existsOverlappingGeneralRule(
            @Param("shiftCount") Short shiftCount,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo,
            @Param("excludedId") Integer excludedId);

    @Query("""
            SELECT CASE WHEN COUNT(rule) > 0 THEN true ELSE false END
            FROM SettlementRule rule
            WHERE rule.pochi.id = :pochiId
              AND rule.shiftCount = :shiftCount
              AND (:excludedId IS NULL OR rule.id <> :excludedId)
              AND (:validTo IS NULL OR rule.validFrom <= :validTo)
              AND (rule.validTo IS NULL OR rule.validTo >= :validFrom)
            """)
    boolean existsOverlappingPochiRule(
            @Param("pochiId") Integer pochiId,
            @Param("shiftCount") Short shiftCount,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo,
            @Param("excludedId") Integer excludedId);
}
