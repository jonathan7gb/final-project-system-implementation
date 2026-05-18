package com.centroweg.senai.system_deployment_project_api.orders;

import java.util.List;
import java.util.UUID;

public record PedidoConcluidoEvent(UUID orderId, UUID almoxarifeId, List<ItemRef> items) {}
