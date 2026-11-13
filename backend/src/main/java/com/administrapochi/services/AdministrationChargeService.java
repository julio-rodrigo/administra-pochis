package com.administrapochi.services;

import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeCreateRequest;
import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AdministrationChargeService {

    AdministrationChargeResponse create(AdministrationChargeCreateRequest request);

    AdministrationChargeResponse findById(Integer id);

    Page<AdministrationChargeResponse> findAll(
            Integer pochiId,
            LocalDate chargeDate,
            LocalDate chargeDateFrom,
            LocalDate chargeDateTo,
            Integer createdById,
            Boolean adjusted,
            Pageable pageable);
}
