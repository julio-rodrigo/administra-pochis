package com.administrapochi.repositories;

import com.administrapochi.models.entities.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface SettlementRepository extends
        JpaRepository<Settlement, Integer>,
        JpaSpecificationExecutor<Settlement> {

    @Query("""
            SELECT assignment.id FROM Settlement settlement
            JOIN settlement.pochiAssignments assignment
            WHERE assignment.id IN :assignmentIds
            """)
    Set<Integer> findSettledAssignmentIds(@Param("assignmentIds") Collection<Integer> assignmentIds);

    @Query("""
            SELECT DISTINCT settlement FROM Settlement settlement
            JOIN FETCH settlement.pochiAssignments assignment
            WHERE assignment.id IN :assignmentIds
            """)
    List<Settlement> findByPochiAssignmentIds(@Param("assignmentIds") Collection<Integer> assignmentIds);
}
