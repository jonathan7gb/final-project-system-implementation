package com.centroweg.senai.system_deployment_project_api.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.centroweg.senai.system_deployment_project_api.config.SecurityConfig;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.application.PartService;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.application.StockEntryService;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.exception.InvalidStockOperationException;
import com.centroweg.senai.system_deployment_project_api.inventory.internal.domain.model.Part;
import com.centroweg.senai.system_deployment_project_api.orders.ItemRef;
import com.centroweg.senai.system_deployment_project_api.orders.PedidoAprovadoEvent;
import com.centroweg.senai.system_deployment_project_api.orders.PedidoConcluidoEvent;
import com.centroweg.senai.system_deployment_project_api.orders.PedidoRejeitadoEvent;
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
class OrderStockServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PartService partService;

    @Autowired
    private StockEntryService stockEntryService;

    private Part part;
    private final UUID orderId = UUID.randomUUID();
    private final UUID actorId = SecurityConfig.DEV_ADMIN_ID;

    @BeforeEach
    void setUp() {
        part = partService.createPart("ORD-" + UUID.randomUUID().toString().substring(0, 8), "Peça pedido", "un", 5);
        stockEntryService.registerEntry(part.id(), 100, "estoque inicial", actorId);
        part = partService.getPart(part.id());
    }

    @Test
    void shouldReserveFulfillAndReleaseStockThroughOrderLifecycle() {
        publishApproved(20);
        Part reserved = partService.getPart(part.id());
        assertThat(reserved.qtyReserved()).isEqualTo(20);
        assertThat(reserved.qtyInStock()).isEqualTo(100);
        assertThat(reserved.qtyAvailable()).isEqualTo(80);

        publishCompleted(20);
        Part fulfilled = partService.getPart(part.id());
        assertThat(fulfilled.qtyReserved()).isZero();
        assertThat(fulfilled.qtyInStock()).isEqualTo(80);

        Part fresh = partService.createPart(
                "ORD-REJ-" + UUID.randomUUID().toString().substring(0, 8), "Peça rejeição", "un", 0);
        stockEntryService.registerEntry(fresh.id(), 50, null, actorId);

        UUID rejectOrderId = UUID.randomUUID();
        eventPublisher.publishEvent(new PedidoAprovadoEvent(
                rejectOrderId, actorId, List.of(new ItemRef(fresh.id(), 10))));
        eventPublisher.publishEvent(new PedidoRejeitadoEvent(
                rejectOrderId, actorId, "Sem estoque físico", List.of(new ItemRef(fresh.id(), 10))));

        Part released = partService.getPart(fresh.id());
        assertThat(released.qtyReserved()).isZero();
        assertThat(released.qtyInStock()).isEqualTo(50);
    }

    @Test
    void shouldNotApplySameEventTwice() {
        publishApproved(15);
        publishApproved(15);

        Part afterDuplicate = partService.getPart(part.id());
        assertThat(afterDuplicate.qtyReserved()).isEqualTo(15);
    }

    @Test
    void shouldRejectFulfillmentWhenReservationIsInsufficient() {
        assertThatThrownBy(() -> publishCompleted(10))
                .isInstanceOf(InvalidStockOperationException.class);
    }

    private void publishApproved(int quantity) {
        eventPublisher.publishEvent(new PedidoAprovadoEvent(
                orderId, actorId, List.of(new ItemRef(part.id(), quantity))));
    }

    private void publishCompleted(int quantity) {
        eventPublisher.publishEvent(new PedidoConcluidoEvent(
                orderId, actorId, List.of(new ItemRef(part.id(), quantity))));
    }
}
