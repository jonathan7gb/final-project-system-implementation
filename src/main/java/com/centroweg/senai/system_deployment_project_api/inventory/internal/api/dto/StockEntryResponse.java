package com.centroweg.senai.system_deployment_project_api.inventory.internal.api.dto;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.StockEntry;
import java.time.Instant;
import java.util.UUID;

public record StockEntryResponse(
        UUID id, UUID partId, int quantity, String note, UUID registeredBy, Instant createdAt) {

    public static StockEntryResponse from(StockEntry entry) {
        return new StockEntryResponse(
                entry.id(),
                entry.partId(),
                entry.quantity(),
                entry.note(),
                entry.registeredBy(),
                entry.createdAt());
    }
}
