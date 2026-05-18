package com.centroweg.senai.system_deployment_project_api.inventory.internal.api;

import com.centroweg.senai.system_deployment_project_api.inventory.internal.api.dto.CreatePartRequest;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.api.dto.PartResponse;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.api.dto.UpdatePartRequest;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.application.PartService;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<PartResponse> listParts() {
        return partService.listActiveParts().stream().map(PartResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PartResponse getPart(@PathVariable UUID id) {
        return PartResponse.from(partService.getPart(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMOXARIFE')")
    public PartResponse createPart(@Valid @RequestBody CreatePartRequest request) {
        Part part = partService.createPart(
                request.code(), request.name(), request.unit(), request.qtyMinimum());
        return PartResponse.from(part);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMOXARIFE')")
    public PartResponse updatePart(@PathVariable UUID id, @Valid @RequestBody UpdatePartRequest request) {
        Part part = partService.updatePart(
                id, request.name(), request.unit(), request.qtyMinimum(), request.active());
        return PartResponse.from(part);
    }
}
