package com.centroweg.senai.system_deployment_project_api.inventory.internal.api.dto;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import java.time.Instant;
import java.util.UUID;

public record PartResponse(
        UUID id,
        String code,
        String name,
        String unit,
        int qtyInStock,
        int qtyReserved,
        int qtyAvailable,
        int qtyMinimum,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static PartResponse from(Part part) {
        return new PartResponse(
                part.id(),
                part.code(),
                part.name(),
                part.unit(),
                part.qtyInStock(),
                part.qtyReserved(),
                part.qtyAvailable(),
                part.qtyMinimum(),
                part.active(),
                part.createdAt(),
                part.updatedAt());
    }
}
