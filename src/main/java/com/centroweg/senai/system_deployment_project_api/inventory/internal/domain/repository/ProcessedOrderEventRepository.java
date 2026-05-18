package com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.OrderStockEventType;
import java.util.UUID;

public interface ProcessedOrderEventRepository {

    /**
     * Registra o processamento do evento. Retorna {@code false} se já foi processado (idempotência).
     */
    boolean tryMarkProcessed(UUID orderId, OrderStockEventType eventType);
}
