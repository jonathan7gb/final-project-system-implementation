package com.centroweg.senai.system_deployment_project_api.inventory.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "inventory", name = "inv_stock_entries")
@Getter
@Setter
@NoArgsConstructor
class StockEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "part_id", nullable = false)
    private UUID partId;

    @Column(nullable = false)
    private int quantity;

    private String note;

    @Column(name = "registered_by", nullable = false)
    private UUID registeredBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
