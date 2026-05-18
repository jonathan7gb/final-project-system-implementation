package com.centroweg.senai.system_deployment_project_api.inventory.internal.application;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception.DuplicatePartCodeException;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception.PartNotFoundException;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository.PartRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartService {

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @Transactional(readOnly = true)
    public List<Part> listActiveParts() {
        return partRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public Part getPart(UUID id) {
        return partRepository.findById(id).orElseThrow(() -> new PartNotFoundException(id));
    }

    public Part createPart(String code, String name, String unit, int qtyMinimum) {
        if (partRepository.existsByCode(code)) {
            throw new DuplicatePartCodeException(code);
        }
        Instant now = Instant.now();
        Part part = new Part(
                UUID.randomUUID(), code, name, unit, 0, 0, qtyMinimum, true, now, now);
        return partRepository.save(part);
    }

    public Part updatePart(UUID id, String name, String unit, int qtyMinimum, boolean active) {
        Part existing = getPart(id);
        return partRepository.save(existing.withUpdatedFields(name, unit, qtyMinimum, active));
    }
}
