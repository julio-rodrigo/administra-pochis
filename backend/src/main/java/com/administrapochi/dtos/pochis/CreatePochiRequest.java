package com.administrapochi.dtos.pochis;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePochiRequest {

    @NotBlank
    @Size(max = 10)
    private String economicNumber;

    @Size(max = 10)
    private String licensePlate;

    @Size(max = 15)
    private String unionName;
}
