package com.centroweg.senai.system_deployment_project_api.inventory.internal.api.dto;

import java.time.Instant;

public record ErrorResponse(String message, Instant timestamp) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, Instant.now());
    }
}
