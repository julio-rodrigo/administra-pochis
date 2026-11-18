package com.administrapochi.repositories;

import com.administrapochi.models.entities.Pochi;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PochiRepository extends JpaRepository<Pochi, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pochi FROM Pochi pochi WHERE pochi.id = :id")
    Optional<Pochi> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByEconomicNumber(String economicNumber);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByEconomicNumberAndIdNot(String economicNumber, Integer id);

    boolean existsByLicensePlateAndIdNot(String licensePlate, Integer id);

    List<Pochi> findAllByOwner_IdOrderByIdAsc(Integer ownerId);
}
