package com.centroweg.senai.system_deployment_project_api.orders;

import java.util.List;
import java.util.UUID;

public record PedidoRejeitadoEvent(
        UUID orderId, UUID reviewerId, String rejectionNote, List<ItemRef> items) {}
