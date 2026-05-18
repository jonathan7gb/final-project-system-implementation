package com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception;

import java.util.UUID;

public class PartNotFoundException extends RuntimeException {

    public PartNotFoundException(UUID id) {
        super("Peça não encontrada: " + id);
    }
}
