package com.centroweg.senai.system_deployment_project_api.inventory.internal.infrastructure.persistence;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.StockEntry;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository.StockEntryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class StockEntryRepositoryAdapter implements StockEntryRepository {

    private final StockEntryJpaRepository jpaRepository;

    StockEntryRepositoryAdapter(StockEntryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StockEntry save(StockEntry entry) {
        return StockEntryPersistenceMapper.toDomain(
                jpaRepository.save(StockEntryPersistenceMapper.toEntity(entry)));
    }

    @Override
    public List<StockEntry> findByPartIdOrderByCreatedAtDesc(UUID partId) {
        return jpaRepository.findByPartIdOrderByCreatedAtDesc(partId).stream()
                .map(StockEntryPersistenceMapper::toDomain)
                .toList();
    }
}
