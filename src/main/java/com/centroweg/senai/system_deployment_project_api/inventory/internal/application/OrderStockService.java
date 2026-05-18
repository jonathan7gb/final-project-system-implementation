package com.centroweg.senai.system_deployment_project_api.inventory.internal.application;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception.PartNotFoundException;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.OrderStockEventType;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository.PartRepository;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository.ProcessedOrderEventRepository;
import com.centroweg.senai.system_deployment_project_api.orders.ItemRef;
import com.centroweg.senai.system_deployment_project_api.orders.PedidoAprovadoEvent;
import com.centroweg.senai.system_deployment_project_api.orders.PedidoConcluidoEvent;
import com.centroweg.senai.system_deployment_project_api.orders.PedidoRejeitadoEvent;
import java.util.List;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderStockService {

    private final ProcessedOrderEventRepository processedOrderEventRepository;
    private final PartRepository partRepository;

    public OrderStockService(
            ProcessedOrderEventRepository processedOrderEventRepository, PartRepository partRepository) {
        this.processedOrderEventRepository = processedOrderEventRepository;
        this.partRepository = partRepository;
    }

    public void handlePedidoAprovado(PedidoAprovadoEvent event) {
        if (!processedOrderEventRepository.tryMarkProcessed(
                event.orderId(), OrderStockEventType.PEDIDO_APROVADO)) {
            return;
        }
        applyToItems(event.items(), Part::withReservedIncrement);
    }

    public void handlePedidoConcluido(PedidoConcluidoEvent event) {
        if (!processedOrderEventRepository.tryMarkProcessed(
                event.orderId(), OrderStockEventType.PEDIDO_CONCLUIDO)) {
            return;
        }
        applyToItems(event.items(), Part::withFulfillment);
    }

    public void handlePedidoRejeitado(PedidoRejeitadoEvent event) {
        if (!processedOrderEventRepository.tryMarkProcessed(
                event.orderId(), OrderStockEventType.PEDIDO_REJEITADO)) {
            return;
        }
        applyToItems(event.items(), (part, quantity) -> part.withReservedIncrement(-quantity));
    }

    private void applyToItems(List<ItemRef> items, BiFunction<Part, Integer, Part> movement) {
        for (ItemRef item : items) {
            Part part = partRepository.findById(item.partId()).orElseThrow(() -> new PartNotFoundException(item.partId()));
            partRepository.save(movement.apply(part, item.quantity()));
        }
    }
}
