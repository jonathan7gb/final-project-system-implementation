package com.centroweg.senai.system_deployment_project_api.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.centroweg.senai.system_deployment_project_api.config.SecurityConfig;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.application.PartService;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.application.StockEntryService;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class StockCheckServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private StockCheckPort stockCheckPort;

    @Autowired
    private PartService partService;

    @Autowired
    private StockEntryService stockEntryService;

    @Test
    void shouldCheckAvailabilityUsingQtyAvailable() {
        Part part = partService.createPart("CHK-001", "Peça teste", "un", 0);
        stockEntryService.registerEntry(part.id(), 100, null, SecurityConfig.DEV_ADMIN_ID);

        StockCheckResult sufficient = stockCheckPort.checkAvailability(part.id(), 30);
        assertThat(sufficient.available()).isTrue();
        assertThat(sufficient.qtyAvailable()).isEqualTo(100);

        StockCheckResult insufficient = stockCheckPort.checkAvailability(part.id(), 150);
        assertThat(insufficient.available()).isFalse();
        assertThat(insufficient.qtyInStock()).isEqualTo(100);
    }
}
