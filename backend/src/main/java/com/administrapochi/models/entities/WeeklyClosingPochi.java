package com.administrapochi.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "weekly_closing_pochis")
public class WeeklyClosingPochi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weekly_closing_id", nullable = false, updatable = false)
    private WeeklyClosing weeklyClosing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pochi_id", nullable = false, updatable = false)
    private Pochi pochi;
}
