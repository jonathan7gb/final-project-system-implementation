package com.centroweg.senai.system_deployment_project_api.inventory.internal.infrastructure.persistence;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.StockEntry;

final class StockEntryPersistenceMapper {

    private StockEntryPersistenceMapper() {}

    static StockEntry toDomain(StockEntryJpaEntity entity) {
        return new StockEntry(
                entity.getId(),
                entity.getPartId(),
                entity.getQuantity(),
                entity.getNote(),
                entity.getRegisteredBy(),
                entity.getCreatedAt());
    }

    static StockEntryJpaEntity toEntity(StockEntry entry) {
        StockEntryJpaEntity entity = new StockEntryJpaEntity();
        entity.setId(entry.id());
        entity.setPartId(entry.partId());
        entity.setQuantity(entry.quantity());
        entity.setNote(entry.note());
        entity.setRegisteredBy(entry.registeredBy());
        entity.setCreatedAt(entry.createdAt());
        return entity;
    }
}
