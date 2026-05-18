package com.centroweg.senai.system_deployment_project_api.inventory;

import java.util.UUID;

/**
 * Publicado quando o estoque disponível cruza o limite mínimo (RF19).
 * Consumido pelo módulo Notification.
 */
public record EstoqueInsuficienteEvent(
        UUID partId, String partName, int qtyInStock, int qtyMinimum) {}
