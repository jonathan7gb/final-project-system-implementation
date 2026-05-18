package com.centroweg.senai.system_deployment_project_api.inventory.internal.application;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception.PartNotFoundException;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.StockEntry;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository.PartRepository;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository.StockEntryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockEntryService {

    private final PartRepository partRepository;
    private final StockEntryRepository stockEntryRepository;
    private final PartStockMovementService partStockMovementService;

    public StockEntryService(
            PartRepository partRepository,
            StockEntryRepository stockEntryRepository,
            PartStockMovementService partStockMovementService) {
        this.partRepository = partRepository;
        this.stockEntryRepository = stockEntryRepository;
        this.partStockMovementService = partStockMovementService;
    }

    public StockEntry registerEntry(UUID partId, int quantity, String note, UUID registeredBy) {
        if (partRepository.findById(partId).isEmpty()) {
            throw new PartNotFoundException(partId);
        }
        partStockMovementService.applyMovement(partId, part -> part.withStockIncrement(quantity));

        StockEntry entry = new StockEntry(
                UUID.randomUUID(), partId, quantity, note, registeredBy, Instant.now());
        return stockEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<StockEntry> listEntries(UUID partId) {
        if (partRepository.findById(partId).isEmpty()) {
            throw new PartNotFoundException(partId);
        }
        return stockEntryRepository.findByPartIdOrderByCreatedAtDesc(partId);
    }
}
