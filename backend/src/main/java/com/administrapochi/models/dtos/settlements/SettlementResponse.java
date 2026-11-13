package com.administrapochi.models.dtos.settlements;

import com.administrapochi.models.dtos.pochiassignments.PochiAssignmentResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SettlementResponse {

    private Integer id;
    private Integer driverId;
    private String driverName;
    private Integer pochiId;
    private String pochiIdentifier;
    private LocalDate workDate;
    private BigDecimal expectedAmount;
    private BigDecimal acceptedAmount;
    private String adjustmentReason;
    private String notes;
    private List<PochiAssignmentResponse> pochiAssignments;
    private Integer createdById;
    private String createdByName;
    private Integer modifiedById;
    private String modifiedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
