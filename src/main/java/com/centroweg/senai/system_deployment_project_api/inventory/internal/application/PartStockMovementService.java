package com.centroweg.senai.system_deployment_project_api.inventory.internal.application;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception.PartNotFoundException;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository.PartRepository;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PartStockMovementService {

    private final PartRepository partRepository;
    private final LowStockEventPublisher lowStockEventPublisher;

    public PartStockMovementService(
            PartRepository partRepository, LowStockEventPublisher lowStockEventPublisher) {
        this.partRepository = partRepository;
        this.lowStockEventPublisher = lowStockEventPublisher;
    }

    public Part applyMovement(UUID partId, Function<Part, Part> movement) {
        Part before = partRepository.findById(partId).orElseThrow(() -> new PartNotFoundException(partId));
        Part after = movement.apply(before);
        Part saved = partRepository.save(after);
        lowStockEventPublisher.publishIfCrossedBelowMinimum(before, saved);
        return saved;
    }
}
