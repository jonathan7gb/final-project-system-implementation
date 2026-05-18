package com.centroweg.senai.system_deployment_project_api.inventory.internal.infrastructure.persistence;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;

final class PartPersistenceMapper {

    private PartPersistenceMapper() {}

    static Part toDomain(PartJpaEntity entity) {
        return new Part(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getUnit(),
                entity.getQtyInStock(),
                entity.getQtyReserved(),
                entity.getQtyMinimum(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    static PartJpaEntity toEntity(Part part) {
        PartJpaEntity entity = new PartJpaEntity();
        entity.setId(part.id());
        entity.setCode(part.code());
        entity.setName(part.name());
        entity.setUnit(part.unit());
        entity.setQtyInStock(part.qtyInStock());
        entity.setQtyReserved(part.qtyReserved());
        entity.setQtyMinimum(part.qtyMinimum());
        entity.setActive(part.active());
        entity.setCreatedAt(part.createdAt());
        entity.setUpdatedAt(part.updatedAt());
        return entity;
    }
}
