package com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.StockEntry;
import java.util.List;
import java.util.UUID;

public interface StockEntryRepository {

    StockEntry save(StockEntry entry);

    List<StockEntry> findByPartIdOrderByCreatedAtDesc(UUID partId);
}
