package com.centroweg.senai.system_deployment_project_api.inventory.internal.infrastructure.persistence;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository.PartRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class PartRepositoryAdapter implements PartRepository {

    private final PartJpaRepository jpaRepository;

    PartRepositoryAdapter(PartJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Part save(Part part) {
        return PartPersistenceMapper.toDomain(jpaRepository.save(PartPersistenceMapper.toEntity(part)));
    }

    @Override
    public Optional<Part> findById(UUID id) {
        return jpaRepository.findById(id).map(PartPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Part> findByCode(String code) {
        return jpaRepository.findByCode(code).map(PartPersistenceMapper::toDomain);
    }

    @Override
    public List<Part> findAllActive() {
        return jpaRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(PartPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, UUID id) {
        return jpaRepository.existsByCodeAndIdNot(code, id);
    }
}
