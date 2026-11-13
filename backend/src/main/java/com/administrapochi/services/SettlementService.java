package com.administrapochi.services;

import com.administrapochi.models.dtos.settlements.SettlementCreateRequest;
import com.administrapochi.models.dtos.settlements.SettlementResponse;
import com.administrapochi.models.dtos.settlements.SettlementUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SettlementService {

    SettlementResponse create(SettlementCreateRequest request);

    SettlementResponse findById(Integer id);

    Page<SettlementResponse> findAll(
            Integer driverId, Integer pochiId, LocalDate workDate,
            LocalDate workDateFrom, LocalDate workDateTo, Pageable pageable);

    SettlementResponse update(Integer id, SettlementUpdateRequest request);

    void delete(Integer id);
}
