package com.administrapochi.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class AdministrationChargeSettlementId implements Serializable {

    @Column(name = "administration_charge_id")
    private Integer administrationChargeId;

    @Column(name = "settlement_id")
    private Integer settlementId;
}
