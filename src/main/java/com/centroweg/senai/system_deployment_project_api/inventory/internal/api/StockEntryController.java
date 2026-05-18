package com.centroweg.senai.system_deployment_project_api.inventory.internal.api;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.api.dto.CreateStockEntryRequest;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.api.dto.StockEntryResponse;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.application.StockEntryService;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.StockEntry;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parts/{partId}/stock-entries")
public class StockEntryController {

    private final StockEntryService stockEntryService;
    private final CurrentUserProvider currentUserProvider;

    public StockEntryController(StockEntryService stockEntryService, CurrentUserProvider currentUserProvider) {
        this.stockEntryService = stockEntryService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMOXARIFE')")
    public StockEntryResponse registerEntry(
            @PathVariable UUID partId, @Valid @RequestBody CreateStockEntryRequest request) {
        StockEntry entry = stockEntryService.registerEntry(
                partId, request.quantity(), request.note(), currentUserProvider.getCurrentUserId());
        return StockEntryResponse.from(entry);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMOXARIFE')")
    public List<StockEntryResponse> listEntries(@PathVariable UUID partId) {
        return stockEntryService.listEntries(partId).stream()
                .map(StockEntryResponse::from)
                .toList();
    }
}
