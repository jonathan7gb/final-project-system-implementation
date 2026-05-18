package com.centroweg.senai.system_deployment_project_api.inventory.internal.application;

import com.centroweg.senai.system_deployment_project_api.inventory.StockCheckPort;
import com.centroweg.senai.system_deployment_project_api.inventory.StockCheckResult;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception.PartNotFoundException;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.repository.PartRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockCheckService implements StockCheckPort {

    private final PartRepository partRepository;

    public StockCheckService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @Override
    public StockCheckResult checkAvailability(UUID partId, int requestedQty) {
        Part part = partRepository.findById(partId).orElseThrow(() -> new PartNotFoundException(partId));
        int available = part.qtyAvailable();
        return new StockCheckResult(available >= requestedQty, available, part.qtyInStock());
    }
}
