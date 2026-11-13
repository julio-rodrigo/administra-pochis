package com.administrapochi.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "administration_charges")
public class AdministrationCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pochi_id", nullable = false, updatable = false)
    private Pochi pochi;

    @Column(name = "charge_date", nullable = false, updatable = false)
    private LocalDate chargeDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administration_fee_rule_id", nullable = false, updatable = false)
    private AdministrationFeeRule administrationFeeRule;

    @Column(name = "shift_count", nullable = false, updatable = false)
    private Short shiftCount;

    @Column(name = "suggested_amount", nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal suggestedAmount;

    @Column(nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "administrationCharge", fetch = FetchType.LAZY)
    private List<AdministrationChargeSettlement> chargeSettlements = new ArrayList<>();

    @PrePersist
    void applyCreationTimestamp() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
