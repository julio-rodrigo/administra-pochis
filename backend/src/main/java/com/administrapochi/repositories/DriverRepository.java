package com.administrapochi.repositories;

import com.administrapochi.models.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Integer> {

    List<Driver> findAllByDeletedFalseOrderByIdAsc();

    List<Driver> findAllByDeletedTrueOrderByIdAsc();

    Optional<Driver> findByIdAndDeletedFalse(Integer id);

    Optional<Driver> findByIdAndDeletedTrue(Integer id);

    Optional<Driver> findByCurp(String curp);

    boolean existsByCurp(String curp);
}
