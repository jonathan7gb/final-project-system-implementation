package com.centroweg.senai.system_deployment_project_api.inventory.internal.application;

import com.centroweg.senai.system_deployment_project_api.orders.PedidoAprovadoEvent;
import com.centroweg.senai.system_deployment_project_api.orders.PedidoConcluidoEvent;
import com.centroweg.senai.system_deployment_project_api.orders.PedidoRejeitadoEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderStockEventListener {

    private final OrderStockService orderStockService;

    public OrderStockEventListener(OrderStockService orderStockService) {
        this.orderStockService = orderStockService;
    }

    @EventListener
    public void onPedidoAprovado(PedidoAprovadoEvent event) {
        orderStockService.handlePedidoAprovado(event);
    }

    @EventListener
    public void onPedidoConcluido(PedidoConcluidoEvent event) {
        orderStockService.handlePedidoConcluido(event);
    }

    @EventListener
    public void onPedidoRejeitado(PedidoRejeitadoEvent event) {
        orderStockService.handlePedidoRejeitado(event);
    }
}
