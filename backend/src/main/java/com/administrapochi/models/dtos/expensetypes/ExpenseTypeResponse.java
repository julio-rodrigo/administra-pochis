package com.administrapochi.models.dtos.expensetypes;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ExpenseTypeResponse {

    private Integer id;
    private String code;
    private String name;
    private String description;
    private Boolean requiresPochi;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
