package com.administrapochi.repositories;

import com.administrapochi.models.entities.AdministrationCharge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Optional;

public interface AdministrationChargeRepository extends
        JpaRepository<AdministrationCharge, Integer>,
        JpaSpecificationExecutor<AdministrationCharge> {

    boolean existsByPochiIdAndChargeDate(Integer pochiId, LocalDate chargeDate);

    @EntityGraph(attributePaths = {"pochi", "administrationFeeRule", "createdBy"})
    Optional<AdministrationCharge> findWithDetailsById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"pochi", "administrationFeeRule", "createdBy"})
    Page<AdministrationCharge> findAll(Specification<AdministrationCharge> specification, Pageable pageable);
}
