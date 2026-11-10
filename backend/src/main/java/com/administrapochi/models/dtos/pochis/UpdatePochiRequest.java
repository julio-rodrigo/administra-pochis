package com.administrapochi.models.dtos.pochis;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePochiRequest {

    @NotBlank
    @Size(max = 10)
    private String economicNumber;

    @Size(max = 10)
    private String licensePlate;

    @NotNull
    private Integer ownerId;
}
