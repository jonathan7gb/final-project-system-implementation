package com.centroweg.senai.system_deployment_project_api.inventory;

import java.util.UUID;

/**
 * Porta pública síncrona consumida pelo módulo Orders (RF07).
 * Implementação em {@code internal.infrastructure.adapter}.
 */
public interface StockCheckPort {

    StockCheckResult checkAvailability(UUID partId, int requestedQty);
}
