package com.administrapochi.services;

import com.administrapochi.models.dtos.drivers.DriverCreateRequest;
import com.administrapochi.models.dtos.drivers.DriverResponse;
import com.administrapochi.models.dtos.drivers.DriverUpdateRequest;
import com.administrapochi.exceptions.DriverDeletedException;
import com.administrapochi.exceptions.DriverNotFoundException;
import com.administrapochi.exceptions.DuplicateCurpException;
import com.administrapochi.models.mappers.DriverMapper;
import com.administrapochi.models.entities.Driver;
import com.administrapochi.models.enums.DriverStatus;
import com.administrapochi.repositories.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    @Override
    public List<DriverResponse> findAll() {
        return driverMapper.toResponseList(driverRepository.findAllByDeletedFalseOrderByIdAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> findDeleted() {
        return driverMapper.toResponseList(driverRepository.findAllByDeletedTrueOrderByIdAsc());
    }

    @Override
    public DriverResponse findById(Integer id) {
        return driverMapper.toResponse(findDriver(id));
    }

    @Override
    public DriverResponse create(DriverCreateRequest request) {
        String curp = normalizeCurp(request.getCurp());
        if (driverRepository.existsByCurp(curp)) {
            throw new DuplicateCurpException(curp);
        }

        Driver driver = driverMapper.toEntity(request);
        driver.setCurp(curp);
        driver.setEmploymentStatus(DriverStatus.ACTIVE);
        driver.setDeleted(false);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Override
    @Transactional
    public DriverResponse update(Integer id, DriverUpdateRequest request) {
        Driver driver = findDriver(id);
        driverMapper.updateEntity(request, driver);
        driver.setFirstName(normalizeName(request.getFirstName()));
        driver.setFirstLastName(normalizeName(request.getFirstLastName()));
        driver.setSecondLastName(normalizeOptionalName(request.getSecondLastName()));
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Override
    @Transactional
    public DriverResponse changeEmploymentStatus(Integer id, DriverStatus status) {
        return setEmploymentStatus(id, status);
    }

    @Override
    @Transactional
    public DriverResponse activate(Integer id) {
        return setEmploymentStatus(id, DriverStatus.ACTIVE);
    }

    @Override
    @Transactional
    public DriverResponse deactivate(Integer id) {
        return setEmploymentStatus(id, DriverStatus.INACTIVE);
    }

    private DriverResponse setEmploymentStatus(Integer id, DriverStatus status) {
        Driver driver = findDriver(id);
        if (driver.getEmploymentStatus() != status) {
            driver.setEmploymentStatus(status);
            driver = driverRepository.save(driver);
        }
        return driverMapper.toResponse(driver);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Driver driver = findDriver(id);
        driver.setDeleted(true);
        driver.setDeletedAt(Instant.now());
        driver.setEmploymentStatus(DriverStatus.INACTIVE);
        driverRepository.save(driver);
    }

    @Override
    @Transactional
    public DriverResponse restore(Integer id) {
        Driver driver = driverRepository.findByIdAndDeletedTrue(id)
                .orElseThrow(() -> new DriverNotFoundException(id));
        driver.setDeleted(false);
        driver.setDeletedAt(null);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    private Driver findDriver(Integer id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException(id));
        if (Boolean.TRUE.equals(driver.getDeleted())) {
            throw new DriverDeletedException(id);
        }
        return driver;
    }

    private String normalizeCurp(String curp) {
        return curp.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeOptionalName(String name) {
        return name == null || name.isBlank() ? null : name.trim();
    }
}
