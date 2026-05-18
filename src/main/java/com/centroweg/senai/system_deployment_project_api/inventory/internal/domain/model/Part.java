package com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Part(
        UUID id,
        String code,
        String name,
        String unit,
        int qtyInStock,
        int qtyReserved,
        int qtyMinimum,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public int qtyAvailable() {
        return qtyInStock - qtyReserved;
    }

    public Part withStockIncrement(int quantity) {
        return new Part(
                id,
                code,
                name,
                unit,
                qtyInStock + quantity,
                qtyReserved,
                qtyMinimum,
                active,
                createdAt,
                Instant.now());
    }

    public Part withUpdatedFields(String name, String unit, int qtyMinimum, boolean active) {
        return new Part(
                id, code, name, unit, qtyInStock, qtyReserved, qtyMinimum, active, createdAt, Instant.now());
    }
}
