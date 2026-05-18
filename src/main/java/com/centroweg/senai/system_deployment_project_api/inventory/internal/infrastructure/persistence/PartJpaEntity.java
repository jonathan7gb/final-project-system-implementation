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
@Table(schema = "inventory", name = "inv_parts")
@Getter
@Setter
@NoArgsConstructor
class PartJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "qty_in_stock", nullable = false)
    private int qtyInStock;

    @Column(name = "qty_reserved", nullable = false)
    private int qtyReserved;

    @Column(name = "qty_minimum", nullable = false)
    private int qtyMinimum;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
