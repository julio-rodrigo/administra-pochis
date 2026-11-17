package com.administrapochi.models.dtos.expenses;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelExpenseRequest {

    @NotBlank
    private String cancellationReason;
}
