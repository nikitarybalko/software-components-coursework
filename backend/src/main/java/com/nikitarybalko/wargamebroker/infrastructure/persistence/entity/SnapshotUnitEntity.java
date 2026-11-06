package com.nikitarybalko.wargamebroker.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.nikitarybalko.wargamebroker.domain.model.Side;
import com.nikitarybalko.wargamebroker.domain.model.UnitStatus;
import com.nikitarybalko.wargamebroker.domain.model.UnitType;

import java.util.UUID;

@Entity
@Table(name = "snapshot_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotUnitEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private SnapshotEntity snapshot;

    @Column(nullable = false)
    private String unitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Side side;

    @Enumerated(EnumType.STRING)
    private UnitType unitType;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(nullable = false)
    private double viewRadius;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitStatus status;
}
