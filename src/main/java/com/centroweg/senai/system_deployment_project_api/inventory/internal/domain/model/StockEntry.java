package com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model;

import java.time.Instant;
import java.util.UUID;

public record StockEntry(
        UUID id, UUID partId, int quantity, String note, UUID registeredBy, Instant createdAt) {}
