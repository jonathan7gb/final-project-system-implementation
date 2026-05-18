package com.centroweg.senai.system_deployment_project_api.inventory.internal.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StockEntryJpaRepository extends JpaRepository<StockEntryJpaEntity, UUID> {

    List<StockEntryJpaEntity> findByPartIdOrderByCreatedAtDesc(UUID partId);
}
