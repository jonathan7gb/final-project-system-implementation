package com.centroweg.senai.system_deployment_project_api.inventory.internal.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStockEntryRequest(
        @NotNull @Min(1) Integer quantity, @Size(max = 500) String note) {}
