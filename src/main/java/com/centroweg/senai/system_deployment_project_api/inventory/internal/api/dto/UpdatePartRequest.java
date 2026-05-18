package com.centroweg.senai.system_deployment_project_api.inventory.internal.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePartRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 20) String unit,
        @Min(0) int qtyMinimum,
        @NotNull Boolean active) {}
