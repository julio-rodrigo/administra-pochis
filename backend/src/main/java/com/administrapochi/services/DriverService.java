package com.administrapochi.services;

import com.administrapochi.dtos.drivers.DriverCreateRequest;
import com.administrapochi.dtos.drivers.DriverResponse;
import com.administrapochi.dtos.drivers.DriverUpdateRequest;
import com.administrapochi.models.DriverStatus;

import java.util.List;

public interface DriverService {

    List<DriverResponse> findAll();

    List<DriverResponse> findDeleted();

    DriverResponse findById(Integer id);

    DriverResponse create(DriverCreateRequest request);

    DriverResponse update(Integer id, DriverUpdateRequest request);

    DriverResponse changeEmploymentStatus(Integer id, DriverStatus status);

    DriverResponse activate(Integer id);

    DriverResponse deactivate(Integer id);

    void delete(Integer id);

    DriverResponse restore(Integer id);
}
