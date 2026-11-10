package com.administrapochi.dtos.driverabsences;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class DriverAbsenceResponse {

    private Integer id;
    private Integer driverId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
