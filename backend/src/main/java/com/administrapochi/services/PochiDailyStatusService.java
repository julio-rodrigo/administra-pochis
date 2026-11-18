package com.administrapochi.services;

import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusCreateRequest;
import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusResponse;
import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusUpdateRequest;
import com.administrapochi.models.enums.PochiDailyStatusType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PochiDailyStatusService {

    PochiDailyStatusResponse create(PochiDailyStatusCreateRequest request);

    PochiDailyStatusResponse findById(Integer id);

    Page<PochiDailyStatusResponse> findAll(Integer pochiId, PochiDailyStatusType status,
            LocalDate statusDate, LocalDate dateFrom, LocalDate dateTo, Pageable pageable);

    PochiDailyStatusResponse update(Integer id, PochiDailyStatusUpdateRequest request);

    void delete(Integer id);
}
