package com.centroweg.senai.system_deployment_project_api.inventory.internal.infrastructure.persistence;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.OrderStockEventType;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository.ProcessedOrderEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
class ProcessedOrderEventRepositoryAdapter implements ProcessedOrderEventRepository {

    private final ProcessedOrderEventJpaRepository jpaRepository;

    ProcessedOrderEventRepositoryAdapter(ProcessedOrderEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean tryMarkProcessed(UUID orderId, OrderStockEventType eventType) {
        ProcessedOrderEventJpaEntity entity = new ProcessedOrderEventJpaEntity();
        entity.setId(new ProcessedOrderEventId(orderId, eventType.name()));
        entity.setProcessedAt(Instant.now());
        try {
            jpaRepository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
