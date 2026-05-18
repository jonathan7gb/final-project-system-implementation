package com.centroweg.senai.system_deployment_project_api.inventory;

/**
 * Resultado da verificação de estoque disponível para um pedido.
 *
 * @param available    {@code true} quando {@code qtyAvailable >= requestedQty}
 * @param qtyAvailable quantidade disponível ({@code qtyInStock - qtyReserved})
 * @param qtyInStock   quantidade física em estoque
 */
public record StockCheckResult(boolean available, int qtyAvailable, int qtyInStock) {}
