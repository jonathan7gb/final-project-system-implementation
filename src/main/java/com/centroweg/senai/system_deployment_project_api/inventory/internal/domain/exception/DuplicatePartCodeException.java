package com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception;

public class DuplicatePartCodeException extends RuntimeException {

    public DuplicatePartCodeException(String code) {
        super("Já existe uma peça com o código: " + code);
    }
}
