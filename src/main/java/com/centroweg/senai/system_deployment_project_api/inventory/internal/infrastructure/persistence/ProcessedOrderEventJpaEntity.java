package com.centroweg.senai.system_deployment_project_api.inventory.internal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(schema = "inventory", name = "inv_processed_order_events")
@Getter
@Setter
@NoArgsConstructor
class ProcessedOrderEventJpaEntity {

    @EmbeddedId
    private ProcessedOrderEventId id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
