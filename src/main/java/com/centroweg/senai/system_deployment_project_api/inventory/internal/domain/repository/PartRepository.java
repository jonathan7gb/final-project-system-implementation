package com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartRepository {

    Part save(Part part);

    Optional<Part> findById(UUID id);

    Optional<Part> findByCode(String code);

    List<Part> findAllActive();

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
