package com.administrapochi.repositories;

import com.administrapochi.models.entities.AdministrationFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AdministrationFeeRuleRepository extends
        JpaRepository<AdministrationFeeRule, Integer>,
        JpaSpecificationExecutor<AdministrationFeeRule> {

    @Query("""
            SELECT rule FROM AdministrationFeeRule rule
            WHERE rule.pochi.id = :pochiId
              AND rule.active = true
              AND rule.validFrom <= :date
              AND (rule.validTo IS NULL OR rule.validTo >= :date)
            ORDER BY rule.validFrom DESC
            """)
    List<AdministrationFeeRule> findApplicableForPochi(
            @Param("pochiId") Integer pochiId, @Param("date") LocalDate date);

    @Query("""
            SELECT rule FROM AdministrationFeeRule rule
            WHERE rule.pochi IS NULL
              AND rule.active = true
              AND rule.validFrom <= :date
              AND (rule.validTo IS NULL OR rule.validTo >= :date)
            ORDER BY rule.validFrom DESC
            """)
    List<AdministrationFeeRule> findApplicableGlobalRule(@Param("date") LocalDate date);

    @Query("""
            SELECT CASE WHEN COUNT(rule) > 0 THEN true ELSE false END
            FROM AdministrationFeeRule rule
            WHERE rule.pochi IS NULL
              AND (:excludedId IS NULL OR rule.id <> :excludedId)
              AND (:validTo IS NULL OR rule.validFrom <= :validTo)
              AND (rule.validTo IS NULL OR rule.validTo >= :validFrom)
            """)
    boolean existsOverlappingGlobalRule(
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo,
            @Param("excludedId") Integer excludedId);

    @Query("""
            SELECT CASE WHEN COUNT(rule) > 0 THEN true ELSE false END
            FROM AdministrationFeeRule rule
            WHERE rule.pochi.id = :pochiId
              AND (:excludedId IS NULL OR rule.id <> :excludedId)
              AND (:validTo IS NULL OR rule.validFrom <= :validTo)
              AND (rule.validTo IS NULL OR rule.validTo >= :validFrom)
            """)
    boolean existsOverlappingPochiRule(
            @Param("pochiId") Integer pochiId,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo,
            @Param("excludedId") Integer excludedId);
}
