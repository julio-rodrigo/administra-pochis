package com.administrapochi.repositories;

import com.administrapochi.models.entities.AdministrationChargeSettlement;
import com.administrapochi.models.entities.AdministrationChargeSettlementId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AdministrationChargeSettlementRepository extends
        JpaRepository<AdministrationChargeSettlement, AdministrationChargeSettlementId> {

    boolean existsBySettlementId(Integer settlementId);

    @Query("""
            SELECT CASE WHEN COUNT(link) > 0 THEN true ELSE false END
            FROM AdministrationChargeSettlement link
            JOIN link.settlement settlement
            JOIN settlement.pochiAssignments assignment
            WHERE assignment.id = :assignmentId
            """)
    boolean existsBySettlementPochiAssignmentId(@Param("assignmentId") Integer assignmentId);

    @Query("""
            SELECT link.id.settlementId FROM AdministrationChargeSettlement link
            WHERE link.id.settlementId IN :settlementIds
            """)
    Set<Integer> findLinkedSettlementIds(@Param("settlementIds") Collection<Integer> settlementIds);

    @EntityGraph(attributePaths = {"settlement", "settlement.pochiAssignments"})
    List<AdministrationChargeSettlement> findAllByAdministrationChargeIdIn(
            Collection<Integer> administrationChargeIds);
}
