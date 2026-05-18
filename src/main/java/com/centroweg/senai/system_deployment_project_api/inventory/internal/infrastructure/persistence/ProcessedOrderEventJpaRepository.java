package com.centroweg.senai.system_deployment_project_api.inventory.internal.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProcessedOrderEventJpaRepository
        extends JpaRepository<ProcessedOrderEventJpaEntity, ProcessedOrderEventId> {}
