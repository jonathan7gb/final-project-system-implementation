package com.centroweg.senai.system_deployment_project_api.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.centroweg.senai.system_deployment_project_api.config.SecurityConfig;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.application.PartService;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.application.StockEntryService;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import com.centroweg.senai.system_deployment_project_api.inventory.support.EstoqueInsuficienteEventCaptor;
import com.centroweg.senai.system_deployment_project_api.orders.ItemRef;
import com.centroweg.senai.system_deployment_project_api.orders.PedidoAprovadoEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class LowStockEventPublisherTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PartService partService;

    @Autowired
    private StockEntryService stockEntryService;

    @Autowired
    private EstoqueInsuficienteEventCaptor eventCaptor;

    @BeforeEach
    void setUp() {
        eventCaptor.clear();
    }

    @Test
    void shouldPublishOnlyWhenCrossingBelowMinimum() {
        Part part = partService.createPart(
                "LOW-" + UUID.randomUUID().toString().substring(0, 8), "Peça estoque baixo", "un", 50);
        stockEntryService.registerEntry(part.id(), 100, null, SecurityConfig.DEV_ADMIN_ID);

        UUID orderId = UUID.randomUUID();
        eventPublisher.publishEvent(new PedidoAprovadoEvent(
                orderId,
                SecurityConfig.DEV_ADMIN_ID,
                List.of(new ItemRef(part.id(), 60))));

        assertThat(eventCaptor.getEvents()).hasSize(1);
        EstoqueInsuficienteEvent event = eventCaptor.getEvents().getFirst();
        assertThat(event.partId()).isEqualTo(part.id());
        assertThat(event.qtyMinimum()).isEqualTo(50);

        eventCaptor.clear();
        eventPublisher.publishEvent(new PedidoAprovadoEvent(
                orderId, SecurityConfig.DEV_ADMIN_ID, List.of(new ItemRef(part.id(), 60))));

        assertThat(eventCaptor.getEvents()).isEmpty();
    }

    @Test
    void shouldNotPublishWhenAlreadyBelowMinimum() {
        Part part = partService.createPart(
                "LOW2-" + UUID.randomUUID().toString().substring(0, 8), "Peça já baixa", "un", 20);
        stockEntryService.registerEntry(part.id(), 10, null, SecurityConfig.DEV_ADMIN_ID);

        assertThat(eventCaptor.getEvents()).isEmpty();
    }
}
