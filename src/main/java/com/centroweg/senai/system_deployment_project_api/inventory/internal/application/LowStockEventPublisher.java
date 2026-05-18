package com.centroweg.senai.system_deployment_project_api.inventory.internal.application;

import com.centroweg.senai.system_deployment_project_api.inventory.EstoqueInsuficienteEvent;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publica {@link EstoqueInsuficienteEvent} somente quando o estoque disponível
 * cruza o limite mínimo (de acima/igual para abaixo).
 */
@Component
public class LowStockEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public LowStockEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishIfCrossedBelowMinimum(Part before, Part after) {
        int minimum = after.qtyMinimum();
        boolean wasAtOrAboveMinimum = before.qtyAvailable() >= minimum;
        boolean isBelowMinimum = after.qtyAvailable() < minimum;
        if (wasAtOrAboveMinimum && isBelowMinimum) {
            eventPublisher.publishEvent(new EstoqueInsuficienteEvent(
                    after.id(), after.name(), after.qtyInStock(), after.qtyMinimum()));
        }
    }
}
