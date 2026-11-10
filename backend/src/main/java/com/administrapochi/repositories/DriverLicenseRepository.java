package com.administrapochi.repositories;

import com.administrapochi.models.entities.DriverLicense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverLicenseRepository extends JpaRepository<DriverLicense, Integer> {

    List<DriverLicense> findAllByDriverIdOrderByIssuedAtDescIdDesc(Integer driverId);

    Optional<DriverLicense> findByDriverIdAndCurrentLicenseTrue(Integer driverId);

    Optional<DriverLicense> findByIdAndDriverId(Integer id, Integer driverId);

    boolean existsByLicenseNumber(String licenseNumber);
}
