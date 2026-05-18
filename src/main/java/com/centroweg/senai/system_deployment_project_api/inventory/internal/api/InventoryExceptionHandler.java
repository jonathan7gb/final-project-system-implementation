package com.centroweg.senai.system_deployment_project_api.inventory.internal.api;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.api.dto.ErrorResponse;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception.DuplicatePartCodeException;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception.PartNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = {PartController.class, StockEntryController.class})
public class InventoryExceptionHandler {

    @ExceptionHandler(PartNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(PartNotFoundException ex) {
        return ErrorResponse.of(ex.getMessage());
    }

    @ExceptionHandler(DuplicatePartCodeException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicate(DuplicatePartCodeException ex) {
        return ErrorResponse.of(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Dados inválidos");
        return ErrorResponse.of(message);
    }
}
