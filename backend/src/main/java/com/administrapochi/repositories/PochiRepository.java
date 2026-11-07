package com.administrapochi.repositories;

import com.administrapochi.models.Pochi;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PochiRepository extends JpaRepository<Pochi, Integer> {

    boolean existsByEconomicNumber(String economicNumber);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByEconomicNumberAndIdNot(String economicNumber, Integer id);

    boolean existsByLicensePlateAndIdNot(String licensePlate, Integer id);
}
