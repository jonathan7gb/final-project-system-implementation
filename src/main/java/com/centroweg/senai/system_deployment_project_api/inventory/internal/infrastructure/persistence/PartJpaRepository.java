package com.centroweg.senai.system_deployment_project_api.inventory.internal.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PartJpaRepository extends JpaRepository<PartJpaEntity, UUID> {

    Optional<PartJpaEntity> findByCode(String code);

    List<PartJpaEntity> findByActiveTrueOrderByCodeAsc();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
