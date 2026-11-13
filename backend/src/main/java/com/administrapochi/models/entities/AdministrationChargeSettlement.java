package com.administrapochi.models.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "administration_charge_settlements")
public class AdministrationChargeSettlement {

    @EmbeddedId
    private AdministrationChargeSettlementId id;

    @MapsId("administrationChargeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administration_charge_id", nullable = false, updatable = false)
    private AdministrationCharge administrationCharge;

    @MapsId("settlementId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false, updatable = false)
    private Settlement settlement;
}
