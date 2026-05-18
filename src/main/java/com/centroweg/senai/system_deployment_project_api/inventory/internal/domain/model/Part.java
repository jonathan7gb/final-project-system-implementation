package com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception.InvalidStockOperationException;
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

    public Part withReservedIncrement(int quantity) {
        int newReserved = qtyReserved + quantity;
        if (newReserved < 0) {
            throw new InvalidStockOperationException(
                    "Reserva insuficiente para a peça " + code + ": qty_reserved ficaria " + newReserved);
        }
        return new Part(
                id, code, name, unit, qtyInStock, newReserved, qtyMinimum, active, createdAt, Instant.now());
    }

    public Part withFulfillment(int quantity) {
        int newStock = qtyInStock - quantity;
        int newReserved = qtyReserved - quantity;
        if (newStock < 0 || newReserved < 0) {
            throw new InvalidStockOperationException(
                    "Baixa inválida para a peça " + code + ": estoque ou reserva ficaria negativo");
        }
        return new Part(
                id, code, name, unit, newStock, newReserved, qtyMinimum, active, createdAt, Instant.now());
    }
}
